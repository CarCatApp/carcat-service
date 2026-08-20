package com.carland.carland_service.exceptions;

import com.carland.carland_service.dto.response.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * tr: REST API genelinde fırlatılan exception'ları yakalayıp ResponseException gövdesiyle uygun HTTP durum koduna (400/401/404/409) çeviren global exception handler.
 * en: Global exception handler that catches exceptions thrown across the REST API and maps them to the appropriate HTTP status (400/401/404/409) with a ResponseException body.
 */
@RestControllerAdvice
public class CustomExceptionHandler {


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseException> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Param is required")
                .message("Məlumatlar əksikdir!")
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ResponseException> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        ResponseException responseException = ResponseException.builder()
                .error("Missed required fields")
                .message(ex.getRequestPartName() + " file is required (form-data, type File)")
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotMatchException.class)
    public ResponseEntity<ResponseException> handleNotMatchException(NotMatchException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Not match error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ResponseException> handleFileStorageException(FileStorageException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Profile photo error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseException> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ResponseException responseException = ResponseException.builder()
                .error("Validation input error")
                .message(errorMessage)
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<ResponseException> handleMissingBodyException(HttpMessageConversionException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Body is required")
                .message("Məlumatlar əksikdir!")
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ResponseException> handleNotificationException(NotificationException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Notification send error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ResponseException> handleAlreadyExistsExceptionException(AlreadyExistsException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Already exists error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ResponseException> handleConflictException(ConflictException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Conflict error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InviteException.class)
    public ResponseEntity<ResponseException> handleAlreadyExistsExceptionException(InviteException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Invite code error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MsmTransactionException.class)
    public ResponseEntity<ResponseException> handleMsmTransactionException(MsmTransactionException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Otp message error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<ResponseException> handleUserStatusException(InvalidStatusException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Invalid user status")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(ResourceNotFoundException .class)
    public ResponseEntity<ResponseException> handleResourceNotFoundException(ResourceNotFoundException  ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Resource not found error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidOtpCodeException .class)
    public ResponseEntity<ResponseException> handleInvalidOtpCodeException(InvalidOtpCodeException  ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Invalid Otp error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExpiredOtpException.class)
    public ResponseEntity<ResponseException> handleExpiredOtpException(ExpiredOtpException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Expired Otp error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnderageException.class)
    public ResponseEntity<ResponseException> handleUnderageException(UnderageException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Under age error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(UsernameAlreadyExistException.class)
    public ResponseEntity<ResponseException> handleUsernameAlreadyExistException(UsernameAlreadyExistException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("User already exists")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MissingFieldException.class)
    public ResponseEntity<ResponseException> handleMissingFieldException(MissingFieldException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Missed required fields")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseException> handleUserNotFoundException(UserNotFoundException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("User not found")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<ResponseException> handleWrongPasswordException(WrongPasswordException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Wrong password")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenNotSetException.class)
    public ResponseEntity<ResponseException> handleRefreshTokenNotSetException(RefreshTokenNotSetException ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Refresh token not found")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ResponseException> handleRefreshTokenNotSetException(MissingRequestHeaderException  ex) {
        ResponseException responseException=ResponseException.builder()
                .error("Missing field error")
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return new ResponseEntity<>(responseException, HttpStatus.BAD_REQUEST);
    }


}
