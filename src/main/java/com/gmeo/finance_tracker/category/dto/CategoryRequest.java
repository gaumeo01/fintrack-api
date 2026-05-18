package com.gmeo.finance_tracker.category.dto;

import lombok.Getter;
import lombok.Setter;

// TODO: Add validation annotations when implementing category API.
@Getter
@Setter
public class CategoryRequest {

    private String name;
    private String type;
}
