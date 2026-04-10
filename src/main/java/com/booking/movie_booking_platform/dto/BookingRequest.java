package com.booking.movie_booking_platform.dto;

import java.util.List;

public record BookingRequest(
        Long showId,
        String customerName,
        String customerEmail,
        List<Long> seatIds
) {}

