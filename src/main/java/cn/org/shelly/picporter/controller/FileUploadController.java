package cn.org.shelly.picporter.controller;

import cn.org.shelly.picporter.common.Result;
import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.req.FileUploadReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import cn.org.shelly.picporter.model.resp.FileInfoResp;
import cn.org.shelly.picporter.strategy.context.UploadStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制层
 * @author Shelly
 */
@RestController
@RequestMapping("/upload")
@Slf4j
@RequiredArgsConstructor
public class FileUploadController {

    private final UploadStrategyContext uploadStrategyContext;

    /**
     * 小文件上传
     * <p>
     * 用于上传较小的文件，直接将文件上传到存储服务
     * </p>
     */
    @PostMapping(value = "/tiny", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadFile(
            @RequestParam("fileName") String fileName,
            @RequestParam("identifier") String identifier,
            @RequestParam("size") Long size,
            @RequestParam("file") MultipartFile file
    ) {
        FileUploadReq req = FileUploadReq.builder()
                .fileName(fileName)
                .identifier(identifier)
                .size(size)
                .file(file)
                .build();
        return Result.success(uploadStrategyContext.executeUploadStrategy(req));
    }

    /**
     * 初始化文件分片上传任务
     * <p>
     * 创建大文件分片上传任务，生成唯一的uploadId
     * </p>
     *
     * @param req 初始化分片任务请求，包含文件信息、分片大小等
     * @return 分片任务信息，包含uploadId和分片数量
     */
    @PostMapping("/initShardTask")
    public Result<FileChunkResp> initFileChunkTask(@RequestBody FileChunkInitTaskReq req) {
        FileChunkResp fileChunkDTO = uploadStrategyContext.initFileChunkTask(req);
        return Result.success(fileChunkDTO);
    }

    /**
     * 秒传文件
     * <p>
     * 基于文件MD5特征值判断服务器是否已存在相同文件，如存在则直接返回成功，无需实际上传
     * </p>
     */
    @PostMapping("/second")
    public Result<Boolean> secondUpload(String identifier, String fileName) {
        boolean uploadSuccess = uploadStrategyContext.secondUpload(identifier, fileName);
        return Result.success(uploadSuccess);
    }

    /**
     * 删除文件
     * <p>
     * 根据文件唯一标识（MD5值）删除文件，包括从存储中删除文件和数据库记录
     * </p>
     *
     * @param identifier 文件唯一标识（MD5值）
     * @return 操作结果
     */
    @DeleteMapping("/{identifier}")
    public Result<Void> delete(@PathVariable("identifier") String identifier) {
        uploadStrategyContext.delete(identifier);
        return Result.success();
    }
    /**
     * 直接上传分片
     * <p>
     * 通过服务端代理将分片上传到存储服务，避免客户端直接与存储服务通信
     * </p>
     *
     * @param identifier 文件唯一标识
     * @param partNumber 分片序号，从1开始
     * @param file       分片文件
     * @return 上传结果
     */
    @PostMapping("/uploadPart/{identifier}/{partNumber}")
    public Result<Void> uploadPart(@PathVariable("identifier") String identifier,
                                   @PathVariable("partNumber") int partNumber,
                                   @RequestParam("file") MultipartFile file) {
        try {
            boolean success = uploadStrategyContext.uploadPart(identifier, partNumber, file.getBytes());
            return Result.isSuccess(success);
        } catch (Exception e) {
            return Result.fail();
        }
    }
    /**
     * 合并文件分片
     * <p>
     * 将已上传的分片合并为完整文件
     * </p>
     *
     * @param identifier 文件唯一标识
     * @return 合并后的文件路径
     */
    @PostMapping("merge/{identifier}")
    public Result<String> mergeFileChunk(@PathVariable("identifier") String identifier) {
        return Result.success(uploadStrategyContext.mergeFileChunk(identifier));
    }
    /**
     * 获取文件分片上传进度
     * <p>
     * 查询已上传的分片信息，用于断点续传
     * </p>
     *
     * @param identifier 文件唯一标识
     * @return 文件分片信息，包含已上传分片列表
     * @description 查询已上传的分片信息，用于断点续传
     */
    @GetMapping("/progress/{identifier}")
    public Result<FileChunkResp> getFileChunkUploadProgress(@PathVariable("identifier") String identifier) {
        FileChunkResp fileChunkUploadProgressDTO = uploadStrategyContext.listFileChunk(identifier);
        return Result.success(fileChunkUploadProgressDTO);
    }
    /**
     * 获取文件列表
     * <p>
     * 根据文件名模糊查询已上传的文件列表，支持分页查询
     * </p>
     * @param fileName 文件名关键字，用于模糊查询，可为空
     * @param pageNum  页码，默认为1
     * @param pageSize 每页记录数，默认为10
     * @return 文件信息列表，包含文件基本信息和预签名URL
     */
    @GetMapping("/list")
    public Result<List<FileInfoResp>> list(@RequestParam(required = false) String fileName,
                                           @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                           @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<FileInfoResp> fileList = uploadStrategyContext.list(fileName, pageNum, pageSize);
        return Result.success(fileList);
    }

    /**
     * 测试方法
     * @return 测试结果
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success(uploadStrategyContext.test());
    }
}
