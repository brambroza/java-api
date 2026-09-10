package com.enterprise.oms.catalog.api;

import com.enterprise.oms.catalog.application.ProductDtos.ChangePriceRequest;
import com.enterprise.oms.catalog.application.ProductDtos.CreateProductRequest;
import com.enterprise.oms.catalog.application.ProductDtos.ProductResponse;
import com.enterprise.oms.catalog.application.ProductService;
import com.enterprise.oms.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Catalog")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a product (and its inventory record)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product")
    public ProductResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List products")
    public PageResponse<ProductResponse> list(@PageableDefault(sort = "sku", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(service.list(pageable), r -> r);
    }

    @PutMapping("/{id}/price")
    @Operation(summary = "Change the price of a product")
    public ProductResponse changePrice(@PathVariable UUID id, @Valid @RequestBody ChangePriceRequest request) {
        return service.changePrice(id, request);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a product so it can no longer be ordered")
    public ProductResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}
