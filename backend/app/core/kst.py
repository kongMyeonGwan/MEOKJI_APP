"""한국 시각(KST)과 UTC 사이를 오가는 도우미.

DB 에는 모든 시각을 UTC 로 저장한다(db/01_schema.sql 참고).
그런데 화면에서 사용자가 고르는 날짜는 한국 날짜다.

    화면: "2026-09-06 하루 동안 좋아요 받은 글"
    DB  : 2026-09-05 15:00 UTC ~ 2026-09-06 15:00 UTC

이 9시간을 안 맞추면 경계에서 결과가 어긋난다.
새벽 0시~9시에 눌린 좋아요가 전날 것으로 세어지거나, 아예 빠진다.

여기 모아둔 이유는, 같은 변환이 인기 레시피 기간 검색과 연속 방문일 계산
두 곳에서 필요하기 때문이다. 각자 계산하면 한쪽만 고치는 실수가 난다.
"""

from datetime import date, datetime, timedelta, timezone

# 한국은 서머타임이 없어서 1년 내내 +9시간 고정이다.
# ZoneInfo("Asia/Seoul") 를 쓰지 않는 이유는 윈도우에서 tzdata 패키지를
# 따로 깔아야 하는데, 오프셋이 바뀌지 않는 지역이라 그럴 필요가 없다.
KST = timezone(timedelta(hours=9))


def now_kst() -> datetime:
    """지금 시각을 한국 시각으로."""
    return datetime.now(KST)


def today_kst() -> date:
    """오늘 날짜를 한국 기준으로.

    UTC 로 date.today() 를 쓰면 한국 새벽 0~9시에 전날이 나온다.
    """
    return now_kst().date()


def to_kst_date(moment: datetime) -> date:
    """UTC 로 저장된 시각을 한국 날짜로 바꾼다.

    DB 에서 읽은 값은 UtcTimestamp 가 tzinfo=UTC 를 붙여준다.
    혹시 없이 들어오면 UTC 로 간주한다.
    """
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)

    return moment.astimezone(KST).date()


def day_start_utc(day: date) -> datetime:
    """한국 날짜의 0시 0분을 UTC 시각으로.

    2026-09-06 -> 2026-09-05 15:00 UTC
    """
    return datetime(day.year, day.month, day.day, tzinfo=KST).astimezone(timezone.utc)


def day_end_utc(day: date) -> datetime:
    """한국 날짜의 **다음 날** 0시를 UTC 시각으로.

    범위 비교를 `>= 시작 AND < 끝` 으로 쓰기 위한 값이다.
    `<= 23:59:59` 로 잡으면 그날 마지막 1초 안에 들어온 데이터가 빠진다.
    """
    return day_start_utc(day + timedelta(days=1))
