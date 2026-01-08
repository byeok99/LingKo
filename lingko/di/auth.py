from dependency_injector import containers, providers

from lingko.conf.db import DatabaseSession
from lingko.domain.auth.service import AuthService


class _AuthContainer(containers.DeclarativeContainer):
    db = providers.Dependency(instance_of=DatabaseSession)
    config = providers.Dependency()

    service = providers.Factory(
        AuthService,
        config=config.provided,
        db=db.provided,
    )
