package com.booking.movie_booking_platform.service;

import com.booking.movie_booking_platform.TestFixtures;
import com.booking.movie_booking_platform.dto.*;
import com.booking.movie_booking_platform.entity.*;
import com.booking.movie_booking_platform.exception.ResourceNotFoundException;
import com.booking.movie_booking_platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShowServiceTest {

    @Autowired ShowService showService;
    @Autowired MovieRepository movieRepo;
    @Autowired TheatreRepository theatreRepo;
    @Autowired ShowRepository showRepo;
    @Autowired SeatRepository seatRepo;

    Movie dune, pathaan;
    Theatre pvrBlr, inoxMum;
    Show morningShow, afternoonShow, mumShow;

    @BeforeEach
    void setUp() {
        dune = TestFixtures.movie(movieRepo, "Dune: Part Three", "English", "Sci-Fi");
        pathaan = TestFixtures.movie(movieRepo, "Pathaan 2", "Hindi", "Action");

        pvrBlr = TestFixtures.theatre(theatreRepo, "PVR Orion", "Bangalore");
        inoxMum = TestFixtures.theatre(theatreRepo, "INOX Phoenix", "Mumbai");

        morningShow = TestFixtures.show(showRepo, dune, pvrBlr,
                LocalDate.now(), LocalTime.of(10, 0), new BigDecimal("250"), 30);
        TestFixtures.seats(seatRepo, morningShow, 18, 9, 3);

        afternoonShow = TestFixtures.show(showRepo, dune, pvrBlr,
                LocalDate.now(), LocalTime.of(14, 0), new BigDecimal("300"), 20);
        TestFixtures.seats(seatRepo, afternoonShow, 12, 6, 2);

        mumShow = TestFixtures.show(showRepo, pathaan, inoxMum,
                LocalDate.now(), LocalTime.of(15, 0), new BigDecimal("350"), 20);
        TestFixtures.seats(seatRepo, mumShow, 12, 6, 2);
    }

    

    @Test
    void searchShows_byCity_returnsMatches() {
        var shows = showService.searchShows("Bangalore", null, LocalDate.now());
        assertFalse(shows.isEmpty());
        shows.forEach(s -> assertEquals("Bangalore", s.theatreCity()));
    }

    @Test
    void searchShows_byCityAndMovie_filtersCorrectly() {
        var shows = showService.searchShows("Bangalore", "Dune", LocalDate.now());
        assertFalse(shows.isEmpty());
        shows.forEach(s -> assertEquals("Dune: Part Three", s.movieTitle()));
    }

    @Test
    void searchShows_unknownCity_returnsEmpty() {
        assertTrue(showService.searchShows("Atlantis", null, LocalDate.now()).isEmpty());
    }

    @Test
    void searchShows_futureDate_returnsEmpty() {
        assertTrue(showService.searchShows("Bangalore", null,
                LocalDate.now().plusDays(30)).isEmpty());
    }

    @Test
    void searchShows_includesAfternoonFlag() {
        var shows = showService.searchShows("Bangalore", null, LocalDate.now());
        long count = shows.stream().filter(ShowSearchResponse::afternoonShow).count();
        assertTrue(count > 0);
    }

    @Test
    void searchShows_differentCitiesDontMix() {
        var blr = showService.searchShows("Bangalore", null, LocalDate.now());
        var mum = showService.searchShows("Mumbai", null, LocalDate.now());
        blr.forEach(s -> assertEquals("Bangalore", s.theatreCity()));
        mum.forEach(s -> assertEquals("Mumbai", s.theatreCity()));
    }

    

    @Test
    void getSeats_returnsCorrectCount() {
        var seats = showService.getSeatsForShow(morningShow.getId());
        assertEquals(30, seats.size());
    }

    @Test
    void getSeats_containsAllTypes() {
        var seats = showService.getSeatsForShow(morningShow.getId());
        long vip = seats.stream().filter(s -> s.seatType().equals("VIP")).count();
        long premium = seats.stream().filter(s -> s.seatType().equals("PREMIUM")).count();
        long regular = seats.stream().filter(s -> s.seatType().equals("REGULAR")).count();
        assertEquals(3, vip);
        assertEquals(9, premium);
        assertEquals(18, regular);
    }

    @Test
    void getSeats_invalidShow_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> showService.getSeatsForShow(999999L));
    }

    

    @Test
    void createMovie_returnsWithId() {
        MovieResponse r = showService.createMovie(
                new MovieCreateRequest("Test Movie", "English", "Drama", 120, "R"));
        assertNotNull(r.id());
        assertEquals("Test Movie", r.title());
    }

    @Test
    void listMovies_returnsSeededMovies() {
        assertTrue(showService.listMovies().size() >= 2);
    }

    

    @Test
    void createTheatre_returnsWithId() {
        TheatreResponse r = showService.createTheatre(
                new TheatreCreateRequest("New Cinema", "Delhi", "CP", 4, "NEW-DEL-001"));
        assertNotNull(r.id());
        assertEquals("Delhi", r.city());
    }

    @Test
    void listTheatres_returnsSeededTheatres() {
        assertTrue(showService.listTheatres().size() >= 2);
    }

    

    @Test
    void createShow_createsShowWithSeats() {
        ShowSearchResponse r = showService.createShow(new ShowCreateRequest(
                dune.getId(), pvrBlr.getId(),
                LocalDate.now().plusDays(1), LocalTime.of(20, 0), LocalTime.of(22, 30),
                new BigDecimal("400"),
                Map.of("REGULAR", 10, "PREMIUM", 5, "VIP", 2)));

        assertNotNull(r.showId());
        assertEquals(17, r.totalSeats());
        assertEquals("Dune: Part Three", r.movieTitle());
        var seats = showService.getSeatsForShow(r.showId());
        assertEquals(17, seats.size());
    }

    @Test
    void createShow_invalidMovie_throws() {
        assertThrows(ResourceNotFoundException.class, () ->
                showService.createShow(new ShowCreateRequest(
                        999L, pvrBlr.getId(), LocalDate.now(),
                        LocalTime.of(20, 0), LocalTime.of(22, 0),
                        new BigDecimal("300"), Map.of("REGULAR", 10))));
    }

    @Test
    void createShow_invalidTheatre_throws() {
        assertThrows(ResourceNotFoundException.class, () ->
                showService.createShow(new ShowCreateRequest(
                        dune.getId(), 999L, LocalDate.now(),
                        LocalTime.of(20, 0), LocalTime.of(22, 0),
                        new BigDecimal("300"), Map.of("REGULAR", 10))));
    }
}
