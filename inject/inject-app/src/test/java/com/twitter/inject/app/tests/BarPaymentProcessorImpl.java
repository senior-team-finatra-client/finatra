package com.twitter.inject.app.tests;

import java.math.BigDecimal;

public class BarPaymentProcessorImpl implements PaymentProcessor {

  @Override
  public String processPayment(BigDecimal payment) {
    return this.getClass().getSimpleName();
  }
}
