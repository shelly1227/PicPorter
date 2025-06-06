package cn.org.shelly.picporter.model.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value ="chunk")
@Data
@Builder
public class Chunk implements Serializable {
    /**
     * 
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 文件唯一标识（md5）
     */
    @TableField(value = "identifier")
    private String identifier;

    /**
     * 分片上传ID
     */
    @TableField(value = "upload_id")
    private String uploadId;

    /**
     * 文件名
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 所属桶名
     */
    @TableField(value = "bucket_name")
    private String bucketName;

    /**
     * 文件的key
     */
    @TableField(value = "object_key")
    private String objectKey;

    /**
     * 总文件大小（byte）
     */
    @TableField(value = "total_size")
    private Long totalSize;

    /**
     * 每个分片大小（byte）
     */
    @TableField(value = "chunk_size")
    private Long chunkSize;

    /**
     * 分片数量
     */
    @TableField(value = "chunk_num")
    private Integer chunkNum;

    /**
     * 
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 
     */
    @TableField(value = "gmt_modified")
    private Date gmtModified;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}