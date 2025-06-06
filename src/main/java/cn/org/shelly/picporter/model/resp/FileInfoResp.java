package cn.org.shelly.picporter.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息DTO
 */
@Data
@Schema(name = "FileInfoResp", description = "文件信息传输对象")
public class FileInfoResp {
	/**
	 * 文件唯一标识
	 */
	@Schema(description = "文件唯一标识（MD5值）", example = "e12a43fd9e24f6dc325aeb7202dd9e3c")
	private String identifier;

	/**
	 * 文件名
	 */
	@Schema(description = "文件名称", example = "测试文档.pdf")
	private String fileName;

	/**
	 * 文件大小（字节）
	 */
	@Schema(description = "文件大小（字节）", example = "1024000")
	private Long fileSize;

	/**
	 * 文件类型
	 */
	@Schema(description = "文件内容类型", example = "pdf")
	private String contentType;

	/**
	 * 文件URL
	 */
	@Schema(description = "文件访问URL", example = "http://minio-server:9000/bucket-name/2023-03-24/test.pdf?token=xxx")
	private String url;

	/**
	 * 上传时间
	 */
	@Schema(description = "文件上传时间", example = "2023-03-24T10:15:30")
	private LocalDateTime uploadTime;
}