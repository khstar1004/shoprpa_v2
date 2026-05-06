# 사용항목방법Python항목이미지항목로항목항목이미지
FROM python:3.13-slim

# 항목항목디렉터리
WORKDIR /app

# 항목항목변수
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 설치시스템시스템항목
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 업그레이드pip
RUN pip install --upgrade pip

# 복사항목목록항목파일
COPY backend/ai-service/pyproject.toml backend/ai-service/uv.lock ./

# 설치항목목록항목
RUN pip install --no-cache-dir -e .

# 복사항목목록항목코드
COPY backend/ai-service/app/ ./app/

# 복사매칭항목파일(예결과저장에서)
# COPY docker/.env ./.env

# 항목단말항목
EXPOSE 8010


# 시작항목사용
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010", "--proxy-headers", "--workers", "4"]