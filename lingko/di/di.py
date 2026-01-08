from dependency_injector import containers, providers
from lingko.common.gpt_service import GPTService
from lingko.common.jwt_service import JWTService
from lingko.conf.db import DatabaseSession
from lingko.di.auth import _AuthContainer
from lingko.di.config import ConfigContainer
from lingko.di.game import _GameContainer
from lingko.di.malbeot import _MalbeotContainer
from lingko.di.mypage import _MyPageContainer
from lingko.di.recommendation import _RecommendationContainer
from lingko.di.speech import _SpeechContainer


class DI(containers.DeclarativeContainer):
    config = providers.Container(ConfigContainer).config

    db = providers.Singleton(
        DatabaseSession,
        config=config.db,
    )

    jwt_service = providers.Singleton(
        JWTService,
        config=config.jwt,
    )

    gpt_service = providers.Singleton(
        GPTService,
        config=config.openai,
    )

    auth = providers.Container(
        _AuthContainer,
        config=config.google,
        db=db,
    )

    speech = providers.Container(
        _SpeechContainer,
        config=config.azure_speech,
        db=db,
        gpt_service=gpt_service,
    )

    recommendation = providers.Container(
        _RecommendationContainer,
        db=db,
        gpt_service=gpt_service,
        speech_service=speech.service,
    )

    mypage = providers.Container(
        _MyPageContainer,
        db=db,
        recommendation_service=recommendation.service
    )

    malbeot = providers.Container(
        _MalbeotContainer,
        config=config.openai,
        db=db,
        mypage_service=mypage.service,
    )

    game = providers.Container(
        _GameContainer,
        db=db,
        gpt_service=gpt_service,
    )


__all__ = (
    'DI',
)
