package com.booking.movie_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "theatres")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String address;

    private Integer totalScreens;

    @Column(unique = true)
    private String partnerCode;
}

