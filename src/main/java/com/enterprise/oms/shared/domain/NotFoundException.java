package com.enterprise.oms.shared.domain;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource, Object identifier) {
        super("NOT_FOUND", "%s %s was not found".formatted(resource, identifier));
    }
}
