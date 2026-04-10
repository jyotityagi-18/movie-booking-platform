package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.TestFixtures;
import com.booking.movie_booking_platform.dto.SeatResponse;
import com.booking.movie_booking_platform.dto.ShowSearchResponse;
import com.booking.movie_booking_platform.entity.*;
import com.booking.movie_booking_platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShowControllerTest {

    @Autowired ShowController showController;
    @Autowired MovieRepository movieRepo;
    @Autowired TheatreRepository theatreRepo;
    @Autowired ShowRepository showRepo;
    @Autowired SeatRepository seatRepo;

    Show morningShow, afternoonShow;

    @BeforeEach
    void setUp() {
        Movie dune = TestFixtures.movie(movieRepo, "Dune", "English", "Sci-Fi");
        Theatre pvr = TestFixtures.theatre(theatreRepo, "PVR", "Bangalore");

        morningShow = TestFixtures.show(showRepo, dune, pvr,
                LocalDate.now(), LocalTime.of(10, 0), new BigDecimal("250"), 20);
        TestFixtures.seats(seatRepo, morningShow, 12, 6, 2);

        afternoonShow = TestFixtures.show(showRepo, dune, pvr,
                LocalDate.now(), LocalTime.of(14, 0), new BigDecimal("300"), 20);
        TestFixtures.seats(seatRepo, afternoonShow, 12, 6, 2);
    }

    @Test
    void searchShows_returns200WithResults() {
        ResponseEntity<List<ShowSearchResponse>> response =
                showController.searchShows("Bangalore", null, LocalDate.now());
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void searchShows_withMovieFilter_returns200() {
        ResponseEntity<List<ShowSearchResponse>> response =
                showController.searchShows("Bangalore", "Dune", LocalDate.now());
        assertEquals(200, response.getStatusCode().value());
        response.getBody().forEach(s -> assertEquals("Dune", s.movieTitle()));
    }

    @Test
    void searchShows_unknownCity_returnsEmptyList() {
        ResponseEntity<List<ShowSearchResponse>> response =
                showController.searchShows("Mars", null, LocalDate.now());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getSeats_returns200WithSeats() {
        ResponseEntity<List<SeatResponse>> response =
                showController.getSeats(morningShow.getId());
        assertEquals(200, response.getStatusCode().value());
        assertEquals(20, response.getBody().size());
    }
}

