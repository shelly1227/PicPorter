package cn.org.shelly.picporter.strategy.impl;

import cn.org.shelly.picporter.config.properties.MinioProperties;
import cn.org.shelly.picporter.exception.CustomException;
import cn.org.shelly.picporter.model.po.Chunk;
import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.req.FileUploadReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.List;

/**
 * Minio上传策略
 * @author Shelly
 */
@Service("minioUploadStrategyImpl")
@Slf4j
public class MinioUploadStrategyImpl extends AbstractUploadStrategyImpl{
    @Resource
    private MinioProperties minioProperties;

    @Resource
    private AmazonS3 amazonS3Client;


    @Override
    protected FileChunkResp listChunks(Chunk task) {
        FileChunkResp result = FileChunkResp.build(task);
        boolean objectExist = isObjectExist(task.getObjectKey());
        //不存在，则获取已上传的分片列表
        if (!objectExist) {
            List<PartSummary> parts = listMultipart(task.getObjectKey(), task.getUploadId()).getParts();
            result.setExistPartList(parts);
            result.setFinished(parts.size() == task.getChunkNum());
        }
        return result;
    }

    @Override
    protected String merge(Chunk fileChunkDO) {
        // 检查所有分片是否已上传
        ListPartsRequest req = new ListPartsRequest(fileChunkDO.getBucketName(), fileChunkDO.getObjectKey(), fileChunkDO.getUploadId());
        PartListing partListing = amazonS3Client.listParts(req);
        List<PartSummary> parts = partListing.getParts();
        if (parts.size() != fileChunkDO.getChunkNum()) {
            throw new CustomException("分片未全部上传");
        }
        // 计算文件实际大小
        long fileSize = parts.stream().mapToLong(PartSummary::getSize).sum();
        // 合并文件
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(fileChunkDO.getBucketName(), fileChunkDO.getObjectKey(), fileChunkDO.getUploadId(),
                parts.stream()
                        .map(partSummary -> new PartETag(partSummary.getPartNumber(), partSummary.getETag()))
                        .toList());
        CompleteMultipartUploadResult result =  amazonS3Client.completeMultipartUpload(request);
        // 判断是否成功，然后存储信息到数据库
        if (result.getETag() != null) {
            FileUploadReq fileUploadReq = FileUploadReq.builder()
                    .fileName(fileChunkDO.getFileName())
                    .identifier(fileChunkDO.getIdentifier())
                    .size(fileSize)
                    .file(null)
                    .build();
            // 存储文件信息到数据库
            saveFile(fileUploadReq, fileChunkDO.getObjectKey());
            // 删除分片上传记录
            fileChunkMapper.deleteById(fileChunkDO.getId());
            String s = MinioProperties.URL + "/" + fileChunkDO.getBucketName() + "/" + fileChunkDO.getObjectKey();
            log.info("文件地址：{}", s);
            return s;
        } else {
            throw new CustomException("分片合并失败");
        }
    }

    @Override
    protected boolean uploadPart(Chunk fileChunkDO, int partNumber, byte[] file) {
        try {
            // 使用AmazonS3原生API上传分片
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withBucketName(fileChunkDO.getBucketName())
                    .withKey(fileChunkDO.getObjectKey())
                    .withUploadId(fileChunkDO.getUploadId())
                    .withPartNumber(partNumber)
                    .withPartSize(file.length)
                    .withInputStream(new java.io.ByteArrayInputStream(file));
            // 执行上传并获取结果
            PartETag partETag = amazonS3Client.uploadPart(uploadPartRequest).getPartETag();
            log.info("分片上传成功, partNumber={}, etag={}", partNumber, partETag.getETag());
            return true;
        } catch (Exception e) {
            log.error("分片上传失败, partNumber={}, error={}",  partNumber, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean upload(String path, InputStream stream, long size, String type) {
        try (InputStream in = stream) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(type);
            metadata.setContentLength(size);
            amazonS3Client.putObject(minioProperties.bucketName, path, in, metadata);
            return true;
        } catch (Exception e) {
            log.error("上传文件异常: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return minioProperties.url + "/" + minioProperties.bucketName + "/" + filePath;
    }

    @Override
    public boolean removeObject(String objectKey) {
        try {
            amazonS3Client.deleteObject(minioProperties.bucketName, objectKey);
            return true;
        } catch (Exception e) {
            log.error("删除对象异常: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public String test() {
        return "Hello, Minio";
    }

    @Override
    public Chunk createFileChunkDO(FileChunkInitTaskReq req, String objectName) {
        // 根据文件名（后缀）获取文件类型
        String contentType = MediaTypeFactory.getMediaType(objectName).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
        // 通过MINIO获取分片上传任务ID
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(req.getTotalSize());
        InitiateMultipartUploadResult uploadResult = initMultipartUploadTask(MinioProperties.BUCKET_NAME, objectName, metadata);
        String uploadId = uploadResult.getUploadId();
        // 存储分片上传任务信息
        int chunkNum = (int) Math.ceil(req.getTotalSize() * 1.0 / req.getChunkSize());
        return Chunk.builder()
                .identifier(req.getIdentifier())
                .uploadId(uploadId)
                .fileName(req.getFileName())
                .bucketName(MinioProperties.BUCKET_NAME)
                .objectKey(objectName)
                .totalSize(req.getTotalSize())
                .chunkSize(req.getChunkSize())
                .chunkNum(chunkNum).build();
    }

    public PartListing listMultipart(String objectKey, String uploadId) {
        try {
            ListPartsRequest request = new ListPartsRequest(minioProperties.bucketName, objectKey, uploadId);
            return amazonS3Client.listParts(request);
        } catch (Exception e) {
            log.error("列出分片异常: {}", e.getMessage(), e);
        }
        return null;
    }


    public boolean isObjectExist(String objectKey) {
        try {
            return amazonS3Client.doesObjectExist(minioProperties.bucketName, objectKey);
        } catch (Exception e) {
            log.error("检查对象存在异常: {}", e.getMessage(), e);
            return false;
        }
    }
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
}
