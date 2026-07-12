package com.VER7U7.exceptions;

import java.io.IOException;

public class OldTokenException extends Exception {
    public OldTokenException() {
        super();
    }

    public OldTokenException(String message) {
        super(message);
    }

    public OldTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public OldTokenException(Throwable cause) {
        super(cause);
    }
}
