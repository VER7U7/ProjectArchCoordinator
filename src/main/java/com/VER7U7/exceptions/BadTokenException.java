package com.VER7U7.exceptions;

public class BadTokenException extends Exception{
    public BadTokenException() {
        super();
    }

    public BadTokenException(String message) {
        super(message);
    }

    public BadTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadTokenException(Throwable cause) {
        super(cause);
    }
}
