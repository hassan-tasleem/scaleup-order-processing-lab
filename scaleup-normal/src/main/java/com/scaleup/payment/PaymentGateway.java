package com.scaleup.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentOutcome charge(Long orderId, BigDecimal amount);
}