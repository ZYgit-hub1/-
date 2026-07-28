package com.huadianguangdong.alert.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Drools 规则引擎配置
 * <p>
 * 启动时从 classpath:rules/*.drl 加载全部规则文件，构建 {@link KieContainer} 单例。<br>
 * 规则文件中通过 global 引用 {@code AlertActionService}，由调用方在 KieSession 中 setGlobal。
 * <p>
 * 动态热加载：
 * <ul>
 *   <li>手动触发：调用 {@link #reload()} 方法重新编译 .drl 文件并原子替换 KieContainer</li>
 *   <li>定时轮询：通过 {@code drools.reload.interval-sec} 配置轮询间隔，0 表示禁用</li>
 *   <li>外部目录加载：通过 {@code drools.reload.external-dir} 指定外部规则目录，覆盖 classpath 规则</li>
 * </ul>
 * 替换过程通过 {@link ReentrantLock} 串行化，运行中的 KieSession 不受影响（持有旧 Container 引用）。
 *
 * @author huadianguangdong
 */
@Slf4j
@Configuration
public class DroolsConfig {

    /** 规则文件扫描路径（classpath 内） */
    private static final String RULES_PATH = "rules/*.drl";

    /** 外部规则目录（优先于 classpath，为空则只读 classpath） */
    @Value("${drools.reload.external-dir:}")
    private String externalRuleDir;

    /** 定时轮询间隔（秒），0 表示禁用 */
    @Value("${drools.reload.interval-sec:0}")
    private long reloadIntervalSec;

    /** KieContainer 原子引用（支持运行时热替换） */
    private final AtomicReference<KieContainer> kieContainerRef = new AtomicReference<>();

    /** 重建锁（串行化 reload 操作，避免并发编译冲突） */
    private final ReentrantLock reloadLock = new ReentrantLock();

    /**
     * 构建 KieContainer（单例）
     *
     * @return KieContainer
     */
    @Bean
    public KieContainer kieContainer() throws IOException {
        KieContainer container = buildKieContainer();
        kieContainerRef.set(container);
        log.info("[Drools] KieContainer 初始化完成，externalDir={} reloadInterval={}s", externalRuleDir, reloadIntervalSec);

        // 启动定时轮询（interval > 0 时生效）
        if (reloadIntervalSec > 0) {
            startPollingWatcher();
        }
        return container;
    }

    /**
     * 默认 KieBase
     *
     * @param kieContainer KieContainer
     * @return KieBase
     */
    @Bean
    public KieBase kieBase(KieContainer kieContainer) {
        return kieContainer.getKieBase();
    }

    /**
     * 获取当前生效的 KieContainer（供 RuleEngineService 使用，支持热替换后立即生效）
     *
     * @return 当前 KieContainer
     */
    public KieContainer getKieContainer() {
        KieContainer container = kieContainerRef.get();
        return container != null ? container : kieContainer;
    }

    /**
     * 手动触发规则热加载
     * <p>
     * 重新扫描 .drl 文件并编译，成功后原子替换 KieContainer。<br>
     * 编译失败时保留旧 Container，不影响在线服务。
     *
     * @return 成功返回 true，编译失败返回 false
     */
    public boolean reload() {
        reloadLock.lock();
        try {
            log.info("[Drools] 开始热加载规则文件...");
            KieContainer newContainer = buildKieContainer();
            KieContainer oldContainer = kieContainerRef.getAndSet(newContainer);

            // 释放旧 Container（已持有旧引用的 KieSession 不受影响）
            if (oldContainer != null) {
                try {
                    oldContainer.dispose();
                } catch (Exception e) {
                    log.warn("[Drools] 旧 KieContainer 释放异常（可忽略）", e);
                }
            }
            log.info("[Drools] 规则热加载完成，KieContainer 已替换");
            return true;
        } catch (Exception e) {
            log.error("[Drools] 规则热加载失败，保留旧 KieContainer", e);
            return false;
        } finally {
            reloadLock.unlock();
        }
    }

    /**
     * 获取当前规则版本标识（用于健康检查 / 监控）
     *
     * @return 规则版本字符串
     */
    public String getRuleVersion() {
        KieContainer container = kieContainerRef.get();
        if (container == null) {
            return "unknown";
        }
        try {
            return container.getReleaseId().toExternalForm();
        } catch (Exception e) {
            return "v-" + System.identityHashCode(container);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建新的 KieContainer（编译 .drl 文件）
     */
    private KieContainer buildKieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        int ruleCount = 0;

        // 1. 优先加载外部目录规则（覆盖 classpath 同名规则）
        if (externalRuleDir != null && !externalRuleDir.isBlank()) {
            ruleCount += loadRulesFromExternalDir(kieServices, kieFileSystem, externalRuleDir);
        }

        // 2. 加载 classpath 规则（外部目录未覆盖的部分）
        ruleCount += loadRulesFromClasspath(kieServices, kieFileSystem);

        if (ruleCount == 0) {
            log.warn("[Drools] 未扫描到任何 .drl 规则文件");
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Drools 规则编译失败: " + kieBuilder.getResults().getMessages());
        }

        KieModule kieModule = kieBuilder.getKieModule();
        log.info("[Drools] 规则编译完成，共加载 {} 个 .drl 文件", ruleCount);
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    /**
     * 从 classpath 加载规则文件
     */
    private int loadRulesFromClasspath(KieServices kieServices, KieFileSystem kieFileSystem) throws IOException {
        org.springframework.core.io.Resource[] resources =
                new PathMatchingResourcePatternResolver().getResources("classpath:" + RULES_PATH);
        int count = 0;
        for (org.springframework.core.io.Resource res : resources) {
            Resource drlResource = ResourceFactory.newUrlResource(res.getURL());
            drlResource.setSourcePath("rules/" + res.getFilename());
            kieFileSystem.write(drlResource);
            count++;
            log.debug("[Drools] 加载 classpath 规则: {}", res.getFilename());
        }
        return count;
    }

    /**
     * 从外部目录加载规则文件（支持运行时修改，无需重启）
     */
    private int loadRulesFromExternalDir(KieServices kieServices, KieFileSystem kieFileSystem, String dirPath) {
        java.io.File dir = new java.io.File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("[Drools] 外部规则目录不存在或非目录: {}", dirPath);
            return 0;
        }

        java.io.File[] drlFiles = dir.listFiles((d, name) -> name.endsWith(".drl"));
        if (drlFiles == null || drlFiles.length == 0) {
            log.warn("[Drools] 外部规则目录无 .drl 文件: {}", dirPath);
            return 0;
        }

        int count = 0;
        for (java.io.File drlFile : drlFiles) {
            Resource drlResource = ResourceFactory.newFileResource(drlFile);
            drlResource.setSourcePath("rules/" + drlFile.getName());
            kieFileSystem.write(drlResource);
            count++;
            log.debug("[Drools] 加载外部规则: {}", drlFile.getAbsolutePath());
        }
        return count;
    }

    /**
     * 启动定时轮询（守护线程，JVM 退出时自动销毁）
     */
    private void startPollingWatcher() {
        Thread watcher = new Thread(() -> {
            log.info("[Drools] 定时轮询启动，间隔 {}s", reloadIntervalSec);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(reloadIntervalSec * 1000L);
                    // 检查外部目录文件是否有变更（通过 lastModified 摘要）
                    if (hasRuleFilesChanged()) {
                        log.info("[Drools] 检测到规则文件变更，触发自动热加载");
                        reload();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("[Drools] 轮询线程被中断，退出");
                    break;
                } catch (Exception e) {
                    log.error("[Drools] 轮询异常", e);
                }
            }
        }, "drools-rule-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    /** 上次扫描的文件指纹（用于变更检测） */
    private volatile String lastFingerprint = "";

    /**
     * 计算规则文件指纹（文件名 + lastModified）
     *
     * @return 文件指纹字符串
     */
    private String computeFingerprint() {
        if (externalRuleDir == null || externalRuleDir.isBlank()) {
            return "classpath-only";
        }
        java.io.File dir = new java.io.File(externalRuleDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return "invalid-dir";
        }
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".drl"));
        if (files == null || files.length == 0) {
            return "empty";
        }
        java.util.Arrays.sort(files, java.util.Comparator.comparing(java.io.File::getName));
        StringBuilder sb = new StringBuilder();
        for (java.io.File f : files) {
            sb.append(f.getName()).append("=").append(f.lastModified()).append(";");
        }
        return sb.toString();
    }

    /**
     * 检查规则文件是否发生变更
     */
    private boolean hasRuleFilesChanged() {
        String current = computeFingerprint();
        if (!current.equals(lastFingerprint)) {
            lastFingerprint = current;
            return true;
        }
        return false;
    }
}
