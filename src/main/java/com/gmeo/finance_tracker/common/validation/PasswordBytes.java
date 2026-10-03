package com.gmeo.finance_tracker.common.validation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordBytesValidator.class)
public @interface PasswordBytes {
    String message() default "password must contain at most 72 UTF-8 bytes (BCrypt limit)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
