# Python service runtime image
FROM python:3.13-slim

ARG UV_VERSION=0.11.9

# Application directory
WORKDIR /app

# Runtime defaults
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app \
    VIRTUAL_ENV=/app/.venv \
    PATH="/app/.venv/bin:$PATH" \
    UV_NO_CACHE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# System build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install the lockfile-aware Python package manager.
RUN pip install --upgrade pip "uv==${UV_VERSION}"

# Copy dependency metadata first for better Docker cache reuse.
COPY backend/ai-service/pyproject.toml backend/ai-service/uv.lock backend/ai-service/README.md ./

# Install locked runtime dependencies.
RUN uv sync --locked --no-dev --no-install-project

# Copy application code.
COPY backend/ai-service/app/ ./app/

# Optional environment file for local debugging.
# COPY docker/.env ./.env

EXPOSE 8010

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010", "--proxy-headers", "--workers", "4"]
