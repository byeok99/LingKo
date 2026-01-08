from lingko.common.gpt_service import GPTService
from lingko.common.jwt_service import JWTService
from lingko.di import DI
from lingko.domain.auth.service import AuthService
from lingko.domain.game.service import GameService
from lingko.domain.malbeot.service import MalbeotService
from lingko.domain.mypage.service import MyPageService
from lingko.domain.recommendation.service import RecommendationService
from lingko.domain.speech.service import SpeechService


def get_auth_service_from_di() -> AuthService:
    return DI.auth.service()


def get_jwt_service_from_di() -> JWTService:
    return DI.jwt_service()


def get_gpt_service_from_di() -> GPTService:
    return DI.gpt_service()


def get_malbeot_service_from_di() -> MalbeotService:
    return DI.malbeot.service()


def get_speech_service_from_di() -> SpeechService:
    return DI.speech.service()


def get_mypage_service_from_di() -> MyPageService:
    return DI.mypage.service()


def get_game_service_from_di() -> GameService:
    return DI.game.service()


def get_recommendation_service_from_di() -> RecommendationService:
    return DI.recommendation.service()
