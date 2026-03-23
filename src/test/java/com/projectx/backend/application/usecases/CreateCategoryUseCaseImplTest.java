package com.projectx.backend.application.usecases;

import com.projectx.backend.application.dto.CreateCategoryRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.Category;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCategoryUseCaseImplTest {

    @Mock private CategoryRepository categoryRepository;
    private CreateCategoryUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new CreateCategoryUseCaseImpl(categoryRepository);
    }

    @Test
    void debeCrearCategoriaExitosamente() {
        Category result = useCase.execute(TENANT, new CreateCategoryRequest("Alimentos", "desc", "img.jpg", 0));

        assertNotNull(result.categoryId());
        assertEquals("Alimentos", result.name());
        assertEquals(TENANT, result.tenantId());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void debeFallarSiNombreVacio() {
        assertThrows(BadRequestException.class, () ->
                useCase.execute(TENANT, new CreateCategoryRequest("", null, null, 0)));
    }

    @Test
    void debeFallarSiNombreNull() {
        assertThrows(BadRequestException.class, () ->
                useCase.execute(TENANT, new CreateCategoryRequest(null, null, null, 0)));
    }
}
