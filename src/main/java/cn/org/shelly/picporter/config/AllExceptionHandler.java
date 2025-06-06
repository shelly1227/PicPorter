package cn.org.shelly.picporter.config;

import cn.org.shelly.picporter.common.Result;
import cn.org.shelly.picporter.constants.CodeEnum;
import cn.org.shelly.picporter.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestControllerAdvice
public class AllExceptionHandler {


    /**
     * 服务器异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> deException(RuntimeException ex) {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        log.error("端口" + request.getServletPath() + "发生异常:", ex);
        log.error("发生异常:", ex);
        return Result.fail().message(CodeEnum.SYSTEM_REPAIR.getMsg() + "\n异常信息：" + ex.getMessage()).code(CodeEnum.SYSTEM_REPAIR.getCode());
    }

    /**
     * 自定义异常
     */
    @ExceptionHandler(CustomException.class)
    public Result<?> handleCustomException(CustomException ex) {
        log.error("异常信息：",ex);
        return Result.fail().message(ex.getMessage()).code(ex.getCode());
    }
}
