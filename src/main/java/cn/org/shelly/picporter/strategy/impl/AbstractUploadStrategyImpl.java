package cn.org.shelly.picporter.strategy.impl;

import cn.hutool.core.io.file.FileNameUtil;
import cn.org.shelly.picporter.exception.CustomException;
import cn.org.shelly.picporter.mapper.ChunkMapper;
import cn.org.shelly.picporter.mapper.FileMapper;
import cn.org.shelly.picporter.model.po.Chunk;
import cn.org.shelly.picporter.model.po.File;
import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.req.FileUploadReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import cn.org.shelly.picporter.model.resp.FileInfoResp;
import cn.org.shelly.picporter.strategy.UploadStrategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传策略抽象类
 * @author Shelly
 */
@Slf4j
@Service
public abstract class AbstractUploadStrategyImpl implements UploadStrategy {

    @Resource
    protected FileMapper fileMapper;

    @Resource
    protected ChunkMapper fileChunkMapper;

    @Value("${upload.prefix}")
    protected String prefix;

    @Value("${upload.is-stored}")
    protected boolean isStored;

    /**
     * 简单文件上传
     * @param req 文件上传请求
     * @return {@link String} 文件url
     */
    @Override
    public String uploadFile(FileUploadReq req) {
        String extName = FileNameUtil.extName(req.getFileName());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "." + extName;
        String objectName = prefix+ "/" + fileName;
        // 上传至MinIO
        boolean uploadSuccess = upload(objectName, req.getFile());
        if (!uploadSuccess) {
            throw new CustomException("上传失败");
        }
        // 存储信息至数据库
        saveFile(req, objectName);
        return getFileAccessUrl(objectName);
    }

    /**
     * 秒传文件上传
     * @param identifier 标识符
     * @param fileName 文件名
     * @return boolean
     */
    @Override
    public boolean secondUpload(String identifier, String fileName) {
        checkIsStored();
        if (StringUtils.isNotBlank(identifier)) {
            File fileDO = fileMapper.selectOne(new QueryWrapper<File>().eq("identifier",identifier));
            return fileDO != null;
        }
        return false;
    }

    /**
     * 删除文件
     * @param identifier 标识符
     */
    @Override
    public void delete(String identifier) {
        checkIsStored();
        File file = fileMapper.selectOne(new QueryWrapper<File>().eq("identifier", identifier));
        if (file != null) {
            // 从存储中删除文件
           boolean removeSuccess = removeObject(file.getObjectKey());
           if (!removeSuccess) {
               throw new CustomException("删除失败");
           }
            // 从数据库删除记录
            fileMapper.deleteById(file.getId());
        }
    }
    @Override
    public FileChunkResp initFileChunkTask(FileChunkInitTaskReq req) {
        checkIsStored();
        String extName = FileNameUtil.extName(req.getFileName()); // 提取扩展名
        String uuid = UUID.randomUUID().toString().replace("-", "");  // 生成无短横线的 UUID
        String fileName = uuid + "." + extName;   // 拼接文件名
        String objectName = prefix + "/" + fileName;
        Chunk fileChunkDO = createFileChunkDO(req, objectName);
        fileChunkMapper.insert(fileChunkDO);
        return FileChunkResp.build(fileChunkDO).setFinished(false).setExistPartList(new ArrayList<>());
    }
    @Override
    public List<FileInfoResp> list(String fileName, Integer pageNum, Integer pageSize) {
        checkIsStored();
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
            resp.setUrl(getFileAccessUrl(file.getObjectKey()));
            // 转换时间格式
            resp.setUploadTime(file.getGmtCreate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            return resp;
        }).toList();
    }
    @Override
    public boolean uploadPart(String identifier, int partNumber, byte[] file) {
        checkIsStored();
        // 检查分片上传任务是否存在
        Chunk fileChunkDO = fileChunkMapper.selectOne(new QueryWrapper<Chunk>().eq("identifier", identifier));
        if (fileChunkDO == null) {
            throw new CustomException("任务不存在");
        }
        return uploadPart(fileChunkDO, partNumber, file);
    }

    @Override
    public String mergeFileChunk(String identifier) {
        checkIsStored();
        // 检查分片上传任务是否存在
        Chunk fileChunkDO = fileChunkMapper.selectOne(new QueryWrapper<Chunk>().eq("identifier", identifier));
        if (fileChunkDO == null) {
            throw new CustomException("任务不存在");
        }
        return merge(fileChunkDO);
    }
    @Override
    public FileChunkResp listFileChunk(String identifier) {
        checkIsStored();
        Chunk task = fileChunkMapper.selectOne(new QueryWrapper<Chunk>().eq("identifier", identifier));
        if (task == null) {
            throw new CustomException("上传任务不存在");
        }
        return listChunks(task);
    }

    /**
     * 列出文件分片
     * @param task 文件分片信息
     * @return {@link FileChunkResp} 文件分片信息
     */
    protected abstract FileChunkResp listChunks(Chunk task);

    /**
     * 合并文件分片
     * @param fileChunkDO 文件分片信息
     * @return {@link String} 文件url
     */
    protected abstract String merge(Chunk fileChunkDO);

    /**
     * 上传分片
     * @param fileChunkDO 文件分片信息
     * @param partNumber 分片编号
     * @param file 文件
     * @return boolean
     */
    protected abstract boolean uploadPart(Chunk fileChunkDO, int partNumber, byte[] file);

    /**
     * 上传文件
     * @param path 路径
     * @param file 文件
     */
    public abstract boolean upload(String path, MultipartFile file);

    /**
     * 获取文件访问url
     *
     * @param filePath 文件路径
     * @return {@link String} 文件url
     */
    public abstract String getFileAccessUrl(String filePath);

    /**
     * 删除文件
     * @param objectKey 对象键
     */
    public abstract boolean removeObject(String objectKey);

    /**
     * 测试
     * @return {@link String}
     */
    public abstract String test();


    /**
     * 创建文件分片信息
     * @param req 文件分片初始化请求
     * @param objectName 对象名称
     * @return {@link Chunk} 文件分片信息
     */
    public abstract Chunk createFileChunkDO(FileChunkInitTaskReq req, String objectName);

    private void checkIsStored() {
        if (!isStored) {
            throw new CustomException("未开启数据库存储功能！");
        }
    }

    void saveFile(FileUploadReq req, String o) {
        checkIsStored();
        String extName = FileNameUtil.extName(req.getFileName());
        // 构建文件数据对象，包含文件名、大小、后缀等基本信息
        File fileDO = File.builder()
                .fileName(req.getFileName())
                .fileSize(req.getFile() != null ? req.getFile().getSize() : req.getSize())
                .fileSuffix(extName)
                .objectKey(o)
                .identifier(req.getIdentifier())
                .build();
        // 插入数据库
        fileMapper.insert(fileDO);
        log.info("文件信息保存成功");
    }
}
