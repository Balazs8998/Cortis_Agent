package dev.rehm.lingua.common.exception;


import dev.rehm.lingua.common.exception.classes.ArgumentValidationException;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    @ExceptionHandler(ArgumentValidationException.class)
    public void fldksjf(){

    }

}
