package com.example.pos.Service;

import com.example.pos.DTO.CategoryRequestDto;
import com.example.pos.DTO.ProductNameDto;

import com.example.pos.entity.pos.Category;
import com.example.pos.entity.pos.ProductName;
import com.example.pos.repo.pos.CategoryRepository;
import com.example.pos.repo.pos.ProductNameRepository;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductNameRepository productNameRepository;

    @Transactional("posTransactionManager")
    public Status saveCategoryWithProducts(CategoryRequestDto dto) {

        if (dto.getCategoryName() == null || dto.getCategoryName().isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Category name cannot be empty");
        }

        // --- 1. Check existing category (case-insensitive) ---
        // First, find any active category with same name (case-insensitive)
        List<Category> existingCategories = categoryRepository
                .findAllByIsActiveTrue()  // Changed to findAllByIsActiveTrue()
                .stream()
                .filter(c -> c.getCategoryName().equalsIgnoreCase(dto.getCategoryName()))
                .toList();

        Category category;
        if (existingCategories.isEmpty()) {
            // No existing category with this name (case-insensitive), create new one
            category = new Category();
            category.setCategoryName(dto.getCategoryName());
            category.setIsActive(true);
            category.setProducts(new ArrayList<>());
        } else {
            // Use the existing category
            category = existingCategories.get(0);
        }

        // --- 2. Check for duplicate products (case-insensitive) ---
        List<String> existingProductNames = category.getProducts().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))  // Safe null check
                .map(ProductName::getProductName)
                .map(String::toLowerCase)
                .toList();

        for (ProductNameDto pDto : dto.getProducts()) {
            String newProductNameLower = pDto.getProductName().toLowerCase();

            if (existingProductNames.contains(newProductNameLower)) {
                return new Status(StatusMessage.FAILURE,
                        "Product '" + pDto.getProductName() + "' already exists against this category");
            }

            ProductName product = new ProductName();
            product.setProductName(pDto.getProductName());
            product.setPurchasePrice(pDto.getPurchasePrice());
            product.setSellPrice(pDto.getSellPrice());
            product.setIsActive(true);
            product.setCategory(category);

            category.getProducts().add(product);
        }

        // --- 3. Save category and products in the correct tenant ---
        categoryRepository.save(category);

        return new Status(StatusMessage.SUCCESS, "Category with products saved successfully");
    }


    public Status updateCategory(int id,CategoryRequestDto dto){
        if (dto.getCategoryName() == null || dto.getCategoryName().isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Category name cannot be empty");
        }

        Optional<Category> existingCategoryOpt = categoryRepository.findById(id);

        if (existingCategoryOpt.isEmpty() || Boolean.FALSE.equals(existingCategoryOpt.get().getIsActive())) {
            return new Status(StatusMessage.FAILURE, "Category not found");
        }else {
            Category category = existingCategoryOpt.get();
            if (!dto.getCategoryName().equals(category.getCategoryName())) {
                category.setCategoryName(dto.getCategoryName());
                categoryRepository.save(category);
            }
        }
        return new Status(StatusMessage.SUCCESS, "Category updated successfully");
    }

    public Status updateProductNameAndPrice( int id ,ProductNameDto dtoProduct) {

        Optional<ProductName> existingProductOpt = productNameRepository.findProductByIdAndIsActiveTrue(id);
        if (existingProductOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product not found");
        }
        ProductName existingProduct = existingProductOpt.get();
        if (dtoProduct.getProductName()!=null) {
            existingProduct.setProductName(dtoProduct.getProductName());
        }
        if (dtoProduct.getPurchasePrice() != null) {
            existingProduct.setPurchasePrice(dtoProduct.getPurchasePrice());
        }
        if (dtoProduct.getSellPrice() != null) {
            existingProduct.setSellPrice(dtoProduct.getSellPrice());
        }
        productNameRepository.save(existingProduct);
        return new Status(StatusMessage.SUCCESS, "Product name or price updated successfully");
    }

    public Status getCategories(String categoryName) {
        if (categoryName != null && !categoryName.isEmpty()) {
            Optional<Category> optionalCategory = categoryRepository.findByCategoryNameAndIsActiveTrue(categoryName);
            if (optionalCategory.isPresent()) {
                Category category = optionalCategory.get();
                return new Status(StatusMessage.SUCCESS,mapToDto(category));
            } else {
                return new Status(StatusMessage.FAILURE, "Categories not found");
            }
        } else {
            List<Category> allCategories = categoryRepository.findAllByIsActiveTrue();
            List<CategoryRequestDto> dtoList = allCategories.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            return new Status(StatusMessage.SUCCESS, dtoList);
        }
    }

    public CategoryRequestDto mapToDto(Category category) {
        CategoryRequestDto dto = new CategoryRequestDto();
        dto.setId(category.getId());
        dto.setCategoryName(category.getCategoryName());

        // ✅ Filter out inactive products
        List<ProductNameDto> activeProducts = category.getProducts().stream()
                .filter(product -> Boolean.TRUE.equals(product.getIsActive())) // Only fetch active products
                .map(product -> {
                    ProductNameDto productDto = new ProductNameDto();
                    productDto.setId(product.getId());
                    productDto.setProductName(product.getProductName());
                    productDto.setPurchasePrice(product.getPurchasePrice());
                    productDto.setSellPrice(product.getSellPrice());

                    return productDto;
                })
                .collect(Collectors.toList());

        dto.setProducts(activeProducts);
        return dto;
    }

    public Status deleteCategoryById(int id) {

        Optional<Category> optionalCategory =
                categoryRepository.findById(id);

        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();

            // Soft delete category
            category.setIsActive(false);
            category.setDeletedBy(LocalDateTime.now());

            // Soft delete all products of that category
            if (category.getProducts() != null) {
                category.getProducts().forEach(product -> {
                    product.setIsActive(false);
                    product.setDeletedBy(LocalDateTime.now());
                });
            }

            categoryRepository.save(category);

            return new Status(StatusMessage.SUCCESS,
                    "Category and all product names soft-deleted successfully");
        } else {
            return new Status(StatusMessage.FAILURE, "Category not found");
        }
    }


    public Status deleteProductById(int id) {

        Optional<ProductName> optionalProduct = productNameRepository.findProductByIdAndIsActiveTrue(id);

        if (optionalProduct.isPresent()) {
            ProductName product = optionalProduct.get();
            product.setIsActive(false);
            product.setDeletedBy(LocalDateTime.now());
            productNameRepository.save(product);
            return new Status(StatusMessage.SUCCESS, "Product deleted successfully");
        } else {
            return new Status(StatusMessage.FAILURE, "Product not found");
        }
    }
}
