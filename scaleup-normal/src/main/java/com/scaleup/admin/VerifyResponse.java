package com.scaleup.admin;

import java.util.List;
import java.util.Map;

public record VerifyResponse(
        boolean allChecksPassed,
        String seedChecksum,
        long inFlightOrders,
        Map<String, Long> rowCounts,
        Map<String, Long> ordersByStatus,
        List<Check> checks) {

    public record Check(String name, boolean passed, long violations) {
    }
}