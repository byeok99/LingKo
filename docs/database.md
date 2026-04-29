# LingKo Database Design

## 목적

이 문서는 LingKo 구현을 시작할 때 참고할 데이터베이스 설계 초안이다. 현재 코드에 이미 존재하는 `User`, `EvaluationLog`, `EvaluationSyllable`, `Syllable` 엔티티를 기반으로 하되, 최신 제품 기획인 글로벌 발음 교정 앱, 필수 OAuth 로그인, 다국어 콘텐츠, 코스, 글자 단위 피드백, 광고 기반 일일 연습 제한을 반영한다.

현재 코드의 평가 엔티티는 `evaluation_*` 이름을 쓰지만, 제품 관점에서는 "연습 세션"이 더 정확하다. 신규 구현에서는 `practice_*` 명명을 권장한다.

## 설계 원칙

- 모든 학습 기록은 로그인 사용자 기준으로 저장한다.
- 학습 대상 언어는 한국어로 고정하고, UI/번역/설명/피드백은 `locale` 기준으로 다국어 제공한다.
- 추천 문장과 코스는 운영자가 관리하는 콘텐츠로 본다.
- 자유 입력 문장은 콘텐츠 테이블에 반드시 저장하지 않고, 연습 세션에 원문과 표준 발음을 스냅샷으로 남긴다.
- 사용자에게 보이는 조음 가이드는 한 글자 단위다.
- 내부적으로는 초성-중성-종성 요소를 조합해 가이드 자산을 재사용할 수 있다.
- 하루 무료 연습 5회와 광고 보상 연습권은 서버에서 검증한다.

## 명명 규칙

- 테이블명은 `snake_case` 복수형보다 도메인 의미가 명확한 단수/복합명을 사용한다.
- PK는 `*_id` 형식을 권장한다.
- 날짜/시간은 `created_at`, `updated_at`, `last_login_at`처럼 명명한다.
- 외래키는 참조 테이블의 PK 이름을 그대로 사용한다.
- 다국어 테이블은 원본 테이블명 뒤에 `_translation`을 붙인다.

## 테이블 개요

### users

OAuth 기반 사용자 계정이다. 현재 코드의 `users` 엔티티를 확장한다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| user_id | BIGINT PK | N | 사용자 ID |
| social_id | VARCHAR(255) | N | OAuth provider user id |
| social_type | VARCHAR(30) | N | `GOOGLE`, `APPLE` |
| email | VARCHAR(255) | Y | 이메일 |
| name | VARCHAR(100) | Y | 표시 이름 |
| profile_image_url | VARCHAR(500) | Y | 프로필 이미지 |
| locale | VARCHAR(20) | N | 앱 표시 언어. 예: `en`, `ja`, `zh-CN` |
| native_language | VARCHAR(20) | Y | 사용자 모국어 |
| target_level | VARCHAR(30) | Y | 학습 레벨. 예: `BEGINNER_1` |
| created_at | DATETIME | N | 가입 시각 |
| updated_at | DATETIME | N | 수정 시각 |
| last_login_at | DATETIME | Y | 마지막 로그인 |

Constraints:

- Unique: `(social_type, social_id)`
- Index: `(locale)`

Notes:

- 현재 코드에는 `user_idx`가 PK다. 신규 설계에서는 `user_id`를 권장하지만, 기존 코드 유지 비용이 크면 `user_idx`를 유지해도 된다.
- `KAKAO`는 현재 enum에 있지만 글로벌 MVP 인증 수단에서는 제외한다.

### auth_refresh_token

JWT refresh token 저장용이다. 토큰 회전과 로그아웃 처리를 위해 둔다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| refresh_token_id | BIGINT PK | N | 토큰 ID |
| user_id | BIGINT FK | N | 사용자 ID |
| token_hash | VARCHAR(255) | N | refresh token hash |
| expires_at | DATETIME | N | 만료 시각 |
| revoked_at | DATETIME | Y | 폐기 시각 |
| created_at | DATETIME | N | 생성 시각 |

Indexes:

- `(user_id)`
- `(expires_at)`

### sentence

추천 문장 또는 코스 문장의 원본 한국어 콘텐츠다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| sentence_id | BIGINT PK | N | 문장 ID |
| text | VARCHAR(300) | N | 한국어 원문 |
| standard_pronunciation | VARCHAR(300) | N | 표준 발음 표기 |
| difficulty | VARCHAR(30) | N | 난이도 |
| category | VARCHAR(50) | Y | 카테고리 |
| learning_point | VARCHAR(100) | Y | 내부용 학습 포인트 |
| audio_url | VARCHAR(500) | Y | 추천 문장 사전 생성 오디오 URL |
| active | BOOLEAN | N | 노출 여부 |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Indexes:

- `(difficulty)`
- `(category)`
- `(active)`

Notes:

- 자유 입력 문장은 이 테이블에 저장하지 않는 것을 기본으로 한다.
- 추천 문장으로 승격할 콘텐츠만 저장한다.

### sentence_translation

문장의 다국어 번역과 발음 설명이다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| sentence_translation_id | BIGINT PK | N | 번역 ID |
| sentence_id | BIGINT FK | N | 문장 ID |
| locale | VARCHAR(20) | N | 번역 언어 |
| translated_text | VARCHAR(500) | N | 번역문 |
| learning_point_title | VARCHAR(100) | Y | 학습 포인트 제목 |
| learning_point_description | TEXT | Y | 발음 설명 |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Constraints:

- Unique: `(sentence_id, locale)`

Indexes:

- `(locale)`

### course

코스 원본 메타데이터다. 제목/설명은 번역 테이블에 둔다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| course_id | BIGINT PK | N | 코스 ID |
| level | VARCHAR(30) | N | 코스 레벨 |
| active | BOOLEAN | N | 노출 여부 |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Indexes:

- `(level)`
- `(active)`

### course_translation

코스 제목과 설명의 다국어 버전이다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| course_translation_id | BIGINT PK | N | 번역 ID |
| course_id | BIGINT FK | N | 코스 ID |
| locale | VARCHAR(20) | N | 표시 언어 |
| title | VARCHAR(100) | N | 코스 제목 |
| description | TEXT | Y | 코스 설명 |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Constraints:

- Unique: `(course_id, locale)`

### course_sentence

코스와 문장의 순서 관계다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| course_sentence_id | BIGINT PK | N | 관계 ID |
| course_id | BIGINT FK | N | 코스 ID |
| sentence_id | BIGINT FK | N | 문장 ID |
| order_no | INT | N | 코스 내 순서 |

Constraints:

- Unique: `(course_id, sentence_id)`
- Unique: `(course_id, order_no)`

Indexes:

- `(sentence_id)`

### user_course_progress

사용자의 코스 진행 상태다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| user_course_progress_id | BIGINT PK | N | 진행 ID |
| user_id | BIGINT FK | N | 사용자 ID |
| course_id | BIGINT FK | N | 코스 ID |
| completed_sentence_count | INT | N | 완료 문장 수 |
| last_sentence_id | BIGINT FK | Y | 마지막 학습 문장 |
| completed_at | DATETIME | Y | 완료 시각 |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Constraints:

- Unique: `(user_id, course_id)`

### practice_session

사용자 1회의 발음 연습 결과다. 추천 문장과 자유 입력을 모두 지원한다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| practice_session_id | BIGINT PK | N | 연습 세션 ID |
| user_id | BIGINT FK | N | 사용자 ID |
| sentence_id | BIGINT FK | Y | 추천 문장 ID. 자유 입력이면 null |
| original_text | VARCHAR(300) | N | 사용자가 학습한 원문 스냅샷 |
| standard_pronunciation | VARCHAR(300) | N | 표준 발음 스냅샷 |
| recognized_text | VARCHAR(300) | Y | 음성 인식 결과 |
| audio_url | VARCHAR(500) | Y | 사용자 녹음 파일 URL |
| overall_score | DECIMAL(5,2) | Y | 종합 점수 |
| accuracy_score | DECIMAL(5,2) | Y | 정확도 |
| fluency_score | DECIMAL(5,2) | Y | 유창성 |
| completeness_score | DECIMAL(5,2) | Y | 완성도 |
| quota_source | VARCHAR(30) | N | `FREE_DAILY`, `AD_REWARD`, `ADMIN` |
| created_at | DATETIME | N | 생성 시각 |

Indexes:

- `(user_id, created_at)`
- `(sentence_id)`

Notes:

- 기존 `evaluation_log`는 이 테이블로 대체하는 것을 권장한다.
- Azure 평가 원본 JSON을 보관하고 싶다면 별도 `assessment_raw_json` 컬럼 또는 별도 로그 테이블을 둔다.

### practice_character_result

글자 단위 발음 피드백과 조음 가이드 결과다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| practice_character_result_id | BIGINT PK | N | 글자 결과 ID |
| practice_session_id | BIGINT FK | N | 연습 세션 ID |
| character_text | VARCHAR(10) | N | 대상 글자. 예: `싯` 또는 `게` |
| position_no | INT | N | 문장 내 순서 |
| score | DECIMAL(5,2) | Y | 글자별 점수 |
| feedback_code | VARCHAR(50) | Y | 로컬라이즈용 피드백 코드 |
| mouth_guide_url | VARCHAR(500) | Y | 입 모양 가이드 URL |
| tongue_guide_url | VARCHAR(500) | Y | 혀 모양 가이드 URL |

Constraints:

- Unique: `(practice_session_id, position_no)`

Notes:

- 사용자에게 보여주는 단위는 글자다.
- `feedback_code`를 두면 피드백 문장을 locale별로 변환하기 쉽다.

### pronunciation_guide_asset

한 글자 가이드를 생성하거나 재사용하기 위한 자산 매핑이다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| guide_asset_id | BIGINT PK | N | 자산 ID |
| guide_key | VARCHAR(100) | N | 조합 키. 예: 초성+중성+종성 |
| character_text | VARCHAR(10) | Y | 대표 글자 |
| mouth_guide_url | VARCHAR(500) | Y | 입 모양 가이드 |
| tongue_guide_url | VARCHAR(500) | Y | 혀 모양 가이드 |
| source_type | VARCHAR(30) | N | `STATIC`, `GENERATED` |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Constraints:

- Unique: `(guide_key)`

Notes:

- 현재 코드의 `syllables` 또는 `syllable_mapping.json`은 이 테이블의 초기 데이터 소스로 볼 수 있다.
- 실제로는 글자 단위 결과를 제공하지만, 내부 키는 초성-중성-종성 조합으로 만들 수 있다.

### daily_practice_quota

사용자별 일일 무료 연습 사용량이다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| daily_practice_quota_id | BIGINT PK | N | 사용량 ID |
| user_id | BIGINT FK | N | 사용자 ID |
| quota_date | DATE | N | 기준 날짜 |
| free_limit | INT | N | 무료 제공 횟수. 기본 5 |
| free_used_count | INT | N | 무료 사용 횟수 |
| ad_reward_used_count | INT | N | 광고 보상 사용 횟수 |
| created_at | DATETIME | N | 생성 시각 |
| updated_at | DATETIME | N | 수정 시각 |

Constraints:

- Unique: `(user_id, quota_date)`

Indexes:

- `(quota_date)`

Notes:

- 무료 횟수 차감은 평가 결과 생성 성공 시점에 처리한다.
- 사용자 타임존 정책을 정해야 한다. MVP는 서버 기준 날짜 또는 사용자 locale 기반 날짜 중 하나로 고정해야 한다.

### ad_reward

보상형 광고 시청 검증과 지급 기록이다.

| Column | Type | Null | Description |
| --- | --- | --- | --- |
| ad_reward_id | BIGINT PK | N | 광고 보상 ID |
| user_id | BIGINT FK | N | 사용자 ID |
| provider | VARCHAR(50) | N | 광고 플랫폼 |
| provider_transaction_id | VARCHAR(255) | Y | 광고 SDK 검증 ID |
| reward_type | VARCHAR(30) | N | `PRACTICE_ATTEMPT` |
| reward_amount | INT | N | 지급 횟수 |
| used_amount | INT | N | 사용 횟수 |
| verification_status | VARCHAR(30) | N | `PENDING`, `VERIFIED`, `REJECTED`, `USED` |
| created_at | DATETIME | N | 생성 시각 |
| verified_at | DATETIME | Y | 검증 시각 |
| used_at | DATETIME | Y | 사용 시각 |

Indexes:

- `(user_id, created_at)`
- `(provider_transaction_id)`

## 관계 요약

- `users` 1:N `practice_session`
- `users` 1:N `daily_practice_quota`
- `users` 1:N `ad_reward`
- `users` 1:N `user_course_progress`
- `course` 1:N `course_translation`
- `course` 1:N `course_sentence`
- `sentence` 1:N `sentence_translation`
- `sentence` 1:N `course_sentence`
- `sentence` 1:N `practice_session`
- `practice_session` 1:N `practice_character_result`

## 기존 엔티티와의 매핑

| Current Entity | Current Table | Recommended Target |
| --- | --- | --- |
| `User` | `users` | `users` 확장 |
| `EvaluationLog` | `evaluation_log` | `practice_session` |
| `EvaluationSyllable` | `evaluation_syllable` | `practice_character_result` |
| `Syllable` | `syllables` | `pronunciation_guide_asset` 또는 초기 seed |

## 구현 순서 권장

1. `users`, `auth_refresh_token`부터 정리한다.
2. `sentence`, `sentence_translation`, `course`, `course_translation`, `course_sentence`를 만든다.
3. `practice_session`, `practice_character_result`를 만든다.
4. `daily_practice_quota`, `ad_reward`를 만든다.
5. 기존 `evaluation_*`, `syllables` 엔티티를 새 명명으로 마이그레이션하거나 호환 계층을 둔다.

## 남은 결정 사항

- PK 명명: 기존 `*_idx` 유지 또는 신규 `*_id`로 통일
- 일일 무료 횟수 초기화 기준: 서버 날짜 또는 사용자 타임존
- 광고 SDK provider: AdMob, Unity Ads 등
- 자유 입력 문장의 번역 지원 범위
- Azure 평가 결과에서 글자 단위 점수를 어떻게 안정적으로 추출할지
- 입/혀 가이드 자산을 DB에 둘지, JSON/S3 manifest로 둘지
