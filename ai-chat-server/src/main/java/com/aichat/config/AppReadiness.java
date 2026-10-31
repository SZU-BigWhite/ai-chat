package com.aichat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 在初始化任务结束后发布服务就绪状态。 */
@Component
public class AppReadiness {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppReadiness.class);
    // 端口监听不代表初始化完成；需等待主题种子等启动任务全部完成。
    private volatile boolean ready;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        ready = true;
        LOGGER.info("Application ready: initialization completed; API health endpoint is available");
    }

    public boolean isReady() {
        return ready;
    }
}
