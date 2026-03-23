package com.projectx.backend.application.dto;

/**
 * DTO para la creación de una categoría.
 */
public record CreateCategoryRequest(
        String name,
        String description,
        String imageUrl,
        int sortOrder
) {}
