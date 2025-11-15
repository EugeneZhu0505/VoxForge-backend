package com.eugenezhu.voxforge.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.config
 * @className: SwaggerConfig
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 下午3:48
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("VoxForge API")
                                .version("1.0.0")
                                .description("VoxForge 语音助手 API 接口文档")
                                .contact(new Contact().name("EugeneZhu").email("EugeneZhu@163.com"))
                                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"))
                )
                .servers(
                        List.of(
                                new Server().url("http://localhost:8080").description("Local Server"),
                                new Server().url("https://voxforge.hzau.edu.cn").description("Production Server")
                        )
                );
    }
}

