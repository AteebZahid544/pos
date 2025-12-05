package com.example.pos.repo.pos;

import com.example.pos.entity.pos.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesRepo extends JpaRepository<SalesEntity,Integer> {
    Optional<SalesEntity>findById(int id);
    SalesEntity deleteById(int id);
}
