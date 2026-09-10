package com.enterprise.oms.shared.domain;

/** Base class for all business exceptions; {@code code} is a stable, machine-readable identifier. */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
