package com.gmeo.finance_tracker.category;

import com.gmeo.finance_tracker.category.dto.CategoryRequest;
import com.gmeo.finance_tracker.category.dto.CategoryResponse;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.user.User;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public CategoryService(CategoryRepository categoryRepository, CurrentUserService currentUserService) {
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setUser(currentUserService.getCurrentUser());

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        User currentUser = currentUserService.getCurrentUser();
        return categoryRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = findOwnedCategory(id);

        return mapToResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findOwnedCategory(id);

        category.setName(request.getName());
        category.setType(request.getType());

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    public void deleteCategory(Long id) {
        Category category = findOwnedCategory(id);

        categoryRepository.delete(category);
    }

    private Category findOwnedCategory(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return categoryRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setType(category.getType());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
