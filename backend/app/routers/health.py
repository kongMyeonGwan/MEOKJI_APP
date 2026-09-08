"""Health Check 라우터.

서버가 살아있는지, DB 연결이 되는지를 한 번에 확인한다.
"""

from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.database.database import get_db

router = APIRouter(prefix="/api", tags=["health"])


@router.get("/health")
def health_check(db: Session = Depends(get_db)):
    """앱 + DB 상태 확인.

    DB 연결 실패 시 503을 반환한다(앱은 떠 있지만 정상 서비스는 불가한 상태).
    """
    db_status = "ok"
    db_error: str | None = None

    try:
        # Oracle은 FROM 절 없는 SELECT를 허용하지 않는다 (DUAL 필수)
        db.execute(text("SELECT 1 FROM DUAL"))
    except SQLAlchemyError as exc:
        db_status = "error"
        # 운영에서 커넥션 문자열이 노출되지 않도록 예외 타입만 남긴다
        db_error = type(exc).__name__

    # DB가 안 되면 앱은 떠 있어도 정상 서비스는 못 하는 상태다.
    # 그래서 상태 문구와 HTTP 상태 코드를 같이 바꿔준다.
    if db_status == "ok":
        overall_status = "ok"
        status_code = 200
    else:
        overall_status = "degraded"
        status_code = 503

    body = {
        "status": overall_status,
        "app": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "env": settings.APP_ENV,
        "database": db_status,
    }
    if db_error:
        body["database_error"] = db_error

    return JSONResponse(status_code=status_code, content=body)
