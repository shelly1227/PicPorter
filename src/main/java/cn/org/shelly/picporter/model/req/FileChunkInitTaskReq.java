package cn.org.shelly.picporter.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileChunkInitTaskReq {
    @Schema(description = "文件名称", example = "大型测试文件.zip")
    private String fileName;

    @Schema(description = "文件唯一标识（MD5值）", example = "e12a43fd9e24f6dc325aeb7202dd9e3c")
    private String identifier;

    @Schema(description = "文件总大小（字节）", example = "104857600")
    private Long totalSize;

    @Schema(description = "分片大小（字节）", example = "5242880")
    private Long chunkSize;
}
