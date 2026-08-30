package com.portfolio.inventoryservice.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Inventory product not found: " + productId);
    }
}