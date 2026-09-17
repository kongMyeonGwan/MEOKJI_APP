"""한국 시각(KST)과 UTC 사이를 오가는 도우미."""

from datetime import date, datetime, timedelta, timezone

# 한국은 서머타임이 없어서 1년 내내 +9시간 고정이다.
KST = timezone(timedelta(hours=9))


def now_kst() -> datetime:
    """지금 시각을 한국 시각으로."""
    return datetime.now(KST)


def today_kst() -> date:
    """오늘 날짜를 한국 기준으로."""
    return now_kst().date()


def to_kst_date(moment: datetime) -> date:
    """UTC 로 저장된 시각을 한국 날짜로 바꾼다."""
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)

    return moment.astimezone(KST).date()


def day_start_utc(day: date) -> datetime:
    """한국 날짜의 0시 0분을 UTC 시각으로."""
    return datetime(day.year, day.month, day.day, tzinfo=KST).astimezone(timezone.utc)


def day_end_utc(day: date) -> datetime:
    """한국 날짜의 **다음 날** 0시를 UTC 시각으로."""
    return day_start_utc(day + timedelta(days=1))
