package com.enterprise.oms.shared.domain;

/** A request that is well-formed but violates a business rule (HTTP 422). */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
