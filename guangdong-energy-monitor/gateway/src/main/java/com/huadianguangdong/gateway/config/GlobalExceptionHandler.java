package com.huadianguangdong.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huadianguangdong.common.api.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器（WebFlux 响应式）
 * <p>
 * 捕获网关层的所有未处理异常，统一返回 JSON 格式的错误响应。
 * 异常优先级：
 * <ol>
 *   <li>ResponseStatusException（如 404 Not Found）→ 对应状态码</li>
 *   <li>其他异常 → 500 系统异常</li>
 * </ol>
 *
 * @author huadianguangdong
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerWebExchange.Builder builder = exchange.mutate();

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "系统异常，请联系管理员";
            log.error("[网关异常] 未捕获异常 path={}", exchange.getRequest().getPath(), ex);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> body = R.fail(status.value(), message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"code\":" + status.value() + ",\"msg\":\"" + message + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        log.warn("[网关异常] status={} message={} path={}",
                status.value(), message, exchange.getRequest().getPath());

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
