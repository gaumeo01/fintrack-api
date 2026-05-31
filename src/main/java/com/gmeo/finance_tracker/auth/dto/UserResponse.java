package com.gmeo.finance_tracker.auth.dto;

import com.gmeo.finance_tracker.user.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private UserRole role;
    private LocalDateTime createdAt;
}
