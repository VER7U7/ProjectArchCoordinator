package com.VER7U7.exceptions;

public class AccountNotExists extends Exception{
    public AccountNotExists() {
        super();
    }

    public AccountNotExists(String message) {
        super(message);
    }

    public AccountNotExists(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountNotExists(Throwable cause) {
        super(cause);
    }
}
