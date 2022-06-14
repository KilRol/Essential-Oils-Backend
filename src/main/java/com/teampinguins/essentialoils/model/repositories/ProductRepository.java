package com.teampinguins.essentialoils.model.repositories;

import com.teampinguins.essentialoils.model.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByName(String name);

    Stream<ProductEntity> streamAllBy();
}