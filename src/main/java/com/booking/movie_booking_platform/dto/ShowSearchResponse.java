package com.booking.movie_booking_platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ShowSearchResponse(
        Long showId,
        String movieTitle,
        String movieLanguage,
        String movieGenre,
        String movieRating,
        String theatreName,
        String theatreCity,
        String theatreAddress,
        LocalDate showDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal basePrice,
        int totalSeats,
        int availableSeats,
        boolean afternoonShow
) {}

