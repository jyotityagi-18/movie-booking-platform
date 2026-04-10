package com.booking.movie_booking_platform.dto;

public record MovieCreateRequest(
        String title,
        String language,
        String genre,
        Integer durationMinutes,
        String rating
) {}

