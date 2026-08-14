"""FastAPI 애플리케이션 진입점."""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.routers import auth, health

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="오늘 뭐 먹지? — AI/API 중심 음식 추천 서비스",
    docs_url="/docs",
    redoc_url=None,
)

# React(Vite) 개발 서버에서의 호출 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(auth.router)


@app.get("/", include_in_schema=False)
def root():
    return {
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "docs": "/docs",
        "health": "/api/health",
    }
