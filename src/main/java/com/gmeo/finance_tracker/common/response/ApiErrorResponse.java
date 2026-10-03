package com.gmeo.finance_tracker.common.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class ApiErrorResponse {

    public ApiErrorResponse(int status, String error, String message, String path,
            LocalDateTime timestamp, Map<String, String> validationErrors) {
        this.status = status; this.error = error; this.message = message; this.path = path;
        this.timestamp = timestamp; this.validationErrors = validationErrors;
    }

    private String code;
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;
}
