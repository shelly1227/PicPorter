package cn.org.shelly.picporter.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UploadModeEnum {

    /**
     * 本地
     */
    LOCAL("local", "localUploadStrategyImpl"),

    /**
     * minio
     */
    MINIO("minio", "minioUploadStrategyImpl"),

    /**
     * oss
     */
    OSS("oss", "ossUploadStrategyImpl");

    /**
     * 模式
     */
    private final String mode;

    /**
     * 策略
     */
    private final String strategy;

    /**
     * 获取策略
     *
     * @param mode 模式
     * @return 搜索策略
     */
    public static String getStrategy(String mode) {
        for (UploadModeEnum value : UploadModeEnum.values()) {
            if (value.getMode().equals(mode)) {
                return value.getStrategy();
            }
        }
        return null;
    }
}