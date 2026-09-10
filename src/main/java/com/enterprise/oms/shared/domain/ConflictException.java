package com.enterprise.oms.shared.domain;

/** The request conflicts with the current state of the resource (HTTP 409). */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
