package com.booking.movie_booking_platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        Long showId,
        String movieTitle,
        String theatreName,
        String showDate,
        String showTime,
        String customerName,
        String customerEmail,
        List<String> seatNumbers,
        int numberOfTickets,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        List<TicketPriceBreakdown> priceBreakdown,
        List<String> appliedOffers,
        String status,
        LocalDateTime createdAt
) {}
