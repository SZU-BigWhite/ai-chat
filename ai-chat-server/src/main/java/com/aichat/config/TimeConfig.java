package com.aichat.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 提供统一时间来源；额度日期使用配置的时区换算。 */
@Configuration
public class TimeConfig {
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
