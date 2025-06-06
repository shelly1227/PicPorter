package cn.org.shelly.picporter.service;

import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.req.FileUploadReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import cn.org.shelly.picporter.model.resp.FileInfoResp;

import java.util.List;

public interface IFileUploadService {
    String upload(FileUploadReq req);

    FileChunkResp initFileChunkTask(FileChunkInitTaskReq req);

    boolean secondUpload(String identifier, String fileName);

    void delete(String identifier);

    boolean uploadPart(String identifier, int partNumber, byte[] bytes);

    String mergeFileChunk(String identifier);

    FileChunkResp listFileChunk(String identifier);

    List<FileInfoResp> list(String fileName, Integer pageNum, Integer pageSize);
}
