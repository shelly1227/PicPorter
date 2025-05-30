package cn.org.shelly.picporter.config.properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Minio配置
 * @author shelly
 */

@Component
@SuppressWarnings("all")
public class MinioProperties implements InitializingBean {
    public static String URL;
    public static String ACCESS_KEY;
    public static String SECRET_KEY;
    public static String BUCKET_NAME;
    @Value("${minio.url}")
    public String url;

    @Value("${minio.access-key}")
    public String accessKey;

    @Value("${minio.secret-key}")
    public String secretKey;

    @Value("${minio.bucket-name}")
    public String bucketName;

    @Override
    public void afterPropertiesSet() {
        URL = url;
        ACCESS_KEY = accessKey;
        SECRET_KEY = secretKey;
        BUCKET_NAME = bucketName;
    }

}
