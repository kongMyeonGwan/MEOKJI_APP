"""서버가 시작될 때 제일 먼저 실행되는 파일."""

import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.routers import (
    auth,
    bans,
    catalog,
    games,
    health,
    histories,
    likes,
    notices,
    posts,
    recommendations,
    users,
)

from app.services.bargain_service import warm_bargains


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """서버가 뜰 때 KAMIS 시세를 미리 한 번 훑어둔다."""
    asyncio.create_task(warm_bargains())
    yield


app = FastAPI(
    lifespan=lifespan,
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="오늘 뭐 먹지? — AI/API 중심 음식 추천 서비스",
    docs_url="/docs",   # API 문서 주소
    redoc_url=None,     # 문서가 두 종류라 헷갈려서 하나만 쓴다
)

# React(Vite) 개발 서버는 5173 포트, 이 서버는 8000 포트다.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],   # GET, POST, PUT, DELETE 전부 허용
    allow_headers=["*"],   # Authorization 헤더 등 전부 허용
)

# 기능별로 나눠둔 라우터를 앱에 붙인다.
# 각 라우터가 자기 주소(prefix)를 들고 있다. 예: posts -> /api/posts
app.include_router(health.router)           # 서버 · DB 살아있는지 확인
app.include_router(auth.router)             # 회원가입 · 로그인 · 비밀번호 재설정
app.include_router(users.router)            # 내 정보 · 알림 · 탈퇴
app.include_router(bans.router)             # 못 먹는 재료 관리
app.include_router(histories.router)        # 내 식사 기록
app.include_router(posts.router)            # 후기 게시판 · 인기 레시피
app.include_router(recommendations.router)  # AI 추천 · 레시피
app.include_router(catalog.router)          # 카테고리 · 룰렛 · 게임 같은 고정 목록
app.include_router(notices.router)          # 공지사항 (NOTICES 표)
app.include_router(games.router)            # 미니게임 플레이 기록 (GAME_HISTORIES 표)
app.include_router(likes.router)             # 찜한 레시피 (RECIPE_LIKES 표)


@app.get("/", include_in_schema=False)
def root():
    """주소창에 http://localhost:8000 만 쳤을 때 보여줄 안내."""
    return {
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "docs": "/docs",
        "health": "/api/health",
    }
