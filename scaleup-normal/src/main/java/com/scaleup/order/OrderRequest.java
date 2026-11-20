package com.scaleup.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(
        @NotNull Long customerId,
        @NotEmpty @Size(max = 5) @Valid List<OrderItemRequest> items) {
}