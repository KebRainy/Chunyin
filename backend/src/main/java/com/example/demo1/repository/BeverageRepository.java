package com.example.demo1.repository;

import com.example.demo1.entity.Beverage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeverageRepository extends JpaRepository<Beverage, Long> {
    Page<Beverage> findByIsActiveTrue(Pageable pageable);
    Page<Beverage> findByTypeAndIsActiveTrue(String type, Pageable pageable);
    Page<Beverage> findByNameContainingAndIsActiveTrue(String name, Pageable pageable);
}
