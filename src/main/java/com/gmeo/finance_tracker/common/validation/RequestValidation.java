package com.gmeo.finance_tracker.common.validation;

import com.gmeo.finance_tracker.common.exception.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Shared validation for HTTP, CSV and direct application-service callers. */
public final class RequestValidation {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private RequestValidation() {}
    public static void validate(Object request) {
        var errors = VALIDATOR.validate(request);
        if (!errors.isEmpty()) throw new BadRequestException(errors.stream()
                .map(e -> e.getPropertyPath() + ": " + e.getMessage()).sorted()
                .collect(java.util.stream.Collectors.joining("; ")));
    }
}
