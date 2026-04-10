package com.booking.movie_booking_platform.dto;

public record TheatreResponse(
        Long id,
        String name,
        String city,
        String address,
        Integer totalScreens,
        String partnerCode
) {}

