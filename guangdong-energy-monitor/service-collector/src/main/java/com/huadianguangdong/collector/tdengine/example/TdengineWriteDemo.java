package com.huadianguangdong.collector.tdengine.example;

import com.huadianguangdong.collector.tdengine.entity.HydroLevel;
import com.huadianguangdong.collector.tdengine.entity.WeatherLive;
import com.huadianguangdong.collector.tdengine.repository.TdengineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TDengine JDBC Connector 写入示例
 * <p>
 * 演示如何通过官方 JDBC Connector 向 TDengine 写入气象与水文实时数据。
 * 该类为 ApplicationRunner，启动时自动执行一次示例写入与查询。
 * <p>
 * 生产环境使用时：
 * <ol>
 *   <li>移除 @Component 或改用 @Profile("dev") 限定环境</li>
 *   <li>将写入逻辑集成到 Kafka 消费者中，按消息触发写入</li>
 * </ol>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TdengineWriteDemo implements ApplicationRunner {

    private final TdengineRepository tdengineRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== TDengine 写入示例开始 ==========");

        // 1. 单条气象数据写入
        WeatherLive weather = new WeatherLive();
        weather.setTs(LocalDateTime.now());
        weather.setTemp(28.5f);
        weather.setHumidity(75.0f);
        weather.setWindSpeed(5.2f);
        weather.setWindDir((short) 180);
        weather.setRain1h(0.0f);
        weather.setPressure(1013.2f);
        weather.setPlantId(1L);
        weather.setDistrictCode("440500");
        tdengineRepository.insertWeather(weather);
        log.info("[示例] 单条气象数据写入完成: plantId=1 temp=28.5℃");

        // 2. 批量气象数据写入（模拟过去 1 小时每分钟一条）
        List<WeatherLive> weatherBatch = buildWeatherBatch(1L, "440500", 60);
        tdengineRepository.batchInsertWeather(weatherBatch);
        log.info("[示例] 批量气象数据写入完成: plantId=1 count=60");

        // 3. 单条水文数据写入
        HydroLevel hydro = new HydroLevel();
        hydro.setTs(LocalDateTime.now());
        hydro.setWaterLevel(8.52f);
        hydro.setFlow(5200.0f);
        hydro.setIsOverWarning(false);
        hydro.setStationId(1L);
        tdengineRepository.insertHydro(hydro);
        log.info("[示例] 单条水文数据写入完成: stationId=1 waterLevel=8.52m");

        // 4. 批量水文数据写入
        List<HydroLevel> hydroBatch = buildHydroBatch(1L, 60);
        tdengineRepository.batchInsertHydro(hydroBatch);
        log.info("[示例] 批量水文数据写入完成: stationId=1 count=60");

        // 5. 查询演示
        List<WeatherLive> recentWeather = tdengineRepository.queryWeatherLastHours(1L, 1);
        log.info("[示例] 查询电厂1最近1小时气象数据: {} 条", recentWeather.size());

        WeatherLive latest = tdengineRepository.queryLatestWeather(1L);
        if (latest != null) {
            log.info("[示例] 电厂1最新气象: temp={}℃ windSpeed={}m/s", latest.getTemp(), latest.getWindSpeed());
        }

        List<WeatherLive> downsample = tdengineRepository.queryWeatherDownsample(1L, 1, "5m");
        log.info("[示例] 电厂1过去1小时5分钟降采样: {} 个数据点", downsample.size());

        log.info("========== TDengine 写入示例结束 ==========");
    }

    /**
     * 构建气象数据批量写入列表
     */
    private List<WeatherLive> buildWeatherBatch(Long plantId, String districtCode, int count) {
        List<WeatherLive> list = new ArrayList<>(count);
        LocalDateTime now = LocalDateTime.now();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = count - 1; i >= 0; i--) {
            WeatherLive w = new WeatherLive();
            w.setTs(now.minusMinutes(i));
            w.setTemp(rnd.nextFloat(25.0f, 32.0f));
            w.setHumidity(rnd.nextFloat(60.0f, 90.0f));
            w.setWindSpeed(rnd.nextFloat(2.0f, 10.0f));
            w.setWindDir((short) rnd.nextInt(0, 360));
            w.setRain1h(rnd.nextFloat(0.0f, 5.0f));
            w.setPressure(rnd.nextFloat(1005.0f, 1020.0f));
            w.setPlantId(plantId);
            w.setDistrictCode(districtCode);
            list.add(w);
        }
        return list;
    }

    /**
     * 构建水文数据批量写入列表
     */
    private List<HydroLevel> buildHydroBatch(Long stationId, int count) {
        List<HydroLevel> list = new ArrayList<>(count);
        LocalDateTime now = LocalDateTime.now();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double baseLevel = 8.5;

        for (int i = count - 1; i >= 0; i--) {
            HydroLevel h = new HydroLevel();
            h.setTs(now.minusMinutes(i));
            double level = baseLevel + rnd.nextDouble(-0.2, 0.2);
            h.setWaterLevel((float) Math.round(level * 100) / 100);
            h.setFlow(rnd.nextFloat(5000.0f, 5300.0f));
            h.setIsOverWarning(level > 9.5);
            h.setStationId(stationId);
            list.add(h);
        }
        return list;
    }
}
