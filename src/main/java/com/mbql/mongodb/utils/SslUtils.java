package com.mbql.mongodb.utils;

import com.google.common.collect.Lists;
import com.mbql.mongodb.config.MongodbProperties;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCompressor;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.SneakyThrows;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * SSL 认证工具类
 *
 * @author slp
 */
public class SslUtils {

    private final static String FIXED_MONGO_PATH = File.separator + "certs" + File.separator;

    private final static String TLS_12_PROTOCOL = "TLSv1.2";

    private final static String CLASS_PATH = "classpath:";

    private MongodbProperties properties;

    private SslUtils() {
    }

    public SslUtils(MongodbProperties properties) {
        this.properties = properties;
    }

    /**
     * MongoDB 默认客户端配置
     *
     * @return MongoClientSettings
     */
    public MongoClientSettings mongoDefaultClientSettings() {
        return MongoClientSettings.builder()
                .credential(MongoCredential
                        .createScramSha256Credential(properties.getAuthUserName(),
                                properties.getDatabase(), properties.getPassword().toCharArray()))
                .applyConnectionString(new ConnectionString(properties.getUrl())).build();
    }

    /**
     * MongoDB SSL / TLS 静态认证
     *
     * @return MongoClient
     */
    @SneakyThrows
    public MongoClient mongoSslStaticClient() {
        String path = ResourceUtils.getURL(CLASS_PATH).getPath();
        String trustStorePath = path.concat(FIXED_MONGO_PATH).concat(properties.getCerts().getTrustStoreName());
        String keyStorePath = path.concat(FIXED_MONGO_PATH).concat(properties.getCerts().getKeyStoreName());
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", properties.getCerts().getPassword());
        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", properties.getCerts().getPassword());
        return MongoClients.create(getMongoStaticClientSettings());
    }

    /**
     * SSL / TLS 静态客户端配置
     *
     * @return MongoClientSettings
     */
    private MongoClientSettings getMongoStaticClientSettings() {
        // 设置 SSL / TLS 客户端配置
        return MongoClientSettings.builder().applyToSslSettings(builder -> {
                    builder.enabled(properties.isEnableSsl());
                    builder.invalidHostNameAllowed(properties.isInvalidHostNameAllowed());
                }).applyConnectionString(new ConnectionString(properties.getUrl()))
                .applyToClusterSettings(builder -> builder.mode(properties.getClusterConnectionMode())
                        .requiredClusterType(properties.getClusterType())
                        .requiredReplicaSetName(properties.getReplSetName()))
                .compressorList(Lists.newArrayList(
                        MongoCompressor.createZstdCompressor(),
                        MongoCompressor.createSnappyCompressor(),
                        MongoCompressor.createZlibCompressor()))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(properties.getConnectTimeout(), TimeUnit.SECONDS)
                        .readTimeout(properties.getReadTimeout(), TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxWaitTime(properties.getMaxWaitTime(), TimeUnit.MILLISECONDS)
                        .maxSize(properties.getMaxSize()))
                .credential(MongoCredential.createMongoX509Credential()).build();
    }

    /**
     * SSL / TLS 动态模式下, 选择 MongoDB 动态客户端配置
     *
     * @return MongoClientSettings
     */
    public MongoClientSettings chooseMongoDynamicClientSettings() {
        return properties.isUseDynamicNettyMode() ? getMongoDynamicNettyClientSettings()
                : getMongoDynamicJdkClientSettings();
    }

    /**
     * 获取动态 Netty 方式 SSL / TLS MongoDB 客户端配置
     * https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/mongoclientsettings
     *
     * @return MongoClientSettings
     */
    private MongoClientSettings getMongoDynamicNettyClientSettings() {
        // 获取 SSL / TLS SslContext 上下文
        SslContext sslContext = getNettySslContext(
                properties.getCerts().getTrustStoreName(),
                properties.getCerts().getKeyStoreName(),
                properties.getCerts().getPassword(),
                properties.isEnableSsl());

        // 设置 SSL / TLS 客户端配置
        return MongoClientSettings.builder().applyToSslSettings(builder -> {
                    builder.enabled(properties.isEnableSsl());
                    builder.invalidHostNameAllowed(properties.isInvalidHostNameAllowed());
                })
                .streamFactoryFactory(NettyStreamFactoryFactory.builder()
                        .sslContext(sslContext).build())
                .applyConnectionString(new ConnectionString(properties.getUrl()))
                .applyToClusterSettings(builder -> builder
                        .mode(properties.getClusterConnectionMode())
                        .requiredClusterType(properties.getClusterType())
                        .requiredReplicaSetName(properties.getReplSetName()))
                .compressorList(Lists.newArrayList(MongoCompressor.createZstdCompressor(),
                        MongoCompressor.createSnappyCompressor(),
                        MongoCompressor.createZlibCompressor()))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(properties.getConnectTimeout(), TimeUnit.SECONDS)
                        .readTimeout(properties.getReadTimeout(), TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxWaitTime(properties.getMaxWaitTime(), TimeUnit.MILLISECONDS)
                        .maxConnecting(properties.getMaxConn())
                        .maxSize(properties.getMaxSize())
                        .maxConnectionIdleTime(properties.getMaxConnectIdleTime(), TimeUnit.MILLISECONDS))
                .credential(MongoCredential.createMongoX509Credential()).build();
    }

    /**
     * 获取动态 JDK 方式 SSL / TLS MongoDB 客户端配置
     * https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/mongoclientsettings
     *
     * @return MongoClientSettings
     */
    private MongoClientSettings getMongoDynamicJdkClientSettings() {
        SSLContext context = getJdkSslContext(properties.getCerts().getTrustStoreName(),
                properties.getCerts().getKeyStoreName(), properties.getCerts().getPassword());
        return MongoClientSettings.builder().applyToSslSettings(builder -> {
                    builder.enabled(properties.isEnableSsl());
                    builder.context(context);
                    builder.invalidHostNameAllowed(properties.isInvalidHostNameAllowed());
                })
                .applyConnectionString(new ConnectionString(properties.getUrl()))
                .applyToClusterSettings(builder -> builder.mode(properties.getClusterConnectionMode())
                        .requiredClusterType(properties.getClusterType())
                        .requiredReplicaSetName(properties.getReplSetName()))
                .compressorList(Lists.newArrayList(MongoCompressor.createZstdCompressor(),
                        MongoCompressor.createSnappyCompressor(),
                        MongoCompressor.createZlibCompressor()))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(properties.getConnectTimeout(), TimeUnit.SECONDS)
                        .readTimeout(properties.getReadTimeout(), TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxWaitTime(properties.getMaxWaitTime(), TimeUnit.MILLISECONDS)
                        .maxSize(properties.getMaxSize()))
                .credential(MongoCredential.createMongoX509Credential()).build();
    }

    /**
     * 获取 Netty 的 SSL / TLS 上下文
     *
     * @param trustStoreName JVM 信任库名称
     * @param keyStoreName   JVM 密钥库名称
     * @param password       密码
     * @param isEnableSsl    是否开启 SSL / TLS
     * @return SslContext
     */
    @SneakyThrows
    private SslContext getNettySslContext(String trustStoreName, String keyStoreName, String password, boolean isEnableSsl) {
        // 解析获取客户端认证证书路径
        String path = ResourceUtils.getURL(CLASS_PATH).getPath();
        KeyManagerFactory kmf = getKeyManagerFactory(keyStoreName, password, path);
        TrustManagerFactory tmf = getTrustManagerFactory(trustStoreName, password, path);

        return SslContextBuilder.forClient().sslProvider(OpenSsl.supportsKeyManagerFactory() ?
                        SslProvider.OPENSSL : SslProvider.JDK).
                trustManager(tmf).keyManager(kmf).startTls(isEnableSsl).build();
    }

    /**
     * 获取 JDK 的 SSL / TLS 上下文
     *
     * @param trustStoreName JVM 信任库名称
     * @param keyStoreName   JVM 密钥库名称
     * @param password       密码
     * @return SslContext
     */
    @SneakyThrows
    private SSLContext getJdkSslContext(String trustStoreName, String keyStoreName, String password) {
        SSLContext context = SSLContext.getInstance(TLS_12_PROTOCOL);
        String path = ResourceUtils.getURL(CLASS_PATH).getPath();
        KeyManagerFactory kmf = getKeyManagerFactory(keyStoreName, password, path);
        TrustManagerFactory tmf = getTrustManagerFactory(trustStoreName, password, path);
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return context;
    }

    /**
     * 获取 TrustManagerFactory
     *
     * @param trustStoreName JVM 密钥库名称
     * @param password       密码
     * @param path           classpath 路径
     * @return TrustManagerFactory
     */
    @SneakyThrows
    private TrustManagerFactory getTrustManagerFactory(String trustStoreName, String password, String path) {
        String trustStorePath = path.concat(FIXED_MONGO_PATH).concat(trustStoreName);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream myTrustStore = new FileInputStream(trustStorePath);
        trustStore.load(myTrustStore, password.toCharArray());
        myTrustStore.close();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }

    /**
     * 获取 KeyManagerFactory
     *
     * @param keyStoreName JVM 密钥库名称
     * @param password     密码
     * @param path         classpath 路径
     * @return KeyManagerFactory
     */
    @SneakyThrows
    private KeyManagerFactory getKeyManagerFactory(String keyStoreName, String password, String path) {
        String keyStorePath = path.concat(FIXED_MONGO_PATH).concat(keyStoreName);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream myKeyStore = new FileInputStream(keyStorePath);
        keyStore.load(myKeyStore, password.toCharArray());
        myKeyStore.close();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());
        return kmf;
    }

}
