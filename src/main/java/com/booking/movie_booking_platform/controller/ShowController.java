package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.dto.SeatResponse;
import com.booking.movie_booking_platform.dto.ShowSearchResponse;
import com.booking.movie_booking_platform.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    
    @GetMapping("/search")
    public ResponseEntity<List<ShowSearchResponse>> searchShows(
            @RequestParam String city,
            @RequestParam(required = false) String movieName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.searchShows(city, movieName, date));
    }

    
    @GetMapping("/seats/{showId}")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long showId) {
        return ResponseEntity.ok(showService.getSeatsForShow(showId));
    }
}
