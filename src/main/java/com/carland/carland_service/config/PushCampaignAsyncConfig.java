package com.carland.carland_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * tr: Toplu push kampanyaları için dar havuz: HTTP thread'ini bloklamaz, FCM'i aşırı yüklemez.
 * en: Narrow pool for bulk push campaigns: does not block HTTP threads, avoids overloading FCM.
 */
@Configuration
public class PushCampaignAsyncConfig {

    @Bean(name = "pushCampaignExecutor")
    public Executor pushCampaignExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("push-campaign-");
        executor.initialize();
        return executor;
    }
}
