package com.example.weatherapp.controller;

import com.example.weatherapp.model.WeatherData;
import com.example.weatherapp.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
// import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WeatherController {

    private final WeatherService weatherService;

    // 메인 페이지 렌더링
    @GetMapping("/")
    public String index(Model model) {
        List<WeatherData> weatherDataList = weatherService.getAllWeatherData();
        List<String> cities = weatherService.getAllCities();
        
        model.addAttribute("weatherDataList", weatherDataList);
        model.addAttribute("cities", cities);
        
        return "index";
    }

    // 날씨 데이터 API
    @GetMapping("/api/weather")
    @ResponseBody
    public List<WeatherData> getAllWeatherData() {
        return weatherService.getAllWeatherData();
    }
    
    // 모든 도시 목록 조회 API
    @GetMapping("/api/weather/cities")
    @ResponseBody
    public List<String> getAllCities() {
        log.info("API 요청: 모든 도시 목록 조회");
        return weatherService.getAllCities();
    }
    
    // 도시 검색 API (자동완성용)
    @GetMapping("/api/weather/cities/search")
    @ResponseBody
    public List<String> searchCities(@RequestParam String query) {
        log.info("API 요청: 도시 검색 - 검색어: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        // WeatherService의 searchCities 메소드 사용
        return weatherService.searchCities(query);
    }
    
    // 새로운 도시 추가 API
    @PostMapping("/api/weather/cities")
    @ResponseBody
    public ResponseEntity<?> addCity(@RequestBody Map<String, String> request) {
        String cityName = request.get("city");
        
        if (cityName == null || cityName.trim().isEmpty()) {
            log.warn("API 요청 오류: 도시 이름이 제공되지 않았습니다");
            return ResponseEntity.badRequest().body("도시 이름을 입력해주세요");
        }
        
        log.info("API 요청: 도시 추가 - {}", cityName);
        boolean added = weatherService.addCity(cityName);
        
        if (added) {
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", cityName + " 도시가 성공적으로 추가되었습니다"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "도시 추가에 실패했습니다. 이미 존재하거나 유효하지 않은 도시일 수 있습니다"
            ));
        }
    }
    
    // 도시 삭제 API
    @DeleteMapping("/api/weather/cities/{cityName}")
    @ResponseBody
    public ResponseEntity<?> removeCity(@PathVariable String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            log.warn("API 요청 오류: 도시 이름이 제공되지 않았습니다");
            return ResponseEntity.badRequest().body("도시 이름을 입력해주세요");
        }
        
        log.info("API 요청: 도시 삭제 - {}", cityName);
        boolean removed = weatherService.removeCity(cityName);
        
        if (removed) {
            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", cityName + " 도시가 성공적으로 삭제되었습니다"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "도시 삭제에 실패했습니다. 존재하지 않는 도시일 수 있습니다"
            ));
        }
    }
} 