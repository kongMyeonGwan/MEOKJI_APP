"""올라온 파일을 디스크에 저장하고 다시 찾아오는 일 (STEP 26)."""

import uuid
from datetime import date
from pathlib import Path

from fastapi import HTTPException, UploadFile, status

from app.core.config import BASE_DIR, settings

# 한 번에 읽어들일 크기(바이트).
CHUNK_SIZE = 1024 * 1024

# 원래 파일명을 적어둘 컬럼이 VARCHAR2(255 CHAR) 다.
# 더 긴 이름이 오면 저장할 때 ORA-12899 가 나므로 여기서 먼저 자른다.
MAX_ORG_NAME = 255


def check_ext(filename: str | None) -> str:
    """확장자를 꺼내 허용 목록과 대조한다."""
    name = (filename or "").strip()

    if not name:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY, "파일 이름이 없습니다."
        )

    ext = Path(name).suffix.lower().lstrip(".")
    allowed = settings.upload_allowed_ext_list

    if not ext or ext not in allowed:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            f"{', '.join(allowed)} 파일만 올릴 수 있습니다.",
        )

    return ext


def safe_org_name(filename: str | None) -> str:
    """사용자가 올린 이름에서 경로를 떼어내고 길이를 맞춘다."""
    name = Path((filename or "").strip()).name

    # 확장자만 남는 이름(".env")도 그대로 두면 화면이 비어 보인다.
    return name[:MAX_ORG_NAME] or "첨부파일"


def month_path() -> str:
    """오늘 날짜로 저장 폴더 경로를 만든다."""
    today = date.today()

    return f"{settings.UPLOAD_DIR}/{today.year:04d}/{today.month:02d}"


def absolute_path(path: str, name: str) -> Path:
    """DB 에 적힌 (폴더, 파일명) 을 실제 디스크 경로로 바꾼다."""
    target = (BASE_DIR / path / name).resolve()
    root = settings.upload_root.resolve()

    if root != target and root not in target.parents:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND, "첨부파일을 찾을 수 없습니다."
        )

    return target


def save_upload(upload: UploadFile) -> dict:
    """올라온 파일을 디스크에 저장하고 DB 에 적을 값을 돌려준다."""
    ext = check_ext(upload.filename)

    folder = month_path()
    stored_name = f"{uuid.uuid4().hex}.{ext}"
    target = absolute_path(folder, stored_name)

    target.parent.mkdir(parents=True, exist_ok=True)

    size = 0
    try:
        with target.open("wb") as out:
            while chunk := upload.file.read(CHUNK_SIZE):
                size += len(chunk)

                # 다 받아놓고 재는 게 아니라 받으면서 잰다.
                # 넘는 순간 끊어야 디스크와 메모리를 아낀다.
                if size > settings.upload_max_bytes:
                    raise HTTPException(
                        status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                        f"{settings.UPLOAD_MAX_MB}MB 이하만 올릴 수 있습니다.",
                    )

                out.write(chunk)
    except Exception:
        # 크기 초과든 디스크 오류든, 쓰다 만 파일을 남기지 않는다.
        remove_file(folder, stored_name)
        raise

    if size == 0:
        remove_file(folder, stored_name)
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY, "빈 파일은 올릴 수 없습니다."
        )

    return {
        "name": stored_name,
        "org_name": safe_org_name(upload.filename),
        "path": folder,
        "ext": ext,
        "size": size,
    }


def remove_file(path: str | None, name: str | None) -> None:
    """저장된 파일을 지운다."""
    if not path or not name:
        return

    try:
        absolute_path(path, name).unlink(missing_ok=True)
    except (OSError, HTTPException) as exc:
        print(f"[첨부] 파일 삭제 실패: {path}/{name}: {exc}", flush=True)


def human_size(size: int | None) -> str:
    """바이트를 사람이 읽는 크기로. 화면에 '184 KB' 로 보여준다."""
    if not size or size < 0:
        return ""

    if size < 1024:
        return f"{size} B"

    if size < 1024 * 1024:
        return f"{size / 1024:.0f} KB"

    return f"{size / (1024 * 1024):.1f} MB"
