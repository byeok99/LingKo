from dependency_injector import containers, providers

from lingko.common.gpt_service import GPTService
from lingko.conf.db import DatabaseSession
from lingko.domain.game.service import GameService


class _GameContainer(containers.DeclarativeContainer):
    db = providers.Dependency(instance_of=DatabaseSession)
    gpt_service = providers.Dependency(instance_of=GPTService)

    service = providers.Factory(
        GameService,
        gpt_service=gpt_service.provided,
        db=db.provided,
    )
