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
COPY backend/openapi-service/pyproject.toml backend/openapi-service/uv.lock backend/openapi-service/README.md ./

# Copy the bundled websocket client wheel at the path declared in pyproject.toml.
RUN mkdir -p ./app/utils
COPY backend/openapi-service/app/utils/rpawebsocket-1.0.6-py3-none-any.whl ./app/utils/

# Install locked runtime dependencies.
RUN uv sync --locked --no-dev --no-install-project

# Copy application code.
COPY backend/openapi-service/app/ ./app/

# Optional environment file for local debugging.
# COPY docker/.env ./.env

EXPOSE 8020

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8020", "--proxy-headers"]
