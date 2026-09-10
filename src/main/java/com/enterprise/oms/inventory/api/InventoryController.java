package com.enterprise.oms.inventory.api;

import com.enterprise.oms.inventory.application.InventoryDtos.InventoryResponse;
import com.enterprise.oms.inventory.application.InventoryDtos.RestockRequest;
import com.enterprise.oms.inventory.application.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Stock position of a product")
    public InventoryResponse get(@PathVariable UUID productId) {
        return service.get(productId);
    }

    @PostMapping("/{productId}/restock")
    @Operation(summary = "Add stock for a product")
    public InventoryResponse restock(@PathVariable UUID productId, @Valid @RequestBody RestockRequest request) {
        return service.restock(productId, request.quantity());
    }
}
