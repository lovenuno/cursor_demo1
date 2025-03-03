package com.example.weatherapp.service;

import com.example.weatherapp.model.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    @Qualifier("supabaseWebClient")
    private final WebClient supabaseWebClient;
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${weatherapi.api.key}")
    private String apiKey;

    // 날씨 정보를 가져올 도시 목록 - CopyOnWriteArrayList로 변경하여 스레드 안전성 확보
    private final List<String> cities = new CopyOnWriteArrayList<>(Arrays.asList("Seoul", "Tokyo", "New York", "London", "Paris"));
    
    // 현재 메모리에 저장된 날씨 데이터
    private List<WeatherData> weatherDataList = new ArrayList<>();

    // 1분마다 날씨 데이터 업데이트
    @Scheduled(fixedRate = 60000)
    public void updateWeatherData() {
        log.info("===== 날씨 데이터 업데이트 시작: {} =====", LocalDateTime.now());
        
        List<WeatherData> newWeatherDataList = new ArrayList<>();
        
        for (String city : cities) {
            try {
                log.info("[{}] 날씨 데이터 가져오기 시작", city);
                WeatherData weatherData = fetchWeatherData(city);
                newWeatherDataList.add(weatherData);
                log.info("[{}] 날씨 데이터 가져오기 완료: 온도={}, 습도={}, 풍속={}", 
                        city, weatherData.getTemperature(), weatherData.getHumidity(), weatherData.getWindSpeed());
                saveWeatherDataToSupabase(weatherData);
            } catch (Exception e) {
                log.error("[{}] 날씨 데이터 업데이트 실패: {}", city, e.getMessage());
                log.error("[{}] 상세 오류: ", city, e);
            }
        }
        
        this.weatherDataList = newWeatherDataList;
        log.info("===== 날씨 데이터 업데이트 완료: {} =====", LocalDateTime.now());
    }

    // WeatherAPI.com에서 날씨 데이터 가져오기
    private WeatherData fetchWeatherData(String city) {
        String url = "https://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + city + "&aqi=no";
        log.info("[{}] WeatherAPI.com API 호출: {}", city, url.replace(apiKey, "API_KEY"));
        
        String response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        log.info("[{}] WeatherAPI.com 응답 수신 완료", city);
        
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode location = root.path("location");
            JsonNode current = root.path("current");
            JsonNode condition = current.path("condition");
            
            log.info("[{}] 날씨 데이터 파싱 시작", city);
            
            // 풍속을 km/h에서 m/s로 변환하고 소수점 2자리까지 반올림
            double windKph = current.path("wind_kph").asDouble();
            double windMps = windKph / 3.6; // km/h를 m/s로 변환
            BigDecimal roundedWindSpeed = new BigDecimal(windMps).setScale(2, RoundingMode.HALF_UP);
            
            log.info("[{}] 풍속 변환: {}km/h -> {}m/s (반올림 후: {}m/s)", 
                    city, windKph, windMps, roundedWindSpeed);
            
            return WeatherData.builder()
                    .city(location.path("name").asText())
                    .temperature(current.path("temp_c").asDouble())
                    .humidity(current.path("humidity").asDouble())
                    .weatherDescription(condition.path("text").asText())
                    .weatherIcon(condition.path("icon").asText().replace("//cdn.weatherapi.com/weather/64x64/", ""))
                    .windSpeed(roundedWindSpeed.doubleValue()) // 소수점 2자리로 반올림된 풍속
                    .updatedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("[{}] 날씨 데이터 파싱 실패: {}", city, e.getMessage());
            log.error("[{}] 파싱 오류 상세: ", city, e);
            throw new RuntimeException("날씨 데이터 파싱 실패", e);
        }
    }

    // Supabase에 날씨 데이터 저장 - 데이터가 있으면 update, 없으면 insert
    private void saveWeatherDataToSupabase(WeatherData weatherData) {
        try {
            log.info("===== [{}] Supabase 데이터 저장 프로세스 시작 =====", weatherData.getCity());
            
            // Map을 사용하여 JSON 직렬화 문제 해결
            Map<String, Object> weatherMap = new HashMap<>();
            weatherMap.put("city", weatherData.getCity());
            weatherMap.put("temperature", weatherData.getTemperature());
            weatherMap.put("humidity", weatherData.getHumidity());
            weatherMap.put("weather_description", weatherData.getWeatherDescription());
            weatherMap.put("weather_icon", weatherData.getWeatherIcon());
            weatherMap.put("wind_speed", weatherData.getWindSpeed());
            
            // ISO 형식으로 날짜 변환
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            weatherMap.put("updated_at", weatherData.getUpdatedAt().format(formatter));
            
            try {
                log.info("[{}] 데이터 맵 생성 완료: {}", weatherData.getCity(), objectMapper.writeValueAsString(weatherMap));
            } catch (Exception e) {
                log.info("[{}] 데이터 맵 생성 완료 (직렬화 실패)", weatherData.getCity());
            }
            
            // 1. 먼저 해당 도시의 데이터가 있는지 확인
            String cityName = weatherData.getCity();
            log.info("[{}] Supabase 기존 데이터 확인 시작", cityName);
            
            String queryUrl = "/rest/v1/weather_data?city=eq." + cityName + "&select=*";
            log.info("[{}] 데이터 조회 URL: {}", cityName, queryUrl);
            
            supabaseWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/v1/weather_data")
                            .queryParam("city", "eq." + cityName)
                            .queryParam("select", "*")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSubscribe(s -> log.info("[{}] Supabase 데이터 조회 요청 시작", cityName))
                    .flatMap(response -> {
                        try {
                            log.info("[{}] Supabase 데이터 조회 응답 수신: {}", cityName, response);
                            JsonNode jsonResponse = objectMapper.readTree(response);
                            
                            if (jsonResponse.isArray() && jsonResponse.size() > 0) {
                                // 기존 데이터가 있는 경우 - 업데이트
                                JsonNode existingData = jsonResponse.get(0);
                                int id = existingData.path("id").asInt();
                                log.info("[{}] 기존 데이터 발견 (ID: {}), 업데이트 시작", cityName, id);
                                
                                String updateUrl = "/rest/v1/weather_data?id=eq." + id;
                                log.info("[{}] 데이터 업데이트 URL: {}", cityName, updateUrl);
                                
                                return supabaseWebClient
                                        .patch()
                                        .uri(updateUrl)
                                        .bodyValue(weatherMap)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .doOnSubscribe(s -> log.info("[{}] Supabase 데이터 업데이트 요청 시작 (ID: {})", cityName, id))
                                        .doOnNext(updateResponse -> log.info("[{}] Supabase 업데이트 응답: {}", cityName, updateResponse))
                                        .map(result -> {
                                            log.info("[{}] Supabase 데이터 업데이트 완료 (ID: {})", cityName, id);
                                            return "updated";
                                        });
                            } else {
                                // 기존 데이터가 없는 경우 - 삽입
                                log.info("[{}] 기존 데이터 없음, Supabase에 새 데이터 삽입 시작", cityName);
                                
                                String insertUrl = "/rest/v1/weather_data";
                                log.info("[{}] 데이터 삽입 URL: {}", cityName, insertUrl);
                                
                                return supabaseWebClient
                                        .post()
                                        .uri(insertUrl)
                                        .bodyValue(weatherMap)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .doOnSubscribe(s -> log.info("[{}] Supabase 데이터 삽입 요청 시작", cityName))
                                        .doOnNext(insertResponse -> log.info("[{}] Supabase 삽입 응답: {}", cityName, insertResponse))
                                        .map(result -> {
                                            log.info("[{}] Supabase에 새 데이터 삽입 완료", cityName);
                                            return "inserted";
                                        });
                            }
                        } catch (Exception e) {
                            log.error("[{}] Supabase 응답 처리 중 오류 발생: {}", cityName, e.getMessage());
                            log.error("[{}] 응답 처리 오류 상세: ", cityName, e);
                            return Mono.error(e);
                        }
                    })
                    .doOnError(error -> {
                        log.error("[{}] Supabase 데이터 저장 중 오류 발생: {}", cityName, error.getMessage());
                        log.error("[{}] 오류 상세: ", cityName, error);
                    })
                    .onErrorResume(e -> {
                        log.error("[{}] Supabase 데이터 저장 실패, 계속 진행: {}", cityName, e.getMessage());
                        return Mono.just("error");
                    })
                    .subscribe(
                        result -> log.info("===== [{}] Supabase 데이터 저장 프로세스 완료: {} =====", cityName, result),
                        error -> log.error("===== [{}] Supabase 데이터 저장 최종 에러: {} =====", cityName, error.getMessage())
                    );
            
        } catch (Exception e) {
            log.error("[{}] Supabase에 날씨 데이터 저장 실패: {}", weatherData.getCity(), e.getMessage());
            log.error("[{}] 저장 오류 상세: ", weatherData.getCity(), e);
        }
    }

    // 모든 도시의 최신 날씨 데이터 가져오기
    public List<WeatherData> getAllWeatherData() {
        log.info("모든 도시의 최신 날씨 데이터 요청 수신");
        return weatherDataList;
    }
    
    // 현재 등록된 모든 도시 목록 가져오기
    public List<String> getAllCities() {
        log.info("모든 도시 목록 요청 수신");
        return new ArrayList<>(cities);
    }
    
    // 새로운 도시 추가
    public boolean addCity(String cityName) {
        log.info("[{}] 도시 추가 요청 수신", cityName);
        
        // 이미 존재하는 도시인지 확인
        if (cities.contains(cityName)) {
            log.info("[{}] 이미 등록된 도시입니다", cityName);
            return false;
        }
        
        try {
            // 유효한 도시인지 확인 (WeatherAPI.com에서 데이터를 가져올 수 있는지)
            fetchWeatherData(cityName);
            
            // 유효한 도시이면 목록에 추가
            cities.add(cityName);
            log.info("[{}] 도시가 성공적으로 추가되었습니다", cityName);
            
            // 추가된 도시의 날씨 데이터 즉시 가져오기
            WeatherData weatherData = fetchWeatherData(cityName);
            saveWeatherDataToSupabase(weatherData);
            
            // 메모리에 있는 날씨 데이터 목록에도 추가
            weatherDataList.add(weatherData);
            
            return true;
        } catch (Exception e) {
            log.error("[{}] 도시 추가 실패: {}", cityName, e.getMessage());
            return false;
        }
    }
    
    // 도시 삭제
    public boolean removeCity(String cityName) {
        log.info("[{}] 도시 삭제 요청 수신", cityName);
        
        // 존재하는 도시인지 확인
        if (!cities.contains(cityName)) {
            log.info("[{}] 등록되지 않은 도시입니다", cityName);
            return false;
        }
        
        // 도시 목록에서 삭제
        boolean removed = cities.remove(cityName);
        
        if (removed) {
            log.info("[{}] 도시가 성공적으로 삭제되었습니다", cityName);
            
            // 메모리에 있는 날씨 데이터 목록에서도 삭제
            weatherDataList.removeIf(data -> data.getCity().equals(cityName));
            
            return true;
        } else {
            log.error("[{}] 도시 삭제 실패", cityName);
            return false;
        }
    }
    
    // 도시 이름 검색 (자동완성용)
    public List<String> searchCities(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        String lowercaseQuery = query.toLowerCase();
        log.info("도시 검색: 검색어 '{}'", query);
        
        // 1. 기존 등록된 도시 목록에서 검색
        List<String> results = cities.stream()
                .filter(city -> city.toLowerCase().startsWith(lowercaseQuery))
                .collect(Collectors.toList());
        
        log.info("기존 도시 목록에서 {} 개의 결과 발견", results.size());
        
        // 2. 결과가 적으면 (3개 미만) WeatherAPI.com에서 도시 유효성 확인
        if (results.size() < 3) {
            try {
                // WeatherAPI.com에서 도시 검색 (유효성 확인)
                String apiUrl = "https://api.weatherapi.com/v1/search.json?key=" + apiKey + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
                log.info("외부 API 도시 검색: {}", apiUrl);
                
                WebClient webClient = webClientBuilder.build();
                String response = webClient.get()
                        .uri(apiUrl)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                
                if (response != null) {
                    JsonNode jsonResponse = objectMapper.readTree(response);
                    if (jsonResponse.isArray()) {
                        for (JsonNode cityNode : jsonResponse) {
                            String cityName = cityNode.path("name").asText();
                            String country = cityNode.path("country").asText();
                            String fullName = cityName + ", " + country;
                            
                            // 이미 결과에 있는지 확인하고, 없으면 추가
                            if (!results.contains(cityName) && !results.contains(fullName)) {
                                results.add(fullName);
                            }
                        }
                    }
                }
                
                log.info("외부 API 검색 후 총 {} 개의 결과 발견", results.size());
            } catch (Exception e) {
                log.error("외부 API 도시 검색 중 오류 발생: {}", e.getMessage());
            }
        }
        
        // 최대 10개 결과로 제한
        return results.stream().limit(10).collect(Collectors.toList());
    }
} 