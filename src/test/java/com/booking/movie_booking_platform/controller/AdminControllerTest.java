package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.TestFixtures;
import com.booking.movie_booking_platform.dto.*;
import com.booking.movie_booking_platform.entity.Movie;
import com.booking.movie_booking_platform.entity.Theatre;
import com.booking.movie_booking_platform.repository.MovieRepository;
import com.booking.movie_booking_platform.repository.TheatreRepository;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminControllerTest {

    @Autowired MovieAdminController movieAdminController;
    @Autowired TheatreAdminController theatreAdminController;
    @Autowired MovieRepository movieRepo;
    @Autowired TheatreRepository theatreRepo;

    @BeforeEach
    void setUp() {
        TestFixtures.movie(movieRepo, "Existing Movie", "English", "Drama");
        TestFixtures.theatre(theatreRepo, "Existing Theatre", "Delhi");
    }

    

    @Test
    void createMovie_returns201() {
        ResponseEntity<MovieResponse> response = movieAdminController.createMovie(
                new MovieCreateRequest("New Film", "Hindi", "Action", 140, "UA"));
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody().id());
        assertEquals("New Film", response.getBody().title());
    }

    @Test
    void listMovies_returns200() {
        ResponseEntity<List<MovieResponse>> response = movieAdminController.listMovies();
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isEmpty());
    }

    

    @Test
    void createTheatre_returns201() {
        ResponseEntity<TheatreResponse> response = theatreAdminController.createTheatre(
                new TheatreCreateRequest("New Cinema", "Mumbai", "Andheri", 6, "NC-MUM-001"));
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody().id());
        assertEquals("Mumbai", response.getBody().city());
    }

    @Test
    void listTheatres_returns200() {
        ResponseEntity<List<TheatreResponse>> response = theatreAdminController.listTheatres();
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isEmpty());
    }

    

    @Test
    void createShow_returns201WithSeats() {
        Movie movie = movieRepo.findAll().get(0);
        Theatre theatre = theatreRepo.findAll().get(0);

        ResponseEntity<ShowSearchResponse> response = theatreAdminController.createShow(
                new ShowCreateRequest(
                        movie.getId(), theatre.getId(),
                        LocalDate.now().plusDays(1),
                        LocalTime.of(20, 0), LocalTime.of(22, 30),
                        new BigDecimal("350"),
                        Map.of("REGULAR", 15, "PREMIUM", 8, "VIP", 2)));

        assertEquals(201, response.getStatusCode().value());
        ShowSearchResponse body = response.getBody();
        assertEquals(25, body.totalSeats());
        assertEquals(25, body.availableSeats());
        assertEquals(movie.getTitle(), body.movieTitle());
        assertEquals(theatre.getName(), body.theatreName());
    }
}

