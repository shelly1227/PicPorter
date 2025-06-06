package cn.org.shelly.picporter.service.impl;

import cn.org.shelly.picporter.config.properties.MinioProperties;
import cn.org.shelly.picporter.exception.CustomException;
import cn.org.shelly.picporter.mapper.ChunkMapper;
import cn.org.shelly.picporter.mapper.FileMapper;
import cn.org.shelly.picporter.model.po.Chunk;
import cn.org.shelly.picporter.model.po.File;
import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.req.FileUploadReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import cn.org.shelly.picporter.model.resp.FileInfoResp;
import cn.org.shelly.picporter.service.IFileUploadService;
import cn.org.shelly.picporter.service.IStorageService;
import com.amazonaws.services.s3.model.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.io.file.FileNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadService implements IFileUploadService {
    private final FileMapper fileMapper;
    private final MinioProperties minioProperties;
    private final IStorageService storageService;
    private final ChunkMapper fileChunkMapper;
    @Override
    public String upload(FileUploadReq req) {
        String extName = FileNameUtil.extName(req.getFileName()); // 提取扩展名
        String uuid = UUID.randomUUID().toString().replace("-", "");  // 生成无短横线的 UUID
        String fileName = uuid + "." + extName;   // 拼接文件名
        String path = minioProperties.prefix;
        String objectName = path + "/" + fileName;
        // 上传至MinIO
        boolean uploadSuccess = storageService.uploadFile(MinioProperties.BUCKET_NAME, objectName, req.getFile());
        if (!uploadSuccess) {
            throw new CustomException("上传失败");
        }
        objectName = MinioProperties.URL + "/" + MinioProperties.BUCKET_NAME + "/" + objectName;
        // 存储信息至数据库
        saveFile(req, objectName);
        return objectName;
    }

    @Override
    public FileChunkResp initFileChunkTask(FileChunkInitTaskReq req) {
        String extName = FileNameUtil.extName(req.getFileName()); // 提取扩展名
        String uuid = UUID.randomUUID().toString().replace("-", "");  // 生成无短横线的 UUID
        String fileName = uuid + "." + extName;   // 拼接文件名
        String path = minioProperties.prefix;
        String objectName = path + "/" + fileName;
        // 根据文件名（后缀）获取文件类型
        String contentType = MediaTypeFactory.getMediaType(objectName).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
        // 通过MINIO获取分片上传任务ID
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(req.getTotalSize());
        InitiateMultipartUploadResult uploadResult = storageService.initMultipartUploadTask(MinioProperties.BUCKET_NAME, objectName, metadata);
        String uploadId = uploadResult.getUploadId();
        // 存储分片上传任务信息
        int chunkNum = (int) Math.ceil(req.getTotalSize() * 1.0 / req.getChunkSize());
        Chunk fileChunkDO = Chunk.builder()
                .identifier(req.getIdentifier())
                .uploadId(uploadId)
                .fileName(req.getFileName())
                .bucketName(MinioProperties.BUCKET_NAME)
                .objectKey(objectName)
                .totalSize(req.getTotalSize())
                .chunkSize(req.getChunkSize())
                .chunkNum(chunkNum).build();
        fileChunkMapper.insert(fileChunkDO);
        return FileChunkResp.build(fileChunkDO).setFinished(false).setExistPartList(new ArrayList<>());
    }

    @Override
    public boolean secondUpload(String identifier, String fileName) {
        if (StringUtils.isNotBlank(identifier)) {
            // 根据文件唯一标识查询数据库
            File fileDO = fileMapper.selectOne(
                    new QueryWrapper<File>()
                            .eq("identifier",identifier));
            // 文件已存在则返回true，表示秒传成功
            return fileDO != null;
        }
        return false;
    }

    @Override
    public void delete(String identifier) {
        File file = fileMapper.selectOne(new QueryWrapper<File>().eq("identifier", identifier));
        if (file != null) {
            // 从存储中删除文件
            storageService.removeObject(MinioProperties.BUCKET_NAME, file.getObjectKey());
            // 从数据库删除记录
            fileMapper.deleteById(file.getId());
        }
    }

    @Override
    public boolean uploadPart(String identifier, int partNumber, byte[] file) {
        try {
            // 检查分片上传任务是否存在
            Chunk fileChunkDO = fileChunkMapper.selectOne(new QueryWrapper<Chunk>().eq("identifier", identifier));
            if (fileChunkDO == null) {
                throw new CustomException("任务不存在");
            }
            // 使用AmazonS3原生API上传分片
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withBucketName(fileChunkDO.getBucketName())
                    .withKey(fileChunkDO.getObjectKey())
                    .withUploadId(fileChunkDO.getUploadId())
                    .withPartNumber(partNumber)
                    .withPartSize(file.length)
                    .withInputStream(new java.io.ByteArrayInputStream(file));
            // 执行上传并获取结果
            PartETag partETag = storageService.uploadPart(uploadPartRequest);
            log.info("分片上传成功 identifier={}, partNumber={}, etag={}", identifier, partNumber, partETag.getETag());
            return true;
        } catch (Exception e) {
            log.error("分片上传失败 identifier={}, partNumber={}, error={}", identifier, partNumber, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String mergeFileChunk(String identifier) {
        // 检查分片上传任务是否存在
        Chunk fileChunkDO = fileChunkMapper.selectOne(new QueryWrapper<Chunk>().eq("identifier", identifier));
        if (fileChunkDO == null) {
            throw new CustomException("任务不存在");
        }
        // 检查所有分片是否已上传
        PartListing partListing = storageService.listMultipart(fileChunkDO.getBucketName(), fileChunkDO.getObjectKey(),
                fileChunkDO.getUploadId());
        List<PartSummary> parts = partListing.getParts();
        if (parts.size() != fileChunkDO.getChunkNum()) {
            throw new CustomException("分片未全部上传");
        }
        // 计算文件实际大小
        long fileSize = parts.stream().mapToLong(PartSummary::getSize).sum();

        // 合并文件
        CompleteMultipartUploadResult result = storageService.mergeChunks(
                fileChunkDO.getBucketName(), fileChunkDO.getObjectKey(), fileChunkDO.getUploadId(),
                parts.stream()
                        .map(partSummary -> new PartETag(partSummary.getPartNumber(), partSummary.getETag()))
                        .toList());

        // 判断是否成功，然后存储信息到数据库
        if (result.getETag() != null) {
            FileUploadReq fileUploadReq = FileUploadReq.builder()
                    .fileName(fileChunkDO.getFileName())
                    .identifier(identifier)
                    .size(fileSize)
                    .file(null)
                    .build();
            String s =  MinioProperties.URL + "/" + fileChunkDO.getBucketName() + "/" + fileChunkDO.getObjectKey();
            log.info("文件地址：{}", s);
            // 存储文件信息到数据库
            saveFile(fileUploadReq, s);
            // 删除分片上传记录
            fileChunkMapper.deleteById(fileChunkDO.getId());
            log.info("分片合并成功");
            return s;
        } else {
            throw new CustomException("分片合并失败");
        }
    }

    @Override
    public FileChunkResp listFileChunk(String identifier) {
        Chunk task = fileChunkMapper.selectOne(new QueryWrapper<Chunk>().eq("identifier", identifier));
        if (task == null) {
            throw new CustomException("上传任务不存在");
        }
        FileChunkResp result = FileChunkResp.build(task);

        boolean objectExist = storageService.isObjectExist(task.getBucketName(), task.getObjectKey());
        //不存在，则获取已上传的分片列表
        if (!objectExist) {
            List<PartSummary> parts = storageService.listMultipart(task.getBucketName(), task.getObjectKey(), task.getUploadId()).getParts();
            result.setExistPartList(parts);
            result.setFinished(parts.size() == task.getChunkNum());
        }
        // 前端根据返回值进行判断，如果finished为空，则表示分片上传任务已经完成，如果为true则表示分片已全部上传完成，需要调用合并接口，如果为false则表示仍有未完成的分片
        return result;
    }

    @Override
    public List<FileInfoResp> list(String fileName, Integer pageNum, Integer pageSize) {
        // 构建查询条件
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(fileName)) {
            queryWrapper.like("file_name", fileName);
        }
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("gmt_create");

        // 查询文件列表
        List<File> files = fileMapper.selectList(queryWrapper);
        // 转换为resp对象
        return files.stream().map(file -> {
            FileInfoResp resp = new FileInfoResp();
            resp.setIdentifier(file.getIdentifier());
            resp.setFileName(file.getFileName());
            resp.setFileSize(file.getFileSize());
            resp.setContentType(file.getFileSuffix());
            // 生成有效期为1天的文件访问URL
            resp.setUrl(storageService.getObjectURL(MinioProperties.BUCKET_NAME, file.getObjectKey(), 1L,
                    TimeUnit.DAYS));
            // 转换时间格式
            resp.setUploadTime(file.getGmtCreate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            return resp;
        }).toList();
    }
    public void saveFile(FileUploadReq req, String objectKey) {
        String extName = FileNameUtil.extName(req.getFileName());
        // 构建文件数据对象，包含文件名、大小、后缀等基本信息
        File fileDO = File.builder()
                .fileName(req.getFileName())
                .fileSize(req.getFile() != null ? req.getFile().getSize() : req.getSize())
                .fileSuffix(extName)
                .objectKey(objectKey)
                .identifier(req.getIdentifier())
                .build();
        // 插入数据库
        fileMapper.insert(fileDO);
        log.info("文件信息保存成功");
    }
}
