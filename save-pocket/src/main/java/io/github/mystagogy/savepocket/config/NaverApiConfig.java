package io.github.mystagogy.savepocket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NaverApiProperties.class)
public class NaverApiConfig {
}
