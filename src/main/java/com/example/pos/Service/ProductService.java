package com.example.pos.Service;

import com.example.pos.DTO.ProductDto;
import com.example.pos.entity.ProductEntity;
import com.example.pos.repo.ProductRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;

    public Status productAdded(ProductDto productDto) {
        ProductEntity product = new ProductEntity();
        product.setCategory(productDto.getCategory());
        product.setQuantity(productDto.getQuantity());
        product.setPrice(productDto.getPrice());

        BigDecimal quantity = BigDecimal.valueOf(productDto.getQuantity());
        BigDecimal totalPrice = productDto.getPrice().multiply(quantity);
        product.setTotalPrice(totalPrice);
        product.setIsActive(true);
        return new Status(StatusMessage.SUCCESS, productRepo.save(product));

    }

    public Status productUpdated(String category, ProductDto productDto) {
        ProductEntity product = productRepo.findByCategory(category);

        if (product == null || Boolean.FALSE.equals(product.getIsActive())) {
            return new Status(StatusMessage.FAILURE, "Category does not exist against this name");
        }

        // Update only if the value is provided in the DTO
        if (Objects.nonNull(productDto.getCategory())) {
            product.setCategory(productDto.getCategory());
        }

        if (Objects.nonNull(productDto.getQuantity())) {
            product.setQuantity(productDto.getQuantity());
        }

        if (Objects.nonNull(productDto.getPrice())) {
            product.setPrice(productDto.getPrice());
        }

        // Recalculate total price if both price and quantity are now available
        if (Objects.nonNull(product.getPrice()) && Objects.nonNull(product.getQuantity())) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            BigDecimal totalPrice = product.getPrice().multiply(quantity);
            product.setTotalPrice(totalPrice);
        }

        return new Status(StatusMessage.SUCCESS, productRepo.save(product));
    }

    public Status getProducts(int id) {
        ProductEntity product = productRepo.findById(id);
        if (Objects.isNull(product.getIsActive()) || Boolean.FALSE.equals(product.getIsActive())) {
            return new Status(StatusMessage.FAILURE, "Record is not find against this id ");
        }
        return new Status(StatusMessage.SUCCESS, product);
    }

    public Status getAll() {
        List<ProductEntity> productEntities = productRepo.findAll();
        if (productEntities.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "List of user is empty");
        }
        List<ProductEntity> product = productEntities.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .collect(Collectors.toList());

        if (product.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "No active product found ");
        }
        return new Status(StatusMessage.SUCCESS, product);
    }

    public Status deleteRecord(int id) {
        ProductEntity product=productRepo.findById(id);
        if (Boolean.FALSE.equals(product.getIsActive())){
            return new Status(StatusMessage.FAILURE,"Record is already deleted");
        }
        product.setIsActive(false);
        productRepo.save(product);
        return new Status(StatusMessage.SUCCESS,"Product is deleted");
    }
}
