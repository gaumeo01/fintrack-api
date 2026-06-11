package com.gmeo.finance_tracker.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.category.dto.CategoryRequest;
import com.gmeo.finance_tracker.category.dto.CategoryResponse;
import com.gmeo.finance_tracker.category.enums.CategoryType;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CategoryServiceTests {

    private CategoryRepository categoryRepository;
    private CurrentUserService currentUserService;
    private CategoryService categoryService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        categoryRepository = Mockito.mock(CategoryRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        categoryService = new CategoryService(categoryRepository, currentUserService);

        currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createCategoryAssignsCurrentUser() {
        Category savedCategory = category(1L, "Food");
        savedCategory.setUser(currentUser);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(request("Food"));

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getUser()).isEqualTo(currentUser);
        assertThat(response.getName()).isEqualTo("Food");
    }

    @Test
    void getAllCategoriesListsOnlyCurrentUsersCategories() {
        when(categoryRepository.findAllByUserId(7L)).thenReturn(List.of(category(1L, "Food")));

        List<CategoryResponse> response = categoryService.getAllCategories();

        verify(categoryRepository).findAllByUserId(7L);
        assertThat(response).extracting(CategoryResponse::getName).containsExactly("Food");
    }

    @Test
    void getCategoryByIdCannotAccessAnotherUsersCategory() {
        when(categoryRepository.findByIdAndUserId(9L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id: 9");
    }

    private CategoryRequest request(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setType(CategoryType.EXPENSE);
        return request;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(CategoryType.EXPENSE);
        return category;
    }
}
