package com.booking.movie_booking_platform.dto;

public record TheatreCreateRequest(
        String name,
        String city,
        String address,
        Integer totalScreens,
        String partnerCode
) {}

