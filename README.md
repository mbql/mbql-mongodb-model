## MongoDB 客户端快速自动配置和注入

### 一、支持通过客户端简单配置去操作 MongoDB（开箱即用）

##### 1、该组件依赖少、轻量级、易用、方便与主流 SpringBoot、SpringCloud 框架快速集成, 支持 MongoDB 默认客户端认证模式, 还支持 SSL / TLS 静态和动态模式认证, 只需要通过简单的配置就可使用强大的 MongoDB 客户端自动配置和相关操作。

##### 2、目前针对 MongoDB 客户端自动配置做了增强, 将原来使用 SSL / TLS 需要编写复杂逻辑去实现安全认证, 有时候还不知道怎么去配置, 对于这种情况, 我就基于原来的 MongoDB 客户端配置做了增强, 现在只需要简单的几行配置就可实现 SSL / TLS 安全认证, 同时也兼容 MongoDB 原来客户端模式, 不影响 MongoDB 的正常使用。

#### 3、下面简单说明一下 MongoDB 客户端不同模式下配置

MongoDB 客户端组件依赖如下：
```
<dependency>
    <groupId>io.github.mbql</groupId>
    <artifactId>mbql-mongodb-model</artifactId>
    <version>1.0.3</version>
</dependency>
```

目前支持如下几种 MongoDB 客户端配置:

1. 支持默认 MongoDB 客户端认证模式并兼容原来的配置
2. 支持 SSL / TLS 客户端静态认证模式
3. 支持 SSL / TLS 客户端动态认证模式

简单说明一下这 3 种 模式在 SpringBoot 中怎么使用及配置 (yaml 方式配置)

1. 默认客户端认证模式

   ```  
   mongodb:
     url: mongodb://127.0.0.1:27017/test
     auth-user-name: admin
     password: 123456
     database: admin
   ```
   或

   ```
   mongodb:
     url: mongodb://admin:123456@127.0.0.1:27017/test?authSource=admin
   ```

2. SSL / TLS 客户端静态认证模式 (该模式下需要 SSL / TLS 签名证书和客户端证书)
  
   ```
    mongodb:
      url: mongodb://127.0.0.1:27017/test
      use-static-mode: true
      enable-ssl: true
      cluster-type: replica_set
      cluster-connection-mode: multiple
      repl-set-name: rs0 # 副本集名称
      invalid-host-name-allowed: true # 允许无效主机连接, 默认是只能本机
      certs:
        password: 123456 # 密钥密码
        trustStoreName: cacerts # 信任证书名称
        keyStoreName: keystore.pkcs12 # 客户端证书名称
   ```

   注意: 该方式需要自己生成签名证书, 可通过 openssl 方式生成服务端和客户端证书, 也可以去第三方厂商去获取证书, 但需要收费的, 自签名证书自己通过 openssl 生成, 免费即可使用, 具体怎么生成免费的自签名证书, 请点击下方连接访问
   
   [自签名证书生成](https://blog.csdn.net/weixin_43322048/article/details/129190278)

   > 将生成的 CA 证书导入到 JVM 服务器中, 客户端密钥证书转换为 pkcs12 格式, 在 Java 中才能识别该证书, 需要将生成的 2 个 JVM 证书放在项目的 classpath 路径的 certs 目录中, certs 目录没有自己创建, 再把这两个 JVM 需要认证的证书放在该目录下。
   
   **如下示例（前提：服务端已开启 SSL / TLS 认证, 并且已生成自签名证书）:** 
   ```
    # 通过 keytool 工具将 ca 证书导入到 JVM 服务器中
    keytool -import -keystore cacerts -file ca.pem -storepass 密码
    
    # 通过 openssl 生成 JVM 客户端密钥证书
    openssl pkcs12 -export -out keystore.pkcs12 -in client.pem
   
    # 最后, 在这里将生成的名为 cacerts、keystore.pkcs12 证书放到项目 classpath 路径的 certs 目录里面
   ```

3. 支持 SSL / TLS 客户端动态认证模式 (该模式下需要 SSL / TLS 服务端签名证书和客户端证书, 生成方式可通过 openssl, 这种方式对比前面几种的优势是不需要将证书导入到 JVM 服务器中, 安全性更高, 建议在生成环境使用该方式)
    
   ```
    mongodb:
      url: mongodb://127.0.0.1:27017/test
      invalid-host-name-allowed: true
      enable-ssl: true
      cluster-type: replica_set
      cluster-connection-mode: multiple
      repl-set-name: rs0
      certs:
        password: 123456
        trustStoreName: cacerts
        keyStoreName: keystore.pkcs12
   ```

   **如下示例（前提：服务端已开启 SSL / TLS 认证, 并且已生成自签名证书）:**
     ```
      # 生成 server 信任证书
      openssl pkcs12 -export -in server.crt -inkey server.key -out server
      
      # 生成 client 密钥证书
      openssl pkcs12 -export -out keystore.pkcs12 -in client.pem
      # 最后, 在这里将生成的名为 server、keystore.pkcs12 证书放到项目 classpath 路径的 certs 目录里面
     ```

通过系统安全性考虑, 推荐使用第三种方式对 MongoDB Client 认证

目前只针对如上几方面做了增强, 也简化了在 MongoDB 中使用 SSL / TLS 复杂配置, 上面通过简单配置即可实现 SSL / TLS 认证, 对于证书的生成不在本文档范围, 同时, 也支持 MongoDB 所有部署方式, 如：单机、副本集、分片等。

**喜欢的记得点个 Star 哦**

###二、有什么问题可以加 QQ 或 微信
> QQ：1491140482
 
> 微信：sueno0786