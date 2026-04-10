package com.booking.movie_booking_platform.repository;

import com.booking.movie_booking_platform.entity.Booking;
import com.booking.movie_booking_platform.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}

