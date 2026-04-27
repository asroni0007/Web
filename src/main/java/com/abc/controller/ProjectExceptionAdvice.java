package com.abc.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;

//异常处理
//@RestControllerAdvice
public class ProjectExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public Result doException(Exception ex){
        return new Result(null,666,"抛异常");
    }
}
