package com.booking.movie_booking_platform.entity;

public enum SeatType {
    REGULAR(1.0),
    PREMIUM(1.5),
    VIP(2.0);

    private final double priceMultiplier;

    SeatType(double priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }
}

