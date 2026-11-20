package dev.rehm.lingua.common.exception;


import java.util.ArrayList;
import java.util.List;

public class ValidationError {

    private final List<FieldErrorDTO> fieldErrors = new ArrayList<>();

    public void addFieldError(String field ,String message,String code) {
        fieldErrors.add(new FieldErrorDTO(field, message, code));
    }

    public record FieldErrorDTO(
            String field,

            String message,
            String code
    ) {
    }

    public List<FieldErrorDTO> getFieldErrors() {
        return fieldErrors;
    }
}