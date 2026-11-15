package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;


public class ArgumentValidationException extends BaseException {


    public ArgumentValidationException(String functionName, String fieldName, Object value, String message, String code) {
        super(functionName, fieldName, value, message, code);
    }




}
