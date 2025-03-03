-- Supabase에서 날씨 데이터를 저장할 테이블 생성
CREATE TABLE IF NOT EXISTS weather_data (
    id BIGSERIAL PRIMARY KEY,
    city TEXT NOT NULL,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    weather_description TEXT,
    weather_icon TEXT,
    wind_speed DOUBLE PRECISION,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- 도시별로 하나의 최신 레코드만 유지하기 위한 유니크 제약조건
    CONSTRAINT unique_city UNIQUE (city)
);

-- RLS(Row Level Security) 정책 설정
ALTER TABLE weather_data ENABLE ROW LEVEL SECURITY;

-- 인증된 사용자만 데이터를 읽을 수 있도록 정책 설정
CREATE POLICY "Allow authenticated read access" ON weather_data
    FOR SELECT USING (auth.role() = 'authenticated');

-- 인증된 사용자만 데이터를 삽입/업데이트할 수 있도록 정책 설정
CREATE POLICY "Allow authenticated insert access" ON weather_data
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "Allow authenticated update access" ON weather_data
    FOR UPDATE USING (auth.role() = 'authenticated');

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_weather_data_city ON weather_data (city);
CREATE INDEX IF NOT EXISTS idx_weather_data_updated_at ON weather_data (updated_at); 