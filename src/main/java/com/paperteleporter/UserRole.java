package com.paperteleporter;

public enum UserRole {
    PEASANT(0.0),    // pays full price
    LORD(0.5),       // pays 50% (1.0 - 0.5)
    KING(1.0);       // pays 0% (full discount)

    private final double discountFactor;

    UserRole(double discountFactor) {
        this.discountFactor = discountFactor;
    }

    public double getDiscountFactor() {
        return discountFactor;
    }

    public double getEffectivePrice(int basePrice) {
        return basePrice * (1.0 - discountFactor);
    }
}
