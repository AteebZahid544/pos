package com.example.pos.repo.pos;

import com.example.pos.entity.pos.CompanyPaymentTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyPaymentTimeRepo extends JpaRepository<CompanyPaymentTime, Integer> {

}
