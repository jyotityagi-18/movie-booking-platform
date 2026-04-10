package com.booking.movie_booking_platform.dto;

public record SeatResponse(
        Long seatId,
        String seatNumber,
        String seatRow,
        String seatType,
        String status,
        double priceMultiplier
) {}

