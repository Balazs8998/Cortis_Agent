package dev.rehm.lingua.common.exception;


import dev.rehm.lingua.common.exception.classes.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {


    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<FieldError> fieldErrors =
                exception.getBindingResult().getFieldErrors();

        log.debug(
                "Request validation failed | Fields: {}",
                fieldErrors.stream()
                        .map(FieldError::getField)
                        .toList()
        );



        ValidationError validationError =
                processFieldErrors(fieldErrors);

        return ResponseEntity
                .status(400)
                .body(validationError);
    }

    @ExceptionHandler(AgentEmptyFinalResponseException.class)
    public ResponseEntity<ApiError> handleAgentEmptyFinalResponse(AgentEmptyFinalResponseException e){

        log.warn(
                "ErrorId: {} | Code: {} | Message: {}",
                e.getErrorId(),
                e.getCode(),
                e.getMessage()
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(502)
                .body(apiError);
    }

    @ExceptionHandler(AgentInvalidMessageRoleException.class)
    public ResponseEntity<ApiError> handleAgentInvalidMessageRole(AgentInvalidMessageRoleException e){

        log.warn(
                "ErrorId: {} | Code: {} | Message: {}",
                e.getErrorId(),
                e.getCode(),
                e.getMessage()
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(502)
                .body(apiError);
    }

    @ExceptionHandler(AgentProcessFailedException.class)

    public ResponseEntity<ApiError> handleAgentProcessFailed(AgentProcessFailedException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(502)
                .body(apiError);
    }

    @ExceptionHandler(AgentUnexpectedFinalToolCallsException.class)
    public ResponseEntity<ApiError> handleAgentUnexpectedFinalToolCalls(AgentUnexpectedFinalToolCallsException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(502)
                .body(apiError);
    }

    @ExceptionHandler(ArgumentValidationException.class)
    public ResponseEntity<ApiError> handleArgumentValidation(ArgumentValidationException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(500)
                .body(apiError);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiError> handleConversationNotFoundException(ConversationNotFoundException e){

        log.warn(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(404)
                .body(apiError);
    }

    @ExceptionHandler(IterationValueException.class)
    public ResponseEntity<ApiError> handleIterationValue(IterationValueException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(500)
                .body(apiError);
    }

    @ExceptionHandler(OllamaTimeoutException.class)
    public ResponseEntity<ApiError> handleOllamaTimeoutException(OllamaTimeoutException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(504)
                .body(apiError);
    }

    @ExceptionHandler(SearxngTimeOutException.class)
    public ResponseEntity<ApiError> handleSearxngTimeOut(SearxngTimeOutException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(504)
                .body(apiError);
    }

    @ExceptionHandler(ToolBudgetException.class)
       public ResponseEntity<ApiError> handleToolBudget(ToolBudgetException e){

        log.error(
                "ErrorId: {} | Code: {}",
                e.getErrorId(),
                e.getCode(),
                e
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(500)
                .body(apiError);
    }

    @ExceptionHandler(UnexpectedNullValueException.class)
    public ResponseEntity<ApiError> handleUnexpectedNullValueException(UnexpectedNullValueException e){

        log.warn(
                "ErrorId: {} | Code: {} | Message: {}",
                e.getErrorId(),
                e.getCode(),
                e.getMessage()
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(500
                )
                .body(apiError);
    }

    @ExceptionHandler(UnsupportedLlmModelException.class)
    public ResponseEntity<ApiError> handleUnsupportedLlmModelException(UnsupportedLlmModelException e){

        log.warn(
                "ErrorId: {} | Code: {} | Message: {}",
                e.getErrorId(),
                e.getCode(),
                e.getMessage()
        );

        ApiError apiError = processApiError(e);

        return ResponseEntity
                .status(400)
                .body(apiError);
    }


    private ValidationError processFieldErrors(
            List<FieldError> fieldErrors
    ) {
        ValidationError validationError = new ValidationError();

        for (FieldError fieldError : fieldErrors) {

            validationError.addFieldError(
                    fieldError.getDefaultMessage(),
                    fieldError.getField(),
                    fieldError.getCode()
            );
        }

        return validationError;
    }

    private ApiError processApiError(BaseException e) {

        return new ApiError(e.getCode(),e.getErrorId(),e.getTimestamp());
    }



}
