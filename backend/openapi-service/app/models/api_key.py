from sqlalchemy import Column, Integer, String

from app.database import Base


class OpenAPIDB(Base):
    """Stored OpenAPI credential hash."""

    __tablename__ = "openapi_auth"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50))
    user_id = Column(String(50))
    api_key = Column(String(100), unique=True)
    prefix = Column(String(100))
    created_at = Column(String(50))
    updated_at = Column(String(50))
    is_active = Column(Integer)


class ShoprpaAgentDB(Base):
    """Stored ShopRPA Agent credential."""

    __tablename__ = "astron_agent_auth"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String(50))
    astron_user_name = Column(String(50), nullable=True)
    name = Column(String(50))
    app_id = Column(String(50))
    api_key = Column(String(100))
    api_secret = Column(String(100))
    created_at = Column(String(50))
    updated_at = Column(String(50))
    is_active = Column(Integer)
