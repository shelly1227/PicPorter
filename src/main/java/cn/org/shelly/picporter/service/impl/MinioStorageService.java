package cn.org.shelly.picporter.service.impl;

import cn.org.shelly.picporter.service.IStorageService;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements IStorageService {

    private final AmazonS3 amazonS3Client;

    /**
     * 检查指定的 bucket 是否存在。
     *
     * @param bucketName bucket 名称
     * @return 存在返回 true，否则返回 false
     */
    @Override
    public boolean isBucketExist(String bucketName) {
        try {
            return amazonS3Client.doesBucketExistV2(bucketName);
        } catch (Exception e) {
            log.error("检查 bucket 存在异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建指定的 bucket。
     *
     * @param bucketName bucket 名称
     */
    @Override
    public void createBucket(String bucketName) {
        if (!isBucketExist(bucketName)) {
            try {
                amazonS3Client.createBucket(bucketName);
            } catch (Exception e) {
                log.error("创建 bucket 异常: {}", e.getMessage(), e);
                throw new RuntimeException("创建 bucket 失败", e);
            }
        }
    }

    /**
     * 删除指定的 bucket。
     *
     * @param bucketName bucket 名称
     * @return 删除成功返回 true，否则返回 false
     */
    @Override
    public boolean removeBucket(String bucketName) {
        try {
            if (isBucketExist(bucketName)) {
                // 判断存储桶内是否有文件
                ObjectListing objectListing = amazonS3Client.listObjects(bucketName);
                while (true) {
                    for (S3ObjectSummary os : objectListing.getObjectSummaries()) {
                        amazonS3Client.deleteObject(bucketName, os.getKey());
                    }
                    if (objectListing.isTruncated()) {
                        objectListing = amazonS3Client.listNextBatchOfObjects(objectListing);
                    } else {
                        break;
                    }
                }
                amazonS3Client.deleteBucket(bucketName);
                return true;
            }
        } catch (Exception e) {
            log.error("删除 bucket 异常: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 获取所有 bucket 列表。
     *
     * @return bucket 列表
     */
    @Override
    public List<Bucket> getAllBuckets() {
        try {
            return amazonS3Client.listBuckets();
        } catch (Exception e) {
            log.error("获取所有 bucket 列表异常: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 列出指定 bucket 中的所有对象。
     *
     * @param bucketName bucket 名称
     * @return 对象列表
     */
    @Override
    public List<S3ObjectSummary> listObjects(String bucketName) {
        try {
            if (isBucketExist(bucketName)) {
                ListObjectsV2Result objects = amazonS3Client.listObjectsV2(bucketName);
                return objects.getObjectSummaries();
            }
        } catch (Exception e) {
            log.error("列出 bucket 对象异常: {}", e.getMessage(), e);
        }
        return List.of();
    }

    /**
     * 检查指定对象是否存在。
     *
     * @param bucketName bucket 名称
     * @param objectName 对象名称
     * @return 存在返回 true，否则返回 false
     */
    @Override
    public boolean isObjectExist(String bucketName, String objectName) {
        try {
            return amazonS3Client.doesObjectExist(bucketName, objectName);
        } catch (Exception e) {
            log.error("检查对象存在异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 上传文件到指定 bucket。
     *
     * @param bucketName bucket 名称
     * @param objectName 对象名称
     * @param filePath   文件路径
     * @return 上传成功返回 true，否则返回 false
     */
    @Override
    public boolean uploadFile(String bucketName, String objectName, String filePath) {
        try {
            amazonS3Client.putObject(bucketName, objectName, new File(filePath));
            return true;
        } catch (Exception e) {
            log.error("上传文件异常: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 上传 MultipartFile 到指定 bucket。
     *
     * @param bucketName bucket 名称
     * @param objectName 对象名称
     * @param file       MultipartFile 文件
     * @return 上传成功返回 true，否则返回 false
     */
    @Override
    public boolean uploadFile(String bucketName, String objectName, MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            amazonS3Client.putObject(bucketName, objectName, in, metadata);
            return true;
        } catch (Exception e) {
            log.error("上传文件异常: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 删除指定 bucket 中的对象。
     *
     * @param bucketName bucket 名称
     * @param objectName 对象名称
     * @return 删除成功返回 true，否则返回 false
     */
    @Override
    public boolean removeObject(String bucketName, String objectName) {
        try {
            amazonS3Client.deleteObject(bucketName, objectName);
            return true;
        } catch (Exception e) {
            log.error("删除对象异常: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 生成指定对象的预签名 URL。
     *
     * @param bucketName bucket 名称
     * @param objectName 对象名称
     * @param timeout    超时时间
     * @param timeUnit   时间单位
     * @return 预签名 URL 字符串
     */
    @Override
    public String getObjectURL(String bucketName, String objectName, long timeout, TimeUnit timeUnit) {
        try {
            if (timeout <= 0) {
                throw new IllegalArgumentException("超时时间必须大于 0");
            }
            Date expireDate = new Date(System.currentTimeMillis() + timeUnit.toMillis(timeout));
            return amazonS3Client.generatePresignedUrl(bucketName, objectName, expireDate).toString();
        } catch (Exception e) {
            log.error("生成预签名 URL 异常: {}", e.getMessage(), e);
            throw new RuntimeException("生成预签名 URL 失败", e);
        }
    }

    /**
     * 下载指定对象并输出到 HttpServletResponse。
     *
     * @param bucketName bucket 名称
     * @param objectName 对象名称
     * @param response   HttpServletResponse 响应对象
     * @return 下载成功返回 true，否则返回 false
     */
    @SneakyThrows
    @Override
    public boolean download2Response(String bucketName, String objectName, HttpServletResponse response) {
        try {
            // 防止路径遍历攻击
            if (objectName.contains("..")) {
                throw new IllegalArgumentException("非法路径");
            }

            S3Object object = amazonS3Client.getObject(bucketName, objectName);
            String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setContentType(object.getObjectMetadata().getContentType());
            response.setCharacterEncoding("UTF-8");
            IOUtils.copy(object.getObjectContent(), response.getOutputStream());
            return true;
        } catch (Exception e) {
            log.error("下载文件异常: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public InitiateMultipartUploadResult initMultipartUploadTask(String bucketName, String objectKey,
            ObjectMetadata metadata) {
        try {
            InitiateMultipartUploadRequest initReq = new InitiateMultipartUploadRequest(bucketName, objectKey,
                    metadata);
            return amazonS3Client.initiateMultipartUpload(initReq);
        } catch (Exception e) {
            log.error("初始化分片上传任务异常: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public URL genePreSignedUrl(String bucketName, String objectKey, HttpMethod httpMethod, Date expiration,
                                Map<String, Object> params) {
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey, httpMethod)
                    .withExpiration(expiration);
            if (params != null && !params.isEmpty()) {
                /*
                 * 遍历params作为参数加到request里面，比如 添加上传ID和分片编号作为请求参数
                 * request.addRequestParameter("uploadId", uploadId);
                 * request.addRequestParameter("partNumber", String.valueOf(i));
                 */
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    request.addRequestParameter(entry.getKey(), entry.getValue().toString());
                }
            }
            return amazonS3Client.generatePresignedUrl(request);
        } catch (Exception e) {
            log.error("生成预签名 URL 异常: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public CompleteMultipartUploadResult mergeChunks(String bucketName, String objectKey, String uploadId,
            List<PartETag> partETags) {
        try {
            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId,
                    partETags);
            return amazonS3Client.completeMultipartUpload(request);
        } catch (Exception e) {
            log.error("合并分片异常: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public PartListing listMultipart(String bucketName, String objectKey, String uploadId) {
        try {
            ListPartsRequest request = new ListPartsRequest(bucketName, objectKey, uploadId);
            return amazonS3Client.listParts(request);
        } catch (Exception e) {
            log.error("列出分片异常: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public PartETag uploadPart(UploadPartRequest uploadPartRequest) {
        try {
            return amazonS3Client.uploadPart(uploadPartRequest).getPartETag();
        } catch (Exception e) {
            log.error("上传分片异常: {}", e.getMessage(), e);
            throw new RuntimeException("上传分片失败", e);
        }
    }
}
