package com.booking.movie_booking_platform.dto;

import java.math.BigDecimal;

public record TicketPriceBreakdown(
        String seatNumber,
        String seatType,
        BigDecimal basePrice,
        BigDecimal thirdTicketDiscount,
        BigDecimal afternoonDiscount,
        BigDecimal finalPrice
) {}

