package com.mbql.mongodb.config;

import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.connection.ClusterType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mongodb 相关配置属性
 *
 * @author slp
 */
@Data
@ConfigurationProperties(prefix = "mongodb")
public class MongodbProperties {

    /**
     * 数据库连接 url
     */
    private String url = "mongodb://localhost:27017/test";

    /**
     * 副本集名称
     */
    private String replSetName;

    /**
     * 集群类型
     */
    private ClusterType clusterType = ClusterType.STANDALONE;

    /**
     * 集群连接模型
     */
    private ClusterConnectionMode clusterConnectionMode = ClusterConnectionMode.SINGLE;

    /**
     * 连接超时时间, 单位 / s
     */
    private int connectTimeout = 10;

    /**
     * 读取超时时间, 单位 / s
     */
    private int readTimeout = 15;

    /**
     * 连接池最大等待时间 单位 / ms
     */
    private long maxWaitTime = 3000L;

    /**
     * 最大连接空闲时间 单位 / ms
     */
    private long maxConnectIdleTime = 2000L;

    /**
     * 连接池中最大并发连接数量
     */
    private int maxConn = Runtime.getRuntime().availableProcessors();

    /**
     * 连接池最大数量
     */
    private int maxSize = 200;

    /**
     * 是否开启 SSL / TLS
     */
    private boolean enableSsl;

    /**
     * SSL / TLS 开启是否使用静态模型
     */
    private boolean useStaticMode;

    /**
     * 配合开启 SSL / TLS 动态模式下生效, 默认 JDK 模式
     */
    private boolean useDynamicNettyMode;

    /**
     * 是否允许无效主机连接
     */
    private boolean invalidHostNameAllowed;

    /**
     * 客户端认证用户名
     */
    private String authUserName = "";

    /**
     * 客户端认证密码
     */
    private String password = "";

    /**
     * 客户端认证数据库
     */
    private String database = "admin";

    /**
     * SSL / TLS 客户端证书配置
     */
    private CertsClient certs;

    @Data
    public static class CertsClient {

        /**
         * 签名证书密码
         */
        private String password;

        /**
         * 配置 JVM 信任库名称
         */
        private String trustStoreName;

        /**
         * 配置 JVM 密钥库名称
         */
        private String keyStoreName;

    }

}
