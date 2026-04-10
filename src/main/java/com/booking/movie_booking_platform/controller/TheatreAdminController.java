package com.booking.movie_booking_platform.controller;

import com.booking.movie_booking_platform.dto.*;
import com.booking.movie_booking_platform.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/theatres")
@RequiredArgsConstructor
public class TheatreAdminController {

    private final ShowService showService;

    
    @GetMapping("/findAll")
    public ResponseEntity<List<TheatreResponse>> listTheatres() {
        return ResponseEntity.ok(showService.listTheatres());
    }

    
    @PostMapping("/create")
    public ResponseEntity<TheatreResponse> createTheatre(@RequestBody TheatreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(showService.createTheatre(request));
    }

    
    @PostMapping("/shows")
    public ResponseEntity<ShowSearchResponse> createShow(@RequestBody ShowCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(showService.createShow(request));
    }
}
