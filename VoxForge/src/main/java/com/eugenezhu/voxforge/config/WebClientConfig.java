package com.eugenezhu.voxforge.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.config
 * @className: WebClientConfig
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午3:14
 */
@Configuration
public class WebClientConfig {
    @Value("${webclient.connect-timeout}")
    private Duration connectionTimeout;
    @Value("${webclient.read-timeout}")
    private Duration readTimeout;
    @Value("${webclient.write-timeout}")
    private Duration writeTimeout;
    @Value("${webclient.max-memory-size}")
    private String maxMemorySize;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                // 设置连接超时（建立 TCP 连接的最大等待时间）
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectionTimeout.toMillis())
                // 启用TCP keepalive以保持连接活跃
                .option(ChannelOption.SO_KEEPALIVE, true)
                // 设置TCP_NODELAY以减少延迟
                .option(ChannelOption.TCP_NODELAY, true)
                // 设置响应超时（从发送请求到收到响应的最大等待时间）
                .responseTimeout(readTimeout)
                // 连接建立后添加超时处理器
                .doOnConnected(
                        conn -> conn.addHandlerLast(new ReadTimeoutHandler((int) readTimeout.toSeconds()))
                                .addHandlerLast(new WriteTimeoutHandler((int) writeTimeout.toSeconds()))
                );

        return WebClient.builder()
                // 关联上面配置好的 HttpClient 作为底层连接器
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // 配置编解码器（处理请求/响应数据的序列化/反序列化）
                .codecs(
                        // 设置默认编解码器的最大内存缓冲区大小（防止大响应数据导致 OOM）
                        configurer -> configurer.defaultCodecs().maxInMemorySize(parseMemorySize(maxMemorySize))
                )
                .build();
    }

    @Bean("embeddingWebClient")
    public WebClient embeddingWebClient(@Value("${external.embedding.api-url}") String embeddingUrl) {
        return webClient().mutate()
                .baseUrl(embeddingUrl)
                .build();
    }

    @Bean("asrWebClient")
    public WebClient asrWebClient(@Value("${external.asr.api-url}") String asrUrl) {
        return webClient().mutate() // 基于默认 WebClient 配置创建新实例
                .baseUrl(asrUrl)
                .build();
    }

    @Bean("ttsWebClient")
    public WebClient ttsWebClient(@Value("${external.tts.api-url}") String ttsUrl) {
        return webClient().mutate()
                .baseUrl(ttsUrl)
                .build();
    }

    @Bean("llmWebClient")
    public WebClient llmWebClient(@Value("${external.llm.api-url}") String llmUrl) {
        return webClient().mutate()
                .baseUrl(llmUrl)
                .build();
    }

    private int parseMemorySize(String memorySize) {
        if (memorySize.endsWith("MB")) {
            return Integer.parseInt(memorySize.replace("MB", "")) * 1024 * 1024;
        } else if (memorySize.endsWith("KB")) {
            return Integer.parseInt(memorySize.replace("KB", "")) * 1024;
        } else if (memorySize.endsWith("GB")) {
            return Integer.parseInt(memorySize.replace("GB", "")) * 1024 * 1024 * 1024;
        } else {
            return Integer.parseInt(memorySize);
        }
    }
}

