from dependency_injector import containers, providers

from lingko.conf.db import DatabaseSession
from lingko.domain.mypage.service import MyPageService
from lingko.domain.recommendation.service import RecommendationService


class _MyPageContainer(containers.DeclarativeContainer):
    db = providers.Dependency(instance_of=DatabaseSession)

    recommendation_service = providers.Dependency(instance_of=RecommendationService)
    service = providers.Factory(
        MyPageService,
        db=db.provided,
        recommendation_service=recommendation_service,
    )
