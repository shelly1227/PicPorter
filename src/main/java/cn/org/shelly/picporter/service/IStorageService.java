package cn.org.shelly.picporter.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 对象存储服务接口
 * <p>
 * 该接口定义了与对象存储服务交互的通用操作，实现了S3协议标准，
 * 可用于MinIO、阿里云OSS、AWS S3等兼容S3协议的对象存储服务。
 * 主要功能包括：
 * - 存储桶（Bucket）的创建、删除和查询
 * - 文件（Object）的上传、下载、删除和查询
 * - 大文件分片上传的全流程支持
 * - 预签名URL的生成
 * </p>
 */
public interface IStorageService {

        /* =====================Bucket相关=========================== */

        /**
         * 判断存储桶是否存在
         * <p>
         * 检查指定名称的存储桶是否存在于当前存储服务中
         * </p>
         *
         * @param bucketName 存储桶名称
         * @return true: 存在, false: 不存在
         */
        boolean isBucketExist(String bucketName);

        /**
         * 创建存储桶
         * <p>
         * 在存储服务中创建一个新的存储桶，如果已存在则不会重复创建
         * </p>
         *
         * @param bucketName 存储桶名称（全局唯一）
         */
        void createBucket(String bucketName);

        /**
         * 删除存储桶
         * <p>
         * 删除指定的存储桶，注意：只有空的存储桶才能被删除
         * </p>
         *
         * @param bucketName 存储桶名称
         * @return true: 删除成功, false: 删除失败
         */
        boolean removeBucket(String bucketName);

        /**
         * 列出所有存储桶
         * <p>
         * 获取当前账户下的所有存储桶列表
         * </p>
         *
         * @return 存储桶列表
         */
        List<Bucket> getAllBuckets();

        /* =====================文件处理相关=========================== */

        /**
         * 列出存储桶中的所有对象
         * <p>
         * 获取指定存储桶中的所有文件对象摘要信息
         * </p>
         *
         * @param bucketName 存储桶名称
         * @return 对象摘要列表，包含文件名、大小、修改时间等信息
         */
        List<S3ObjectSummary> listObjects(String bucketName);

        /**
         * 判断文件是否存在
         * <p>
         * 检查指定存储桶中是否存在指定名称的文件对象
         * </p>
         *
         * @param bucketName 存储桶名称
         * @param objectName 文件对象名称（包含路径）
         * @return true: 存在, false: 不存在
         */
        boolean isObjectExist(String bucketName, String objectName);

        /**
         * 上传本地文件
         * <p>
         * 将本地文件系统中的文件上传至指定存储桶中
         * </p>
         *
         * @param bucketName 存储桶名称
         * @param objectName 文件对象名称（包含路径）
         * @param filePath   本地文件路径
         * @return true: 上传成功, false: 上传失败
         */
        boolean uploadFile(String bucketName, String objectName, String filePath);

        /**
         * 上传MultipartFile文件
         * <p>
         * 将Spring的MultipartFile对象上传至指定存储桶中，
         * 适用于Web应用中处理表单上传的文件
         * </p>
         *
         * @param bucketName 存储桶名称
         * @param objectName 文件对象名称（包含路径）
         * @param file       MultipartFile文件对象
         * @return true: 上传成功, false: 上传失败
         */
        boolean uploadFile(String bucketName, String objectName, MultipartFile file);

        /**
         * 删除文件对象
         * <p>
         * 从指定存储桶中删除指定名称的文件对象
         * </p>
         *
         * @param bucketName 存储桶名称
         * @param objectName 文件对象名称（包含路径）
         * @return true: 删除成功, false: 删除失败
         */
        boolean removeObject(String bucketName, String objectName);

        /* =====================文件下载相关=========================== */

        /**
         * 获取文件临时访问URL
         * <p>
         * 生成一个带有有效期的临时访问URL，用于在指定时间内访问私有文件
         * </p>
         *
         * @param bucketName 存储桶名称
         * @param objectName 文件对象名称（包含路径）
         * @param timeout    URL有效期时长
         * @param timeUnit   时间单位（秒、分钟、小时、天等）
         * @return 临时访问URL字符串
         */
        String getObjectURL(String bucketName, String objectName, long timeout, TimeUnit timeUnit);

        /**
         * 文件下载到HTTP响应
         * <p>
         * 将指定存储桶中的文件对象下载并写入到HTTP响应流中，
         * 适用于Web应用中的文件下载场景
         * </p>
         *
         * @param bucketName 存储桶名称
         * @param objectName 文件对象名称（包含路径）
         * @param response   HTTP响应对象，用于写入文件内容
         * @return true: 下载成功, false: 下载失败
         */
        boolean download2Response(String bucketName, String objectName, HttpServletResponse response);

        /* =====================大文件分片上传相关=========================== */

        /**
         * 初始化分片上传任务
         * <p>
         * 创建一个新的分片上传任务，获取唯一的uploadId，
         * 该uploadId用于后续分片上传和合并操作的标识
         * </p>
         * 
         * @param bucketName 存储桶名称
         * @param objectKey  文件对象名称（包含路径）
         * @param metadata   文件元数据，包含文件类型、自定义属性等
         * @return 初始化分片上传结果，包含uploadId等信息
         */
        InitiateMultipartUploadResult initMultipartUploadTask(String bucketName, String objectKey,
                                                              ObjectMetadata metadata);

        /**
         * 生成分片上传预签名URL
         * <p>
         * 为特定分片上传操作生成一个带有临时授权的URL，
         * 客户端可直接通过该URL上传分片，无需经过应用服务器
         * </p>
         * 
         * @param bucketName 存储桶名称
         * @param objectKey  文件对象名称（包含路径）
         * @param httpMethod HTTP请求方法（通常为PUT）
         * @param expiration URL过期时间
         * @param params     签名参数，包含uploadId和partNumber等信息
         * @return 预签名URL对象
         */
        URL genePreSignedUrl(String bucketName, String objectKey, HttpMethod httpMethod, Date expiration,
                             Map<String, Object> params);

        /**
         * 合并文件分片
         * <p>
         * 当所有分片上传完成后，将所有分片合并成一个完整的文件
         * </p>
         * 
         * @param bucketName 存储桶名称
         * @param objectKey  文件对象名称（包含路径）
         * @param uploadId   上传任务ID
         * @param partETags  分片ETag列表，用于验证分片完整性
         * @return 合并分片上传结果
         */
        CompleteMultipartUploadResult mergeChunks(String bucketName, String objectKey, String uploadId,
                                                  List<PartETag> partETags);

        /**
         * 查询已上传分片列表
         * <p>
         * 获取指定上传任务的已上传分片信息，用于断点续传场景
         * </p>
         * 
         * @param bucketName 存储桶名称
         * @param objectKey  文件对象名称（包含路径）
         * @param uploadId   上传任务ID
         * @return 分片列表信息，包含已上传分片的详细信息
         */
        PartListing listMultipart(String bucketName, String objectKey, String uploadId);

        /**
         * 上传单个分片
         * <p>
         * 上传一个分片到存储服务，并返回该分片的ETag信息
         * </p>
         * 
         * @param uploadPartRequest 分片上传请求，包含分片数据和上传参数
         * @return 分片上传结果，包含ETag信息
         */
        PartETag uploadPart(UploadPartRequest uploadPartRequest);
}
