package com.example.pos.Service;

import com.example.pos.DTO.CategoryRequestDto;
import com.example.pos.DTO.ProductDto;
import com.example.pos.DTO.ProductNameDto;
import com.example.pos.entity.Category;
import com.example.pos.entity.ProductName;
import com.example.pos.repo.CategoryRepository;
import com.example.pos.repo.ProductNameRepository;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductNameRepository productNameRepository;

    public Status saveCategoryWithProducts(CategoryRequestDto dto) {
        if (dto.getCategoryName() == null || dto.getCategoryName().isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Category name cannot be empty");
        }

        Category category;
        Optional<Category> existingCategory = categoryRepository.findByCategoryNameAndIsActiveTrue(dto.getCategoryName());

        if (existingCategory.isPresent()) {
            category = existingCategory.get();
        } else {
            category = new Category(); // create new category
            category.setCategoryName(dto.getCategoryName());
            category.setIsActive(true);
        }

        List<ProductName> updatedProducts = new ArrayList<>();

        for (ProductNameDto p : dto.getProducts()) {
            Optional<ProductName> dbProduct = productNameRepository
                    .findByCategoryAndProductNameIgnoreCaseAndIsActiveTrue(category, p.getProductName());

            if (dbProduct.isPresent()) {
                return new Status(StatusMessage.FAILURE,
                        "Product '" + p.getProductName() + "' already exists against this category");
            }


            ProductName product = new ProductName();
            product.setProductName(p.getProductName());
            product.setProductPrice(p.getProductPrice());
            product.setIsActive(true);
            product.setCategory(category);

            updatedProducts.add(product);
        }

        if (category.getProducts() == null) {
            category.setProducts(new ArrayList<>());
        }
        category.getProducts().addAll(updatedProducts);
        categoryRepository.save(category);

        return new Status(StatusMessage.SUCCESS, "Category with products saved/updated successfully");
    }




    public Status updateCategoryWithProducts(CategoryRequestDto dto){
        if (dto.getCategoryName() == null || dto.getCategoryName().isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Category name cannot be empty");
        }

        Optional<Category> existingCategoryOpt = categoryRepository.findById(dto.getId());

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

    public Status updateProductNameAndPrice( ProductNameDto dtoProduct) {
        if (dtoProduct.getId() == null) {
            return new Status(StatusMessage.FAILURE, "Product ID cannot be null");
        }
        Optional<ProductName> existingProductOpt = productNameRepository.findProductByIdAndIsActiveTrue(dtoProduct.getId());
        if (existingProductOpt.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Product not found");
        }
        ProductName existingProduct = existingProductOpt.get();
        if (dtoProduct.getProductName()!=null) {
            existingProduct.setProductName(dtoProduct.getProductName());
        }
        if (dtoProduct.getProductPrice() != null) {
            existingProduct.setProductPrice(dtoProduct.getProductPrice());
        }
        productNameRepository.save(existingProduct);
        return new Status(StatusMessage.SUCCESS, "Product updated successfully");
    }

    public Status getCategories(String categoryName) {
        if (categoryName != null && !categoryName.isEmpty()) {
            Optional<Category> optionalCategory = categoryRepository.findByCategoryNameAndIsActiveTrue(categoryName);
            if (optionalCategory.isPresent()) {
                Category category = optionalCategory.get();
                return new Status(StatusMessage.SUCCESS,mapToDto(category));
            } else {
                return new Status(StatusMessage.FAILURE, "Category not found");
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
                    productDto.setProductPrice(product.getProductPrice());
                    return productDto;
                })
                .collect(Collectors.toList());

        dto.setProducts(activeProducts);
        return dto;
    }

    public Status deleteCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Category name cannot be empty");
        }
        Optional<Category> optionalCategory = categoryRepository.findByCategoryNameAndIsActiveTrue(categoryName);

        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();
            category.setIsActive(false);
            categoryRepository.save(category);
            return new Status(StatusMessage.SUCCESS, "Category deleted successfully");
        } else {
            return new Status(StatusMessage.FAILURE, "Category not found");
        }
    }

    public Status deleteProductByName(ProductNameDto productNameDto) {
        if (productNameDto.getId() == null) {
            return new Status(StatusMessage.FAILURE, "Product Id cannot be null");
        }
        Optional<ProductName> optionalProduct = productNameRepository.findProductByIdAndIsActiveTrue(productNameDto.getId());

        if (optionalProduct.isPresent()) {
            ProductName product = optionalProduct.get();
            product.setIsActive(false);
            productNameRepository.save(product);
            return new Status(StatusMessage.SUCCESS, "Product deleted successfully");
        } else {
            return new Status(StatusMessage.FAILURE, "Product not found");
        }
    }
}
