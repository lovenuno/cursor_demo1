# 도시별 날씨 정보 애플리케이션

이 프로젝트는 스프링 부트와 Supabase를 이용하여 여러 도시의 현재 날씨 정보를 1분 단위로 갱신하여 보여주는 웹 애플리케이션입니다.

## 기능

- 여러 도시(서울, 도쿄, 뉴욕, 런던, 파리)의 실시간 날씨 정보 표시
- 1분 단위로 자동 업데이트
- 온도, 습도, 풍속, 날씨 상태 등 다양한 날씨 정보 제공
- Supabase를 이용한 데이터 저장 및 관리
- 반응형 웹 디자인

## 기술 스택

- **백엔드**: Spring Boot 3.2.3
- **프론트엔드**: Thymeleaf, HTML, CSS, JavaScript
- **데이터베이스**: Supabase
- **API**: WeatherAPI.com
- **빌드 도구**: Gradle

## 시작하기

### 사전 요구사항

- JDK 17 이상
- Gradle
- Supabase 계정
- WeatherAPI.com API 키

### 설정

1. `application.properties` 파일에서 다음 설정을 업데이트하세요:

```properties
supabase.url=YOUR_SUPABASE_URL
supabase.key=YOUR_SUPABASE_API_KEY
weatherapi.api.key=YOUR_WEATHERAPI_KEY
```

2. Supabase에서 `weather_data` 테이블을 생성하세요. SQL 스크립트는 `supabase/migrations/create_weather_table.sql` 파일에 있습니다.

### 실행

```bash
./gradlew bootRun
```

애플리케이션은 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 프로젝트 구조

```
weather-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── weatherapp/
│   │   │               ├── controller/
│   │   │               ├── service/
│   │   │               ├── model/
│   │   │               ├── config/
│   │   │               └── WeatherApplication.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/
│   │       │   └── js/
│   │       ├── templates/
│   │       └── application.properties
│   └── test/
├── supabase/
│   └── migrations/
│       └── create_weather_table.sql
├── build.gradle
└── README.md
```

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 