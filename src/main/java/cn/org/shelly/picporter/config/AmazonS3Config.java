package cn.org.shelly.picporter.config;

import cn.org.shelly.picporter.config.properties.MinioProperties;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 功能：AmazonS3协议配置
 * 使用AmazonS3协议JDK替代MINIO原生的JDK
 * @author Shelly6 2025/5/10
 */
@Configuration
public class AmazonS3Config {

    @Resource
    private MinioProperties minioProperties;

    /**
     * 创建并配置Amazon S3客户端
     *
     * @return AmazonS3 实例，用于与Amazon S3服务进行交互
     */
    @Bean(name = "amazonS3Client")
    public AmazonS3 getClient() {
        // 设置连接参数
        ClientConfiguration config = new ClientConfiguration();
        // 设置连接协议为HTTP
        config.setProtocol(Protocol.HTTP);
        // 设置连接超时时间为5秒
        config.setConnectionTimeout(5000);
        // 设置是否使用Expect: 100-continue头
        config.setUseExpectContinue(true);

        // 设置Amazon S3服务端点
        AwsClientBuilder.EndpointConfiguration endPointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                minioProperties.url,
                Regions.US_EAST_1.name()
        );

        // 设置AWS凭证
        AWSCredentials credentials = new BasicAWSCredentials(minioProperties.accessKey, minioProperties.secretKey);

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endPointConfiguration)
                .withClientConfiguration(config)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
