package com.sun.config;

import com.sun.util.SpringUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanMethodExecutorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }
}