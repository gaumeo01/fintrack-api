package com.gmeo.finance_tracker.auth.dto;

import lombok.Getter;
import lombok.Setter;

// TODO: Add validation annotations when implementing auth.
@Getter
@Setter
public class RegisterRequest {

    private String fullName;
    private String email;
    private String password;
}
