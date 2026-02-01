package com.example.pos.repo.pos;

import com.example.pos.entity.pos.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory,String > {

    List<ExpenseCategory> findByTypeAndIsActiveTrue(String type);
    Optional<ExpenseCategory> findByNameIgnoreCaseAndIsActiveTrue(String name);
    Optional<ExpenseCategory> findById(Long id);
    Optional<ExpenseCategory> findByIdAndIsActive(Long id,Boolean isActive);

}
