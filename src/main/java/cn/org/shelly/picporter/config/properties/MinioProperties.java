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
    public static String PREFIX;
    @Value("${upload.minio.url}")
    public String url;

    @Value("${upload.minio.access-key}")
    public String accessKey;

    @Value("${upload.minio.secret-key}")
    public String secretKey;

    @Value("${upload.minio.bucket-name}")
    public String bucketName;

    @Value("${upload.prefix}")
    public String prefix;

    @Override
    public void afterPropertiesSet() {
        URL = url;
        ACCESS_KEY = accessKey;
        SECRET_KEY = secretKey;
        BUCKET_NAME = bucketName;
        PREFIX = prefix;
    }

}
