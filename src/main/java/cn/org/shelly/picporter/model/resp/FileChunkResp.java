package cn.org.shelly.picporter.model.resp;

import cn.org.shelly.picporter.model.po.Chunk;
import com.amazonaws.services.s3.model.PartSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "FileChunkResp", description = "文件分片信息传输对象")
public class FileChunkResp implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "文件唯一标识（md5）")
    private String identifier;

    @Schema(description = "分片上传ID")
    private String uploadId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "所属桶名")
    private String bucketName;

    @Schema(description = "文件的key")
    private String objectKey;

    @Schema(description = "总文件大小（byte）")
    private Long totalSize;

    @Schema(description = "每个分片大小（byte）")
    private Long chunkSize;

    @Schema(description = "分片数量")
    private Integer chunkNum;

    @Schema(description = "用户ID")
    private Long accountId;

    @Schema(description = "是否已完成上传")
    private boolean finished;

    @Schema(description = "已存在分片列表")
    private List<PartSummary> existPartList;

    public static FileChunkResp build(Chunk chunk) {
        return new FileChunkResp()
                .setId(chunk.getId())
                .setIdentifier(chunk.getIdentifier())
                .setUploadId(chunk.getUploadId())
                .setFileName(chunk.getFileName())
                .setBucketName(chunk.getBucketName())
                .setObjectKey(chunk.getObjectKey())
                .setTotalSize(chunk.getTotalSize())
                .setChunkSize(chunk.getChunkSize())
                .setChunkNum(chunk.getChunkNum());
    }
}
