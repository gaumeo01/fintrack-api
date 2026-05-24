package com.gmeo.finance_tracker.category.dto;

import com.gmeo.finance_tracker.category.enums.CategoryType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
