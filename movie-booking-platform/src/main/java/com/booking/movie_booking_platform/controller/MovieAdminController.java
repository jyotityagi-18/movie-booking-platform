package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.dto.MovieCreateRequest;
import com.booking.movie_booking_platform.dto.MovieResponse;
import com.booking.movie_booking_platform.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
public class MovieAdminController {

    private final ShowService showService;

    
    @GetMapping("/findAll")
    public ResponseEntity<List<MovieResponse>> listMovies() {
        return ResponseEntity.ok(showService.listMovies());
    }

    
    @PostMapping("/create")
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(showService.createMovie(request));
    }
}

