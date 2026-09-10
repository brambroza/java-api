package com.enterprise.oms.catalog.infrastructure;

import com.enterprise.oms.catalog.domain.Product;
import com.enterprise.oms.catalog.domain.ProductRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, UUID>, ProductRepository {
}
