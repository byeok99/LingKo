from celery import Celery

from lingko.di import DI

container = DI()

# Redis URL
redis_url = config.get_redis_url()

celery_app = Celery(
    'lingko',
    broker=redis_url,
    backend=redis_url
)

# Celery 설정
celery_app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='Asia/Seoul',
    enable_utc=True,
    broker_connection_retry_on_startup=True,
    task_track_started=True,
    task_time_limit=30 * 60,
)

# 태스크 자동 발견
celery_app.autodiscover_tasks([
    'lingko.domain.speech',
    # 나중에 다른 도메인 추가 가능
    # 'lingko.domain.game',
    # 'lingko.domain.recommendation',
])
