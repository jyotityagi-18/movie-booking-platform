package com.booking.movie_booking_platform.repository;

import com.booking.movie_booking_platform.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    
    @Query("SELECT s FROM Show s JOIN s.movie m JOIN s.theatre t " +
           "WHERE LOWER(t.city) = LOWER(:city) " +
           "AND s.showDate = :date " +
           "AND s.availableSeats > 0 " +
           "ORDER BY t.name, s.startTime")
    List<Show> findShowsByCityAndDate(@Param("city") String city,
                                      @Param("date") LocalDate date);

    
    @Query("SELECT s FROM Show s JOIN s.movie m JOIN s.theatre t " +
           "WHERE LOWER(t.city) = LOWER(:city) " +
           "AND LOWER(m.title) LIKE LOWER(CONCAT('%', :movieName, '%')) " +
           "AND s.showDate = :date " +
           "AND s.availableSeats > 0 " +
           "ORDER BY t.name, s.startTime")
    List<Show> findShowsByCityMovieNameAndDate(@Param("city") String city,
                                               @Param("movieName") String movieName,
                                               @Param("date") LocalDate date);
}

