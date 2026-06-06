package com.gmeo.finance_tracker.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private UserResponse user;
    private String accessToken;
    private String tokenType;
}
