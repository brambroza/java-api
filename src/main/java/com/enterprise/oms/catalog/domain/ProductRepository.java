package com.enterprise.oms.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findAllById(Iterable<UUID> ids);

    Page<Product> findAll(Pageable pageable);
}
