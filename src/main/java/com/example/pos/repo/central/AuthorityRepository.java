package com.example.pos.repo.central;

import com.example.pos.entity.central.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Integer> {

    List<Authority> findByAuthorityIdIn(List<Integer> authorityIds);
    Optional<Authority> findByAuthorityId(Integer authorityIds);


}
