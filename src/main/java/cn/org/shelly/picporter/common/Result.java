package cn.org.shelly.picporter.common;

import cn.org.shelly.picporter.constants.CodeEnum;
import lombok.Data;

/**
 * 通用返回类
 * @author shelly
 */
@Data
public class Result<T> {

    Integer code;
    String message;
    T data;

    Result() {
    }

    public static <T> Result<T> fail() {
        return Result.fail(CodeEnum.FAIL);
    }

    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.code = CodeEnum.FAIL.getCode();
        result.message = message;
        return result;
    }

    public static <T> Result<T> fail(CodeEnum codeMsg) {
        Result<T> result = new Result<>();
        result.code = codeMsg.getCode();
        result.message = codeMsg.getMsg();
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> Result<T> success(T data) {
        return buildResult(data, CodeEnum.SUCCESS.getCode());
    }

    public static <T> Result<T> isSuccess(Boolean isSuccess) {
        if (Boolean.TRUE.equals(isSuccess)) {
            return success();
        } else {
            return fail();
        }
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.message = CodeEnum.SUCCESS.getMsg();
        result.code = CodeEnum.SUCCESS.getCode();
        return result;
    }

    /**
     * 返回结果
     */
    public static <T> Result<T> result(CodeEnum codeEnum) {
        return new Result<T>().message(codeEnum.getMsg()).code(codeEnum.getCode());
    }

    public Result<T> message(String message) {
        this.message = message;
        return this;
    }

    public Result<T> code(int code) {
        this.code = code;
        return this;
    }
    private static <T> Result<T> buildResult(T data, Integer code) {
        Result<T> r = new Result<>();
        r.setData(data);
        r.setCode(code);
        return r;
    }
}
