package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleUserNotFoundExcepction(UserNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(NicknameAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleNicknameAlreadyExistsException(NicknameAlreadyExistsException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.CONFLICT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CharacterNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleCharacterNotFoundException(CharacterNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientTokensException.class)
    public ResponseEntity<ErrorMessage> handleInsufficientTokensException(InsufficientTokensException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CharacterAccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleCharacterAccessDeniedException(CharacterAccessDeniedException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.FORBIDDEN, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(NoPendingBuildException.class)
    public ResponseEntity<ErrorMessage> handleNoPendingBuildException(NoPendingBuildException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BuildAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleBuildAlreadyExistsException(BuildAlreadyExistsException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.CONFLICT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BuildNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleBuildNotFoundException(BuildNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HighlightedModelNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleHighlightedModelNotFoundException(HighlightedModelNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleModelNotFoundException(ModelNotFoundException ex) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}