package com.VER7U7.exceptions;

public class BadCredentialException extends Exception{

    public BadCredentialType badCredentialType = BadCredentialType.BadDefault;

    public BadCredentialException() {
        super();
    }

    public BadCredentialException(BadCredentialType type) {
        super(type.name());
    }

    public BadCredentialException(BadCredentialType type, Throwable cause) {
        super(type.name(), cause);
    }

    public BadCredentialException(Throwable cause) {
        super(cause);
    }

    public BadCredentialType getType() {
        return badCredentialType;
    }
}
