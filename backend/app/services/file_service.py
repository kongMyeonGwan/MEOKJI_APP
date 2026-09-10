"""올라온 파일을 디스크에 저장하고 다시 찾아오는 일 (STEP 26).

이 파일은 "후기" 를 모른다
--------------------------
파일을 검사하고 · 저장하고 · 지우는 것만 한다. 그 파일이 어느 게시글에
붙는지, 누가 올릴 수 있는지는 post_service 가 정한다. 나중에 프로필
사진처럼 다른 곳에 파일이 붙어도 이 파일은 그대로 쓴다.

DB 에는 무엇이 들어가나
-----------------------
파일 본체는 DB 에 넣지 않는다. 디스크에 두고 RECIPE_REVIEWS 의 FILE_*
컬럼에 위치만 적는다. BLOB 으로 넣으면 목록 조회마다 사진이 딸려 나와
게시판이 느려지고, 백업 용량이 사진 용량만큼 그대로 불어난다.

    file_name      a1b2c3d4....jpg     서버에 저장한 이름
    file_org_name  김치찌개.jpg         사용자가 올린 원래 이름
    file_path      uploads/2026/09     저장 폴더 (파일명은 안 넣는다)
    file_ext       jpg
    file_size      188_412

이름을 왜 바꿔서 저장하나
-------------------------
사용자가 올린 이름을 그대로 쓰면 두 가지가 깨진다.

    같은 이름끼리 덮어쓴다        "사진.jpg" 를 두 사람이 올리면 하나가 사라진다
    경로가 섞여 들어올 수 있다    "../../.env" 같은 이름이면 서버 파일을 건드린다

그래서 저장할 때는 UUID 로 새 이름을 짓고, 원래 이름은 보여주기용으로만
따로 적어둔다. 내려줄 때 원래 이름으로 돌려주므로 사용자는 차이를 못 느낀다.
"""

import uuid
from datetime import date
from pathlib import Path

from fastapi import HTTPException, UploadFile, status

from app.core.config import BASE_DIR, settings

# 한 번에 읽어들일 크기(바이트).
#
# 파일을 통째로 메모리에 올리지 않는다. 5MB 짜리를 열 명이 동시에 올리면
# 50MB 가 한꺼번에 잡힌다. 조각으로 읽으면서 크기를 세면 제한을 넘는 순간
# 바로 끊을 수 있다.
CHUNK_SIZE = 1024 * 1024

# 원래 파일명을 적어둘 컬럼이 VARCHAR2(255 CHAR) 다.
# 더 긴 이름이 오면 저장할 때 ORA-12899 가 나므로 여기서 먼저 자른다.
MAX_ORG_NAME = 255


def check_ext(filename: str | None) -> str:
    """확장자를 꺼내 허용 목록과 대조한다. 점 없이 소문자로 돌려준다.

    허용 목록 방식이다. "이건 안 된다" 로 적으면 빠뜨린 것이 곧 구멍이 된다.
    """
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
    """사용자가 올린 이름에서 경로를 떼어내고 길이를 맞춘다.

    Path(...).name 이 "../../.env" 를 ".env" 로, "C:\\키\\a.jpg" 를
    "a.jpg" 로 줄인다. 이 값은 화면 표시와 내려받기 이름으로만 쓰지만,
    디스크에 닿을 일이 없다고 해서 경로를 남겨둘 이유도 없다.
    """
    name = Path((filename or "").strip()).name

    # 확장자만 남는 이름(".env")도 그대로 두면 화면이 비어 보인다.
    return name[:MAX_ORG_NAME] or "첨부파일"


def month_path() -> str:
    """오늘 날짜로 저장 폴더 경로를 만든다.  ->  'uploads/2026/09'

    한 폴더에 수만 개가 쌓이면 파일 탐색기도 백업 도구도 느려진다.
    월별로 나눠두면 나중에 "2026년 9월 것만 옮기기" 같은 정리도 쉽다.
    """
    today = date.today()

    return f"{settings.UPLOAD_DIR}/{today.year:04d}/{today.month:02d}"


def absolute_path(path: str, name: str) -> Path:
    """DB 에 적힌 (폴더, 파일명) 을 실제 디스크 경로로 바꾼다.

    FILE_PATH 가 "uploads/2026/09" 처럼 UPLOAD_DIR 을 포함한 상대 경로라서
    이어붙일 기준은 backend 폴더(BASE_DIR)다.

    붙인 결과가 업로드 폴더 밖으로 나가면 막는다. 지금은 경로도 파일명도
    서버가 지어낸 값이라 밖으로 나갈 수 없지만, DB 값을 그대로 믿고
    열어주는 코드는 언젠가 반드시 사고가 난다.
    """
    target = (BASE_DIR / path / name).resolve()
    root = settings.upload_root.resolve()

    if root != target and root not in target.parents:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND, "첨부파일을 찾을 수 없습니다."
        )

    return target


def save_upload(upload: UploadFile) -> dict:
    """올라온 파일을 디스크에 저장하고 DB 에 적을 값을 돌려준다.

    돌려주는 딕셔너리는 RecipeReview.set_file() 의 인자와 이름이 같다.
    """
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
    """저장된 파일을 지운다. 이미 없으면 조용히 넘어간다.

    DB 행은 지워졌는데 파일이 남는 것보다, 파일은 없는데 DB 가 정리되는
    쪽이 낫다. 그래서 여기서 예외를 던져 호출부를 멈춰 세우지 않는다.
    """
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
