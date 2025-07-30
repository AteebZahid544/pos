package com.example.pos.repo;

import com.example.pos.entity.CompanyPaymentTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyPaymentTimeRepo extends JpaRepository<CompanyPaymentTime, Integer> {

}
