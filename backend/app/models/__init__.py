"""테이블 모델 모음."""

from app.models.game_history import game_histories
from app.models.meal_history import MealHistory
from app.models.notice import Notice
from app.models.recipe_review import RecipeReview
from app.models.review_comment import ReviewComment
from app.models.review_like import ReviewLike
from app.models.user import User
from app.models.user_ban import UserBan
from app.models.verification_code import VerificationCode

# from app.models import * 를 썼을 때 넘어갈 이름들
__all__ = [
    "game_histories",
    "MealHistory",
    "Notice",
    "RecipeReview",
    "ReviewComment",
    "ReviewLike",
    "User",
    "UserBan",
    "VerificationCode",
]
