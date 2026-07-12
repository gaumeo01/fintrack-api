package com.gmeo.finance_tracker.auth.dto;

import com.gmeo.finance_tracker.user.dto.UserResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String token;
    private UserResponse user;
}
