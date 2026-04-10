package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.dto.BookingRequest;
import com.booking.movie_booking_platform.dto.BookingResponse;
import com.booking.movie_booking_platform.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    
    @PostMapping("/create")
    public ResponseEntity<BookingResponse> bookTickets(@RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.bookTickets(request));
    }

    
    @GetMapping("getBookingById/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }

    
    @PostMapping("/cancel/{bookingId}")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }
}
