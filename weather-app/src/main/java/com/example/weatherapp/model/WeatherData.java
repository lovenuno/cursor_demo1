package com.example.weatherapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    private Long id;
    private String city;
    private Double temperature;
    private Double humidity;
    private String weatherDescription;
    private String weatherIcon;
    private Double windSpeed;
    private LocalDateTime updatedAt;
} 