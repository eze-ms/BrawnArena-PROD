package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
