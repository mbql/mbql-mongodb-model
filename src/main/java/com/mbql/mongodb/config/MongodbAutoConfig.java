package com.mbql.mongodb.config;

import com.mbql.mongodb.utils.SslUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MongoDB 自动配置类
 * @author slp
 */
@Slf4j
@Configuration
@AllArgsConstructor
@ConditionalOnClass(MongoClient.class)
@EnableConfigurationProperties(MongodbProperties.class)
public class MongodbAutoConfig {

    private final MongodbProperties properties;

    @Bean
    @ConditionalOnMissingBean(value = MongoClient.class)
    @ConditionalOnProperty(prefix = "mongodb", name = {"enable-ssl", "use-static-mode"}, havingValue = "true")
    public MongoClient mongoSslStaticClient() {
        log.info("--------- MongoDB 使用静态 SSL / TLS 模式启动 ----------");
        return new SslUtils(properties).mongoSslStaticClient();
    }

    @Bean
    @ConditionalOnMissingBean(value = MongoClient.class, name = "mongoSslStaticClient")
    @ConditionalOnProperty(prefix = "mongodb", name = "enable-ssl", havingValue = "true")
    public MongoClient mongoSslDynamicClient() {
        log.info("--------- MongoDB 使用动态 SSL / TLS 模式启动 ----------");
        return MongoClients.create(new SslUtils(properties).getMongoDynamicClientSettings());
    }

    @Bean
    @ConditionalOnMissingBean(value = MongoClient.class)
    public MongoClient mongoDefaultClient() {
        log.info("--------- MongoDB 使用默认模式启动 ----------");
        return MongoClients.create(new SslUtils(properties).mongoDefaultClientSettings());
    }

}
