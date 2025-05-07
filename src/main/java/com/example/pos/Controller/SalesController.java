package com.example.pos.Controller;

import com.example.pos.DTO.SalesDto;
import com.example.pos.Service.SalesService;
import com.example.pos.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class SalesController {
    @Autowired
    private SalesService salesService;

    @PostMapping("/sell")
    public Status productSell(@RequestBody SalesDto salesDto){
        return salesService.productSell(salesDto);
    }
    @DeleteMapping("/deleteRecord/{id}")
    public Status deleteSaleRecord(@PathVariable int id){
        return salesService.cancelProductSale(id);
    }
}
