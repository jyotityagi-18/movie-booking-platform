package com.booking.movie_booking_platform.repository;

import com.booking.movie_booking_platform.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}

