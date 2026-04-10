package com.booking.movie_booking_platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

public record ShowCreateRequest(
        Long movieId,
        Long theatreId,
        LocalDate showDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal basePrice,
        Map<String, Integer> seatConfig // e.g. {"REGULAR":60, "PREMIUM":30, "VIP":10}
) {}

