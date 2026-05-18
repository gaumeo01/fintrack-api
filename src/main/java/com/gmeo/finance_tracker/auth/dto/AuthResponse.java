package com.gmeo.finance_tracker.auth.dto;

import com.gmeo.finance_tracker.user.dto.UserResponse;

import lombok.Getter;
import lombok.Setter;

// TODO: Use this after JWT authentication is implemented.
@Getter
@Setter
public class AuthResponse {

    private String token;
    private UserResponse user;
}
