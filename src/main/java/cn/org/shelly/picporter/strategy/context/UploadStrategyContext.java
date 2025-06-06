package cn.org.shelly.picporter.strategy.context;

import cn.org.shelly.picporter.enums.UploadModeEnum;
import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.req.FileUploadReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import cn.org.shelly.picporter.model.resp.FileInfoResp;
import cn.org.shelly.picporter.strategy.UploadStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 上传策略上下文
 * @author Shelly
 */
@Service
public class UploadStrategyContext {
    /**
     * 上传模式
     */
    @Value("${upload.strategy}")
    private String uploadStrategy;

    @Autowired
    private Map<String, UploadStrategy> uploadStrategyMap;

    public String executeUploadStrategy(FileUploadReq req) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).uploadFile(req);
    }

    public boolean secondUpload(String identifier, String fileName) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).secondUpload(identifier, fileName);
    }

    public void delete(String identifier) {
        uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).delete(identifier);
    }

    public List<FileInfoResp> list(String fileName, Integer pageNum, Integer pageSize) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).list(fileName, pageNum, pageSize);
    }

    public FileChunkResp listFileChunk(String identifier) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).listFileChunk(identifier);
    }

    public String mergeFileChunk(String identifier) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).mergeFileChunk(identifier);
    }

    public boolean uploadPart(String identifier, int partNumber, byte[] bytes) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).uploadPart(identifier, partNumber, bytes);
    }

    public FileChunkResp initFileChunkTask(FileChunkInitTaskReq req) {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).initFileChunkTask(req);
    }

    public String test() {
        return uploadStrategyMap.get(UploadModeEnum.getStrategy(uploadStrategy)).test();
    }
}