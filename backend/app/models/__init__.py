"""테이블 모델 모음.

여기서 모든 모델을 한 번에 import 해둔다.

왜 필요한가
-----------
SQLAlchemy 는 "지금까지 import 된 모델"만 알고 있다.
모델 하나만 따로 import 하면, 그 모델의 FK 가 가리키는 테이블을 몰라서
이런 에러가 난다.

    NoReferencedTableError: Foreign key associated with column
    'meal_histories.user_id' could not find table 'users'

이 파일이 다 불러오므로 아래처럼 쓰면 그런 일이 없다.

    from app.models import MealHistory, User
"""

from app.models.meal_history import MealHistory
from app.models.recipe_review import RecipeReview
from app.models.review_comment import ReviewComment
from app.models.review_like import ReviewLike
from app.models.user import User
from app.models.user_ban import UserBan
from app.models.verification_code import VerificationCode

# from app.models import * 를 썼을 때 넘어갈 이름들
__all__ = [
    "MealHistory",
    "RecipeReview",
    "ReviewComment",
    "ReviewLike",
    "User",
    "UserBan",
    "VerificationCode",
]
