package cn.org.shelly.picporter.strategy.impl;

import cn.org.shelly.picporter.config.properties.OssProperties;
import cn.org.shelly.picporter.constants.CodeEnum;
import cn.org.shelly.picporter.exception.CustomException;
import cn.org.shelly.picporter.model.po.Chunk;
import cn.org.shelly.picporter.model.req.FileChunkInitTaskReq;
import cn.org.shelly.picporter.model.resp.FileChunkResp;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * OSS上传策略实现
 * @author shelly
 */
@Slf4j
@Service("ossUploadStrategyImpl")
public class OSSUploadStrategyImpl extends AbstractUploadStrategyImpl {
    @Resource
    private OssProperties ossProperties;
    @Override
    protected FileChunkResp listChunks(Chunk task) {
        throw new CustomException(CodeEnum.SYSTEM_UNSUPPORTED_FUNCTION);
    }

    @Override
    protected String merge(Chunk fileChunkDO) {
        throw new CustomException(CodeEnum.SYSTEM_UNSUPPORTED_FUNCTION);
    }

    @Override
    protected boolean uploadPart(Chunk fileChunkDO, int partNumber, byte[] file) {
        throw new CustomException(CodeEnum.SYSTEM_UNSUPPORTED_FUNCTION);
    }

    @Override
    public boolean upload(String path, MultipartFile file) {
        OSS ossClient = getOssClient();
        try {
            // 调用 OSS 方法上传
            ossClient.putObject(ossProperties.getBucketName(), path, file.getInputStream());
            return true; // 上传成功
        } catch (OSSException oe) {
            log.error("OSS 异常：{}", oe.getErrorMessage());
            log.error("错误码：{}", oe.getErrorCode());
            log.info("Request ID：{}", oe.getRequestId());
            log.info("Host ID：{}", oe.getHostId());
            return false;
        } catch (ClientException ce) {
            log.error("客户端异常：{}", ce.getMessage());
            return false;
        } catch (IOException e) {
            log.error("IO异常：{}", e.getMessage(), e);
            throw new CustomException("上传失败：读取文件流异常", 500);
        } finally {
            ossClient.shutdown();
        }
    }


    @Override
    public String getFileAccessUrl(String filePath) {
        return ossProperties.getUrl() + filePath;
    }

    @Override
    public boolean removeObject(String objectKey) {
        OSS ossClient = getOssClient();
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
            return true;
        } catch (Exception e) {
            log.error("删除对象异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String test() {
        return "Hello, oss";
    }

    @Override
    public Chunk createFileChunkDO(FileChunkInitTaskReq req, String objectName) {
        throw new CustomException(CodeEnum.SYSTEM_UNSUPPORTED_FUNCTION);
    }
    /**
     * 获取ossClient
     *
     * @return {@link OSS} ossClient
     */
    private OSS getOssClient() {
        return new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
    }
}
