package com.abc.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

//异常处理
@RestControllerAdvice
public class ProjectExceptionAdvice {
    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Result handleNotImplemented(UnsupportedOperationException ex) {
        return new Result(null, Code.SYSTEM_ERR, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result doException(Exception ex){
        return new Result(null, Code.SYSTEM_ERR, "Internal application error");
    }
}
