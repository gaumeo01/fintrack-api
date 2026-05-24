package com.gmeo.finance_tracker.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.category.dto.CategoryRequest;
import com.gmeo.finance_tracker.category.dto.CategoryResponse;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void createCategoryReturnsCreatedForValidRequest() throws Exception {
        CategoryResponse response = new CategoryResponse();
        response.setId(1L);
        response.setName("Salary");
        response.setType(CategoryType.INCOME);

        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Salary",
                                  "type": "INCOME"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void createCategoryReturnsBadRequestForBlankName() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "type": "EXPENSE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());

        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategoryReturnsBadRequestForMissingType() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Food"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.type").exists());

        verifyNoInteractions(categoryService);
    }
}
