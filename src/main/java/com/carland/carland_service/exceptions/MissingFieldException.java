package com.carland.carland_service.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissingFieldException extends RuntimeException {

    public MissingFieldException(String message) {
        super(message);
    }

    public static MissingFieldException required(String fieldName) {
        return new MissingFieldException(fieldName + " is required");
    }
}

