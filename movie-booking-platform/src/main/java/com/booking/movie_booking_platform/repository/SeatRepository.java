package com.booking.movie_booking_platform.repository;

import com.booking.movie_booking_platform.entity.Seat;
import com.booking.movie_booking_platform.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByShowIdAndStatus(Long showId, SeatStatus status);
    List<Seat> findByShowId(Long showId);
    List<Seat> findByIdInAndShowIdAndStatus(List<Long> ids, Long showId, SeatStatus status);
}

