package com.booking.movie_booking_platform.repository;

import com.booking.movie_booking_platform.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
}

