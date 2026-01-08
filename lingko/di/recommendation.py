from dependency_injector import containers, providers

from lingko.common.gpt_service import GPTService
from lingko.conf.db import DatabaseSession
from lingko.domain.recommendation.service import RecommendationService
from lingko.domain.speech.service import SpeechService


class _RecommendationContainer(containers.DeclarativeContainer):
    db = providers.Dependency(instance_of=DatabaseSession)
    gpt_service = providers.Dependency(instance_of=GPTService)
    speech_service = providers.Dependency(instance_of=SpeechService)

    service = providers.Factory(
        RecommendationService,
        gpt_service=gpt_service.provided,
        speech_service=speech_service.provided,
        db=db.provided,
    )
