package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.TestFixtures;
import com.booking.movie_booking_platform.dto.BookingRequest;
import com.booking.movie_booking_platform.dto.BookingResponse;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingControllerTest {

    @Autowired BookingController bookingController;
    @Autowired MovieRepository movieRepo;
    @Autowired TheatreRepository theatreRepo;
    @Autowired ShowRepository showRepo;
    @Autowired SeatRepository seatRepo;

    Show show;

    @BeforeEach
    void setUp() {
        Movie movie = TestFixtures.movie(movieRepo, "Dune", "English", "Sci-Fi");
        Theatre theatre = TestFixtures.theatre(theatreRepo, "PVR", "Bangalore");
        show = TestFixtures.show(showRepo, movie, theatre,
                LocalDate.now(), LocalTime.of(14, 0), new BigDecimal("300"), 20);
        TestFixtures.seats(seatRepo, show, 12, 6, 2);
    }

    @Test
    void bookTickets_returns201() {
        List<Long> ids = availableIds(2);
        ResponseEntity<BookingResponse> response = bookingController.bookTickets(
                new BookingRequest(show.getId(), "Test", "test@test.com", ids));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("CONFIRMED", response.getBody().status());
    }

    @Test
    void bookTickets_responseContainsPriceBreakdown() {
        List<Long> ids = availableIds(3);
        ResponseEntity<BookingResponse> response = bookingController.bookTickets(
                new BookingRequest(show.getId(), "Test", "test@test.com", ids));

        BookingResponse body = response.getBody();
        assertNotNull(body.priceBreakdown());
        assertEquals(3, body.priceBreakdown().size());
        assertFalse(body.appliedOffers().isEmpty());
    }

    @Test
    void getBooking_returns200() {
        BookingResponse created = bookingController.bookTickets(
                new BookingRequest(show.getId(), "X", "x@x.com", availableIds(1)))
                .getBody();

        ResponseEntity<BookingResponse> response =
                bookingController.getBooking(created.bookingId());
        assertEquals(200, response.getStatusCode().value());
        assertEquals(created.bookingId(), response.getBody().bookingId());
    }

    @Test
    void cancelBooking_returns200() {
        BookingResponse created = bookingController.bookTickets(
                new BookingRequest(show.getId(), "X", "x@x.com", availableIds(1)))
                .getBody();

        ResponseEntity<BookingResponse> response =
                bookingController.cancelBooking(created.bookingId());
        assertEquals(200, response.getStatusCode().value());
        assertEquals("CANCELLED", response.getBody().status());
    }

    private List<Long> availableIds(int count) {
        return seatRepo.findByShowIdAndStatus(show.getId(), SeatStatus.AVAILABLE)
                .stream().limit(count).map(Seat::getId).collect(Collectors.toList());
    }
}

