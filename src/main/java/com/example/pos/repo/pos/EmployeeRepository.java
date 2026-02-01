package com.example.pos.repo.pos;

import com.example.pos.entity.pos.Employee;
import org.apache.catalina.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Employee findByNameAndAddressAndActive(String name, String address, boolean active);

    List<Employee>findByActiveTrueOrderByCreatedAtDesc();

    Employee findByNameAndDesignationAndActiveTrue(String name, String designation);
    List<Employee> findByActiveTrue();

}
