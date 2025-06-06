package cn.org.shelly.picporter.model.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户文件表
 * @TableName file
 */
@TableName(value ="file")
@Data
@Builder
public class File implements Serializable {
    /**
     * 文件id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 文件名称，秒传需要用到，冗余存储
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件的后缀拓展名，冗余存储
     */
    @TableField(value = "file_suffix")
    private String fileSuffix;

    /**
     * 文件大小，字节，冗余存储
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 文件的key, 格式 日期/md5.拓展名，比如 2025-03-13/921674fd-cdaf-459a-be7b-109469e7050d.png
     */
    @TableField(value = "object_key")
    private String objectKey;

    /**
     * 唯一标识，文件MD5
     */
    @TableField(value = "identifier")
    private String identifier;

    /**
     * 创建者
     */
    @TableField(value = "create_by")
    private String createBy;

    /**
     * 更新者
     */
    @TableField(value = "update_by")
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified")
    private Date gmtModified;

    /**
     * 逻辑删除（0：未删除 1：已删除）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}