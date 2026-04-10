package com.booking.movie_booking_platform.service;

import com.booking.movie_booking_platform.TestFixtures;
import com.booking.movie_booking_platform.dto.BookingRequest;
import com.booking.movie_booking_platform.dto.BookingResponse;
import com.booking.movie_booking_platform.entity.*;
import com.booking.movie_booking_platform.exception.InsufficientSeatsException;
import com.booking.movie_booking_platform.exception.InvalidRequestException;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingServiceTest {

    @Autowired BookingService bookingService;
    @Autowired MovieRepository movieRepo;
    @Autowired TheatreRepository theatreRepo;
    @Autowired ShowRepository showRepo;
    @Autowired SeatRepository seatRepo;

    Show morningShow;
    Show afternoonShow;

    @BeforeEach
    void setUp() {
        Movie movie = TestFixtures.movie(movieRepo, "Dune", "English", "Sci-Fi");
        Theatre theatre = TestFixtures.theatre(theatreRepo, "PVR", "Bangalore");

        morningShow = TestFixtures.show(showRepo, movie, theatre,
                LocalDate.now(), LocalTime.of(10, 0), new BigDecimal("250"), 30);
        TestFixtures.seats(seatRepo, morningShow, 18, 9, 3);

        afternoonShow = TestFixtures.show(showRepo, movie, theatre,
                LocalDate.now(), LocalTime.of(14, 0), new BigDecimal("300"), 20);
        TestFixtures.seats(seatRepo, afternoonShow, 12, 6, 2);
    }

    

    @Test
    void bookTickets_success_returnsConfirmed() {
        List<Long> ids = available(morningShow.getId(), 2);
        BookingResponse r = bookingService.bookTickets(
                new BookingRequest(morningShow.getId(), "Alice", "alice@test.com", ids));

        assertNotNull(r.bookingId());
        assertEquals("CONFIRMED", r.status());
        assertEquals(2, r.numberOfTickets());
        assertEquals(2, r.priceBreakdown().size());
    }

    @Test
    void bookTickets_reducesAvailableSeats() {
        int before = showRepo.findById(morningShow.getId()).orElseThrow().getAvailableSeats();
        bookingService.bookTickets(
                new BookingRequest(morningShow.getId(), "Bob", "bob@test.com",
                        available(morningShow.getId(), 3)));
        int after = showRepo.findById(morningShow.getId()).orElseThrow().getAvailableSeats();
        assertEquals(before - 3, after);
    }

    @Test
    void bookTickets_seatStatusChangesToBooked() {
        List<Long> ids = available(morningShow.getId(), 2);
        bookingService.bookTickets(
                new BookingRequest(morningShow.getId(), "X", "x@x.com", ids));
        ids.forEach(id -> {
            Seat seat = seatRepo.findById(id).orElseThrow();
            assertEquals(SeatStatus.BOOKED, seat.getStatus());
            assertNotNull(seat.getBooking());
        });
    }

    

    @Test
    void bookTickets_morningShow1Ticket_noDiscount() {
        List<Long> ids = regular(morningShow.getId(), 1);
        BookingResponse r = bookingService.bookTickets(
                new BookingRequest(morningShow.getId(), "T", "t@t.com", ids));

        assertEquals(0, r.discountAmount().signum());
        assertTrue(r.appliedOffers().isEmpty());
        assertEquals(r.totalAmount(), r.finalAmount());
    }

    

    @Test
    void bookTickets_morningShow3Tickets_thirdTicketDiscountOnly() {
        List<Long> ids = regular(morningShow.getId(), 3);
        BookingResponse r = bookingService.bookTickets(
                new BookingRequest(morningShow.getId(), "T", "t@t.com", ids));

        assertTrue(r.discountAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(1, r.appliedOffers().size());
        assertTrue(r.appliedOffers().contains("50% discount on every 3rd ticket"));
    }

    

    @Test
    void bookTickets_afternoonShow1Ticket_afternoonDiscountApplied() {
        List<Long> ids = available(afternoonShow.getId(), 1);
        BookingResponse r = bookingService.bookTickets(
                new BookingRequest(afternoonShow.getId(), "T", "t@t.com", ids));

        assertTrue(r.discountAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(r.appliedOffers().contains("20% discount for afternoon show"));
    }

    

    @Test
    void bookTickets_afternoonShow3Tickets_bothDiscountsStack() {
        List<Long> ids = regular(afternoonShow.getId(), 3);
        BookingResponse r = bookingService.bookTickets(
                new BookingRequest(afternoonShow.getId(), "T", "t@t.com", ids));

        assertEquals(2, r.appliedOffers().size());
        assertTrue(r.finalAmount().compareTo(r.totalAmount()) < 0);
        assertEquals(3, r.priceBreakdown().size());
    }

    

    @Test
    void bookTickets_invalidSeatIds_throws() {
        assertThrows(InsufficientSeatsException.class, () ->
                bookingService.bookTickets(
                        new BookingRequest(morningShow.getId(), "X", "x@x.com", List.of(999999L))));
    }

    @Test
    void bookTickets_blankName_throws() {
        assertThrows(InvalidRequestException.class, () ->
                bookingService.bookTickets(
                        new BookingRequest(morningShow.getId(), "", "x@x.com",
                                available(morningShow.getId(), 1))));
    }

    @Test
    void bookTickets_blankEmail_throws() {
        assertThrows(InvalidRequestException.class, () ->
                bookingService.bookTickets(
                        new BookingRequest(morningShow.getId(), "X", "",
                                available(morningShow.getId(), 1))));
    }

    @Test
    void bookTickets_emptySeatList_throws() {
        assertThrows(InvalidRequestException.class, () ->
                bookingService.bookTickets(
                        new BookingRequest(morningShow.getId(), "X", "x@x.com", List.of())));
    }

    @Test
    void bookTickets_invalidShowId_throws() {
        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.bookTickets(
                        new BookingRequest(999999L, "X", "x@x.com", List.of(1L))));
    }

    

    @Test
    void cancelBooking_success() {
        BookingResponse booked = book(morningShow.getId(), 2);
        BookingResponse cancelled = bookingService.cancelBooking(booked.bookingId());
        assertEquals("CANCELLED", cancelled.status());
        assertTrue(cancelled.priceBreakdown().isEmpty());
    }

    @Test
    void cancelBooking_restoresAvailableSeats() {
        int before = showRepo.findById(morningShow.getId()).orElseThrow().getAvailableSeats();
        BookingResponse booked = book(morningShow.getId(), 2);
        bookingService.cancelBooking(booked.bookingId());
        int after = showRepo.findById(morningShow.getId()).orElseThrow().getAvailableSeats();
        assertEquals(before, after);
    }

    @Test
    void cancelBooking_releasesSeatStatus() {
        List<Long> ids = available(morningShow.getId(), 2);
        BookingResponse booked = bookingService.bookTickets(
                new BookingRequest(morningShow.getId(), "X", "x@x.com", ids));
        bookingService.cancelBooking(booked.bookingId());
        ids.forEach(id -> assertEquals(SeatStatus.AVAILABLE,
                seatRepo.findById(id).orElseThrow().getStatus()));
    }

    @Test
    void cancelBooking_alreadyCancelled_throws() {
        BookingResponse booked = book(morningShow.getId(), 1);
        bookingService.cancelBooking(booked.bookingId());
        assertThrows(InvalidRequestException.class,
                () -> bookingService.cancelBooking(booked.bookingId()));
    }

    @Test
    void cancelBooking_invalidId_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.cancelBooking(999999L));
    }

    

    @Test
    void getBooking_confirmed_hasPriceBreakdown() {
        BookingResponse created = book(morningShow.getId(), 1);
        BookingResponse fetched = bookingService.getBooking(created.bookingId());
        assertEquals(created.bookingId(), fetched.bookingId());
        assertFalse(fetched.priceBreakdown().isEmpty());
    }

    @Test
    void getBooking_cancelled_emptyBreakdown() {
        BookingResponse booked = book(morningShow.getId(), 1);
        bookingService.cancelBooking(booked.bookingId());
        BookingResponse fetched = bookingService.getBooking(booked.bookingId());
        assertEquals("CANCELLED", fetched.status());
        assertTrue(fetched.priceBreakdown().isEmpty());
    }

    @Test
    void getBooking_invalidId_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getBooking(999999L));
    }

    

    private BookingResponse book(Long showId, int count) {
        return bookingService.bookTickets(
                new BookingRequest(showId, "Test", "test@test.com", available(showId, count)));
    }

    private List<Long> available(Long showId, int count) {
        return seatRepo.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE)
                .stream().limit(count).map(Seat::getId).collect(Collectors.toList());
    }

    private List<Long> regular(Long showId, int count) {
        return seatRepo.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE)
                .stream().filter(s -> s.getSeatType() == SeatType.REGULAR)
                .limit(count).map(Seat::getId).collect(Collectors.toList());
    }
}
