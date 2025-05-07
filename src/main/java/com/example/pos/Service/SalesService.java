package com.example.pos.Service;

import com.example.pos.DTO.SalesDto;
import com.example.pos.entity.ProductEntity;
import com.example.pos.entity.SalesEntity;
import com.example.pos.repo.ProductRepo;
import com.example.pos.repo.SalesRepo;
import com.example.pos.util.Status;
import com.example.pos.util.StatusMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Service
public class SalesService {
    @Autowired
    private SalesRepo salesRepo;

    @Autowired
    private ProductRepo productRepo;

    public Status productSell(SalesDto salesDto) {
        boolean saleProduct = productRepo.existsByCategory(salesDto.getProduct());
        if (Boolean.FALSE.equals(saleProduct)) {
            return new Status(StatusMessage.FAILURE, "This product is out of stock");
        }

        ProductEntity quantity = productRepo.findByCategory(salesDto.getProduct());
        if (Objects.isNull(quantity.getQuantity()) || Objects.isNull(salesDto.getQuantity()) ||
                quantity.getQuantity() < salesDto.getQuantity()) {
            return new Status(StatusMessage.FAILURE, "Your quantity of product is not available in stock");
        }

        SalesEntity salesEntity = new SalesEntity();
        salesEntity.setProduct(salesDto.getProduct());
        Integer setQuantity = salesDto.getQuantity();
        salesEntity.setQuantity(setQuantity);
        salesEntity.setPrice(salesDto.getPrice());
        salesEntity.setDiscount(salesDto.getDiscount());

        BigDecimal discount = salesDto.getDiscount();
        BigDecimal totalQuantity = BigDecimal.valueOf(salesDto.getQuantity());
        BigDecimal total = (salesDto.getPrice().multiply(totalQuantity)).subtract(discount);
        salesEntity.setTotalAmount(total);

        ProductEntity product = productRepo.findByCategory(salesDto.getProduct());
        Integer currentQuantity = product.getQuantity();

        if (currentQuantity == null) {
            return new Status(StatusMessage.FAILURE, "Product quantity is not available.");
        }

        Integer newQuantity = currentQuantity - setQuantity;
        if (newQuantity < 0) {
            return new Status(StatusMessage.FAILURE, "Not enough stock to fulfill the sale.");
        }

        product.setQuantity(newQuantity);
        productRepo.save(product);

        return new Status(StatusMessage.SUCCESS, salesRepo.save(salesEntity));
    }

    @Transactional
    public Status cancelProductSale(int id) {
        Optional<SalesEntity> optionalSale = salesRepo.findById(id);
        if (optionalSale.isEmpty()) {
            return new Status(StatusMessage.FAILURE, "Sale record not found");
        }

        SalesEntity sale = optionalSale.get();
        String productCategory = sale.getProduct();

        ProductEntity product = productRepo.findByCategory(productCategory);
        if (product == null) {
            return new Status(StatusMessage.FAILURE, "Related product not found");
        }
        Integer restoredQuantity = product.getQuantity() + sale.getQuantity();
        product.setQuantity(restoredQuantity);
        productRepo.save(product);

        salesRepo.deleteById(id);

        return new Status(StatusMessage.SUCCESS, "Sale cancelled and stock restored successfully");
    }


}
