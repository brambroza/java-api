package com.enterprise.oms.catalog.application;

import com.enterprise.oms.catalog.application.ProductDtos.ChangePriceRequest;
import com.enterprise.oms.catalog.application.ProductDtos.CreateProductRequest;
import com.enterprise.oms.catalog.application.ProductDtos.ProductResponse;
import com.enterprise.oms.catalog.application.ProductDtos.ProductSnapshot;
import com.enterprise.oms.catalog.domain.Product;
import com.enterprise.oms.catalog.domain.ProductRepository;
import com.enterprise.oms.inventory.application.InventoryService;
import com.enterprise.oms.shared.domain.ConflictException;
import com.enterprise.oms.shared.domain.Money;
import com.enterprise.oms.shared.domain.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final InventoryService inventoryService;

    public ProductService(ProductRepository repository, InventoryService inventoryService) {
        this.repository = repository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase();
        if (repository.existsBySku(sku)) {
            throw new ConflictException("SKU_EXISTS", "Product with SKU %s already exists".formatted(sku));
        }
        Product product = repository.save(Product.create(sku, request.name(), request.description(),
                Money.of(request.price(), request.currency())));
        inventoryService.initialize(product.getId(), request.initialStock());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse changePrice(UUID id, ChangePriceRequest request) {
        Product product = load(id);
        product.changePrice(Money.of(request.price(), request.currency()));
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse deactivate(UUID id) {
        Product product = load(id);
        product.deactivate();
        return ProductResponse.from(repository.save(product));
    }

    public ProductResponse get(UUID id) {
        return ProductResponse.from(load(id));
    }

    public Page<ProductResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(ProductResponse::from);
    }

    /** Snapshots of the requested products; throws NOT_FOUND if any id is unknown. */
    public List<ProductSnapshot> findSnapshots(Collection<UUID> ids) {
        List<Product> products = repository.findAllById(ids);
        if (products.size() != ids.size()) {
            UUID missing = ids.stream()
                    .filter(id -> products.stream().noneMatch(p -> p.getId().equals(id)))
                    .findFirst().orElseThrow();
            throw new NotFoundException("Product", missing);
        }
        return products.stream().map(ProductSnapshot::from).toList();
    }

    private Product load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }
}
