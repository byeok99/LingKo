# 데이터 모델

## 핵심 관계

```mermaid
 erDiagram
    USERS ||--o{ EVALUATION_LOG : records
    USERS ||--o{ DAILY_PRACTICE_QUOTA : owns
    EVALUATION_LOG ||--o{ EVALUATION_SYLLABLE : contains
    SYLLABLES ||--o{ EVALUATION_SYLLABLE : references

    USERS {
      bigint user_idx PK
      varchar social_id
      varchar social_type
      varchar email
      varchar name
      varchar profile_image_url
      varchar display_language
      varchar native_language
      varchar target_level
      datetime created_at
      datetime last_login_at
    }

    EVALUATION_LOG {
      bigint evaluation_log_idx PK
      bigint user_idx FK
      varchar original_word
      int score
      varchar source
      bigint sentence_id
      varchar standard_pronunciation
      varchar recognized_text
      decimal accuracy_score
      decimal fluency_score
      decimal completeness_score
      decimal pronunciation_score
      varchar audio_url
      datetime created_at
    }

    EVALUATION_SYLLABLE {
      bigint evaluation_syllables_idx PK
      bigint evaluation_log_idx FK
      varchar syllable_char FK
      int score
      int position_no
      varchar feedback
      varchar mouth_guide_url
      varchar tongue_guide_url
    }

    SYLLABLES {
      varchar syllable_char PK
      varchar mouth_url
      varchar tongue_url
    }

    DAILY_PRACTICE_QUOTA {
      bigint daily_practice_quota_id PK
      bigint user_idx FK
      date quota_date
      int free_limit
      int free_used
      int rewarded_available
      datetime created_at
      datetime updated_at
    }
```

## 주요 제약

- `users`: `(social_id, social_type)` 유일
- `evaluation_log`: `(user_idx, created_at)` 조회 인덱스
- `evaluation_syllable`: `(evaluation_log_idx, position_no)` 유일
- `daily_practice_quota`: `(user_idx, quota_date)` 유일
- `daily_practice_quota.quota_date` 인덱스

## 데이터 소유권

| 데이터 | 소유자 | 삭제 기준 |
|---|---|---|
| 사용자 프로필 | 사용자 | 회원 탈퇴 정책 필요 |
| 학습 설정 | 사용자 | 사용자와 함께 삭제 |
| 평가 기록 | 사용자 | 사용자 삭제·보존 정책 필요 |
| 음성 URL | 사용자 평가 | 실제 S3 객체와 동기 삭제 필요 |
| 음절 가이드 | 서비스 공용 | 콘텐츠 관리 정책 적용 |
| 일일 쿼터 | 사용자·날짜 | 운영상 보존 기간 결정 필요 |

## 주의사항

- 엔티티와 실제 Flyway 스키마가 항상 일치하도록 테스트합니다.
- `sentence_id`는 추천 문장 참조지만 현재 엔티티 연관관계가 아닌 값으로 저장됩니다.
- 가이드 생성 작업은 DB 모델이 없으며 현재 메모리에만 저장됩니다.
- Refresh Token 저장 모델은 없습니다.
- 사용자 삭제 cascade 및 S3 객체 삭제 정책은 아직 명확하지 않습니다.
