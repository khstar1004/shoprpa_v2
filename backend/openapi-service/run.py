#!/usr/bin/env python3
"""
ShopRPA OpenAPI service launcher.
Pass an environment name to load .env.<name> before starting the service.

Examples:
    python run.py
    python run.py dev
    python run.py prod
    python run.py test

Environment:
    ENV=dev
"""

import os
import sys

import uvicorn
from dotenv import load_dotenv


def load_environment_config(env_name=None):
    """Load .env.default, .env, and optionally .env.<name>."""
    base_dir = os.path.dirname(os.path.abspath(__file__))

    if env_name:
        env_file = f".env.{env_name}"
        env_path = os.path.join(base_dir, env_file)

        if os.path.exists(env_path):
            print(f"Loading environment from: {env_file}")
            load_dotenv(env_path, override=True)
        else:
            print(f"Warning: Environment file {env_file} not found, using default configuration")
            load_dotenv(os.path.join(base_dir, ".env.default"))
            load_dotenv(os.path.join(base_dir, ".env"), override=True)
    else:
        print("Using default environment configuration")
        load_dotenv(os.path.join(base_dir, ".env.default"))
        load_dotenv(os.path.join(base_dir, ".env"), override=True)


def main():
    env_name = os.getenv("ENV") or (sys.argv[1] if len(sys.argv) > 1 else None)
    if not env_name:
        env_name = "local"

    load_environment_config(env_name)

    print(f"Starting OpenAPI Service with environment: {env_name or 'default'}")

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8020,
        proxy_headers=True,
        reload=env_name == "dev",
    )


if __name__ == "__main__":
    main()
