document.addEventListener('DOMContentLoaded', function() {
    // 1분마다 날씨 데이터 업데이트
    setInterval(updateWeatherData, 60000);
    
    // 페이지 로드 시 초기 데이터가 없으면 바로 업데이트
    if (document.querySelector('#weather-cards .alert')) {
        updateWeatherData();
    }
});

// 날씨 데이터 업데이트 함수
function updateWeatherData() {
    fetch('/api/weather')
        .then(response => response.json())
        .then(data => {
            const weatherCardsContainer = document.getElementById('weather-cards');
            
            // 데이터가 없으면 로딩 메시지 표시
            if (data.length === 0) {
                weatherCardsContainer.innerHTML = `
                    <div class="col-12 text-center">
                        <div class="alert alert-info">
                            날씨 데이터를 불러오는 중입니다. 잠시만 기다려주세요...
                        </div>
                    </div>
                `;
                return;
            }
            
            // 데이터가 있으면 날씨 카드 생성
            let cardsHtml = '';
            
            data.forEach(weather => {
                cardsHtml += `
                    <div class="col-md-4 mb-4">
                        <div class="card weather-card">
                            <div class="card-header">
                                <h3 class="city-name">${weather.city}</h3>
                            </div>
                            <div class="card-body">
                                <div class="weather-icon">
                                    <img src="https://cdn.weatherapi.com/weather/64x64/${weather.weatherIcon}" alt="날씨 아이콘">
                                </div>
                                <div class="weather-info">
                                    <p class="temperature">${weather.temperature.toFixed(1)}°C</p>
                                    <p class="description">${weather.weatherDescription}</p>
                                    <div class="details">
                                        <p><strong>습도:</strong> <span>${weather.humidity}%</span></p>
                                        <p><strong>풍속:</strong> <span>${weather.windSpeed} m/s</span></p>
                                    </div>
                                </div>
                            </div>
                            <div class="card-footer text-muted">
                                <small>${formatDateTime(weather.updatedAt)}</small>
                            </div>
                        </div>
                    </div>
                `;
            });
            
            weatherCardsContainer.innerHTML = cardsHtml;
            
            console.log('날씨 데이터가 업데이트되었습니다.');
        })
        .catch(error => {
            console.error('날씨 데이터 업데이트 중 오류 발생:', error);
        });
}

// 날짜 형식 변환 함수
function formatDateTime(dateTimeString) {
    const dateTime = new Date(dateTimeString);
    
    const year = dateTime.getFullYear();
    const month = String(dateTime.getMonth() + 1).padStart(2, '0');
    const day = String(dateTime.getDate()).padStart(2, '0');
    const hours = String(dateTime.getHours()).padStart(2, '0');
    const minutes = String(dateTime.getMinutes()).padStart(2, '0');
    const seconds = String(dateTime.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
} 