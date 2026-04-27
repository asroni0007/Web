package com.abc.controller;



//返回给前段数据的
public class Result {
    private Object data;
    private Integer code;
    private String message;

    public Result(Object data, Integer code) {
        this.data = data;
        this.code = code;
    }

    public Result(Object data, Integer code, String message) {
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public Result() {
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
