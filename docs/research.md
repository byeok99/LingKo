# LingKo 프로젝트 분석

## 개요

이 프로젝트의 원래 목표는 "한국어를 배우고 싶어하는 외국인 대상 글로벌 학습 앱"이다. 단순 번역 앱이 아니라, 학습자가 한국어 문장을 보고, 듣고, 따라 말하고, 발음 평가와 조음 가이드를 함께 받는 발음 중심 학습 경험을 제공하는 것이 핵심이다.

예상 사용자 경험은 대략 다음과 같다.

1. 사용자가 직접 학습할 문장을 입력하거나 추천 문장/코스에서 선택
2. 시스템이 해당 문장의 표준 발음을 생성
3. 문장 음성 발음을 제공
4. 발음 규칙을 반영한 발음 가이드를 제공
5. 사용자가 자신의 발음을 녹음
6. 시스템이 객관적인 발음 점수를 산출
7. 각 글자에 대해 입/혀 모양 가이드 영상을 제공

예를 들어 `"맛있겠다."` 같은 입력은 학습용 표준 발음 `"마싯게따."`처럼 변환되어야 한다. 즉 이 프로젝트의 본질은 "텍스트 -> 학습용 발음 표현 -> 음성/시각 가이드 -> 사용자 발화 평가"를 하나의 학습 흐름으로 묶는 것이다.

이 저장소는 현재 `lingko/` 하위에 있는 Spring Boot 3.4 / Java 17 기반 백엔드가 사실상 전부다. 기능적으로는 "한국어 문장을 표준 발음으로 변환"하는 최소 API가 이미 노출되어 있고, 그 뒤에 붙을 발음 평가(Azure Speech), viseme 추출, 프레임 보간(Replicate), 비디오 병합(FFmpeg), 결과 업로드(S3) 파이프라인이 코드 수준으로는 상당 부분 구현되어 있다.

다만 현재 상태는 "작동 중인 제품"보다는 "핵심 알고리즘과 멀티미디어 파이프라인을 실험적으로 쌓아 둔 초기 백엔드"에 가깝다. 공개 API는 매우 얇고, 서비스 계층은 단순하며, DB/JPA/인증 관련 코드는 준비만 된 상태다. 반대로 설정, 테스트, 일부 유틸 클래스 사이에는 버전 차이와 설계 불일치가 남아 있다.

## 저장소 구조

루트에는 실질 코드가 거의 없고, 실제 애플리케이션은 `lingko/`에 모여 있다.

```text
LingKo/
├── docs/                       # 이 문서 위치
└── lingko/
    ├── build.gradle
    ├── Dockerfile
    ├── docker-compose.yml
    ├── application.example.yaml
    └── src/
        ├── main/java/com/lingko/lingko
        │   ├── api/evaluation
        │   ├── core/config
        │   ├── core/domain/evaluation
        │   ├── core/domain/user
        │   ├── core/util
        │   ├── infra/pronunciation
        │   └── infra/storage
        ├── main/resources
        │   ├── application.yaml
        │   └── syllable_mapping.json
        └── test/java
```

구조는 대략 다음 세 층으로 나뉜다.

- `api`: 외부 HTTP 진입점
- `core`: 도메인 타입, 설정, 순수 로직
- `infra`: 외부 서비스 연동(Azure, Replicate, S3, FFmpeg)

## 기술 스택과 의존성

`build.gradle` 기준 주요 의존성은 다음과 같다.

- Spring Boot Web, Validation
- Spring Data JPA
- MySQL Connector
- Spring WebFlux `WebClient`
- Azure Cognitive Services Speech SDK
- AWS SDK v2 S3
- H2(test)

흥미로운 점은 HTTP 서버는 MVC(`spring-boot-starter-web`)인데, 외부 API 호출은 `WebClient`를 사용한다는 점이다. 하지만 실제 구현은 reactive하지 않고 `.block()`과 `Thread.sleep()`를 사용해 동기식으로 돌고 있다. 즉 "기술 선택은 reactive"지만 "실행 모델은 blocking"이다.

## 현재 공개 기능

현재 컨트롤러는 하나뿐이다.

- `POST /api/pronunciation/convert`

요청 DTO:

- `StandardPronunciationRequest`
- `text` 필드 필수
- 길이 제한 1~30자

응답 DTO:

- `originalText`
- `standardPronunciation`

실제 흐름은 단순하다.

1. `EvaluationController`가 문장을 받는다.
2. `EvaluationService.convertToStandardPronunciation()`를 호출한다.
3. 내부에서 `KoreanPhonemeUtil.toPronunciation()`를 실행한다.
4. 변환 결과를 응답으로 반환한다.

즉 현재 운영 가능한 기능은 "문장 -> 표준 발음 문자열" 하나다. 나머지 발음 평가, 음절별 비디오 생성, 학습 로그 저장은 API까지 연결되어 있지 않다.

제품 목표와 비교하면 현재 구현 상태는 다음과 같이 볼 수 있다.

- 이미 구현된 축: 표준 발음 문자열 변환
- 부분 구현된 축: 발음 평가, viseme 추출, 입/혀 모양 비디오 생성
- 아직 거의 없는 축: 코스/추천 문장, 문장 음성 제공, 사용자 학습 세션, 결과 히스토리 UI/API

## 핵심 도메인 흐름

### 1. 표준 발음 변환

핵심 로직은 `KoreanPhonemeUtil`에 몰려 있다. 이 클래스는 한글 음절 분해/재조합과 발음 규칙 적용을 함께 담당한다.

내부 처리 흐름:

1. 한글 문자를 초성/중성/종성으로 분해
2. 문자 단위로 발음 규칙 적용
3. 다시 한글로 재조합

코드상 반영된 규칙:

- 연음화
- 비음화
- 유음화
- 경음화
- 구개음화
- 격음화
- 종성 7음 규칙 일부

이 유틸은 순수 로직이라 API와 독립적으로 테스트하기 좋고, 현재 프로젝트에서 가장 응집도 높은 코드다. `KoreanPhonemeUtilTest`와 `EvaluationServiceTest`도 이 축을 검증한다.

### 2. 자모 -> 학습 이미지 매핑

`SyllableMappingUtil`은 `src/main/resources/syllable_mapping.json`을 읽어 자모별 입/혀 이미지 파일명을 메모리에 적재한다.

이 클래스가 담당하는 것:

- JSON 로딩
- 자모별 매핑 조회
- `VideoType.MOUTH` / `VideoType.TONGUE`에 맞는 S3 URL 생성
- 음소 시퀀스를 연속 프레임 쌍으로 변환

특징:

- 실제 정적 리소스는 저장소 내부가 아니라 S3 `guides/` 경로를 기준으로 사용한다.
- `createFramePairs()`는 `["img1", "img2", "img3"]`를 `[[img1,img2], [img2,img3]]`로 변환한다.
- 이미지가 1개뿐이면 `[[img1]]` 형태로 반환해 "정적 이미지" 케이스를 별도 처리하게 만든다.

이 설계는 사용자가 임의의 문장을 학습할 때도 자산을 재사용할 수 있게 하려는 의도가 분명하다. 완성형 음절마다 영상을 모두 저장하는 대신, 초성/중성/종성 단위로 조음 이미지를 쪼개고 이를 다시 조립해서 가이드를 만드는 구조다. 사용자가 `"맛있겠다."`를 학습하든 `"한국어는 재미있어요"`를 학습하든, 이미 만들어 둔 자모 단위 조음 자산을 최대한 재사용할 수 있다.

### 3. 비디오 생성 파이프라인

멀티미디어 파이프라인은 다음 순서로 설계되어 있다.

1. 자모별 이미지 URL 확보
2. 프레임 쌍 생성
3. 각 쌍을 Replicate Frame Interpolation으로 동영상화
4. 여러 세그먼트를 FFmpeg로 병합
5. 최종 결과를 S3에 업로드

구성 요소:

- `VideoGenerator`: 포트 인터페이스
- `FrameInterpolationVideoGenerator`: 오케스트레이션 구현체
- `ReplicateApiClient`: Replicate prediction 생성/폴링
- `VideoMerger`: FFmpeg concat 병합
- `S3Uploader`: 결과 업로드

`FrameInterpolationVideoGenerator.generate()`는 사실상 이 파이프라인의 애플리케이션 서비스 역할을 한다.

세부 분기:

- 프레임이 1개면 비디오를 만들지 않고 이미지를 그대로 S3에 업로드
- 프레임 쌍이 여러 개면 세그먼트를 각각 생성 후 병합
- 모든 중간 산출물은 임시 파일로 받고 마지막에 정리

### 4. 음성 평가 파이프라인

`SpeechEvaluator` 인터페이스와 `AzureSpeechEvaluator` 구현체가 있다.

흐름:

1. WAV 파일 경로와 기준 문장을 입력
2. Azure Speech Pronunciation Assessment 실행
3. 정확도/유창성/완성도/종합 점수와 인식 텍스트 반환

반환 모델:

- `AssessmentResult`

이 기능은 구현 자체는 되어 있지만 현재 컨트롤러나 상위 서비스에 연결되지 않았다.

하지만 제품 목표와의 연결은 명확하다. 이 컴포넌트는 "외국인 학습자가 자신의 발음을 녹음하고, 시스템이 객관적인 발음 점수를 준다"는 요구를 직접 담당하는 축이다. 따라서 장기적으로는 단순 점수 반환을 넘어서 다음 정보까지 확장될 가능성이 높다.

- 문장 전체 점수
- 글자 또는 음절 단위 세부 점수
- 목표 발음과 실제 발음 차이
- 어떤 음운 규칙에서 틀렸는지에 대한 피드백

## 제품 관점에서 본 현재 아키텍처

현재 코드베이스를 제품 목표에 대응시키면 다음처럼 읽힌다.

- 입력 경험
  현재는 자유 텍스트 입력만 일부 반영되어 있다.
- 추천 문장/코스
  아직 코드상 모델이나 API가 없다.
- 표준 발음 변환
  현재 가장 완성도가 높다.
- TTS 또는 원어민 발음 제공
  아직 구현 흔적이 약하다.
- 사용자 녹음 평가
  Azure 기반 평가 엔진이 구현돼 있으나 API로 노출되진 않았다.
- 입/혀 모양 영상 가이드
  자모 재사용 기반 파이프라인이 구현돼 있다.
- 학습 이력/성과 추적
  엔티티는 있지만 실제 유스케이스는 연결되지 않았다.

즉 이 프로젝트는 제품 로드맵 기준으로 보면 "발음 엔진과 조음 가이드 엔진을 먼저 만들고, 그 위에 학습 경험을 얹으려던 구조"에 가깝다.

## 데이터 모델

JPA 엔티티는 세 개의 평가 도메인 엔티티와 하나의 사용자 엔티티가 있다.

### `User`

- 소셜 로그인 사용자 모델
- `social_id + social_type` 유니크 제약
- `GOOGLE`, `APPLE`, `KAKAO` 지원

### `EvaluationLog`

- 사용자별 발음 평가 이력
- 원본 단어, 점수, 생성 시각 저장
- `EvaluationSyllable`과 1:N 관계

### `EvaluationSyllable`

- 평가 로그와 개별 음절 연결
- 각 음절별 점수 저장

### `Syllable`

- 음절 문자와 입/혀 URL 저장용 엔티티

해석:

- 장기적으로는 "사용자 발음 평가 결과를 음절 단위까지 세분화해 저장"하려는 모델로 보인다.
- 하지만 현재 런타임 로직은 JSON 기반 `SyllableMappingUtil`을 쓰고 있으며, `SyllableRepository`는 실제 흐름에 연결돼 있지 않다.
- 즉 DB 스키마는 미래 확장을 위한 설계 흔적이고, 현 기능은 리소스 파일 중심이다.

## 설정 구조

설정 클래스는 다음이 존재한다.

- `AzureSettings`
- `AwsSettings`
- `DBSettings`
- `FfmpegSettings`
- `GoogleSettings`
- `JwtSettings`
- `ReplicateSettings`

보조 설정:

- `WebClientConfig`
- `S3Config`

### 중요한 관찰

설정 클래스와 실제 `src/main/resources/application.yaml` 사이에는 구조 차이가 있다.

1. `AwsSettings`는 `aws.s3.bucket`, `aws.s3.region`, `aws.credentials.access-key`, `aws.credentials.secret-key` 구조를 기대한다.
2. 실제 `application.yaml`은 flat key 형태로 작성돼 있다.
3. 따라서 현재 `S3Config`가 기대하는 값이 정상 바인딩되지 않을 가능성이 높다.

또 다른 불일치도 있다.

- `ReplicateSettings`는 `replicate.*`를 기대한다.
- 실제 `application.yaml`에는 `replicate` 블록이 없고 `azure.replicate_key`처럼 보이는 값만 존재한다.
- `application.example.yaml`에는 오히려 올바른 `replicate` 구조가 정의돼 있다.

`DBSettings`도 미완성이다.

- `application.example.yaml`에는 `db.name`이 존재한다.
- `DBSettings`에는 `name` 필드가 없다.
- 반대로 `DBSettings`에는 `username`이 있지만 예제 파일은 `user`를 쓴다.

즉 예제 설정, 실제 설정, 자바 설정 모델이 서로 다른 시점의 설계를 반영하고 있다.

## 컨테이너/실행 환경

`Dockerfile`은 2-stage 빌드다.

1. Gradle 이미지에서 `bootJar`
2. JRE 이미지에서 실행
3. 런타임에 `ffmpeg` 패키지 설치

`docker-compose.yml`은 두 서비스를 띄운다.

- `mysql`
- `backend`

관찰 포인트:

- `backend`는 `.env` 값을 받아 MySQL 접속과 FFmpeg 경로를 넘긴다.
- 하지만 Spring 표준 `spring.datasource.*` 환경 변수가 아니라 커스텀 `DB_*`를 사용한다.
- 코드상 `DBSettings`는 존재하지만, 실제 DataSource를 생성하는 커스텀 설정은 보이지 않는다.
- 즉 Compose는 인프라를 올리지만, 애플리케이션이 이 값을 JPA 연결에 실제 사용한다는 보장은 현재 코드만으로는 없다.

## 테스트 상태

테스트는 여러 층으로 나뉜다.

- 순수 로직 테스트
- 설정 로딩 테스트
- 외부 API 통합 테스트
- 데모/탐색성 테스트

### 확인한 점

`./gradlew test`를 실행했지만 성공하지 못했다.

실패 원인:

- `Visemeextractiondemotest.java`
- `Syllablemappingloadertest.java`

이 두 테스트는 현재 존재하지 않는 과거 클래스/패키지를 참조한다.

- `com.lingko.lingko.core.domain.pronunciation.service.VisemeExtractor`
- `com.lingko.lingko.core.domain.pronunciation.service.SyllableMappingLoader`

즉 최근 구조 변경 이후 테스트가 업데이트되지 않았다. 현재 코드베이스는 적어도 테스트 관점에서 "리팩터링 도중 멈춘 상태"다.

추가로 테스트 내용 자체도 현재 구현과 어긋난 부분이 있다.

- 일부 테스트는 bare filename(`ㅏ.png`)을 기대
- 현재 `SyllableMappingUtil`은 full S3 URL을 조합
- 클래스명/파일명 규칙도 일관되지 않음

결론적으로 테스트 스위트는 신뢰 가능한 회귀 방어선이 아니다.

## 설계상 불일치와 주요 리스크

### 1. 설정/비밀값 관리가 위험함

`src/main/resources/application.yaml`에 실제 서비스 비밀값으로 보이는 민감 정보가 직접 들어 있다.

- OpenAI API key
- Azure key
- Google OAuth client secret
- AWS access key
- DB 비밀번호

이 상태는 즉시 수정 대상이다. 값 폐기와 재발급이 필요하고, 저장소에는 placeholder만 남겨야 한다.

### 2. 설정 모델과 실제 설정 파일이 맞지 않음

앞서 본 것처럼 AWS/Replicate/DB 설정 구조가 서로 다르다. 이 상태면 환경에 따라 다음 문제가 생길 수 있다.

- S3 client 초기화 실패
- Replicate 설정 null
- DB 연결 미설정

즉 "코드는 있어도 배포 시점에 깨질 가능성"이 높다.

### 3. 현재 API와 나머지 도메인 구현이 분리돼 있음

공개 API는 발음 문자열 변환만 제공한다. 반면 다음 코드는 상위 유스케이스에 연결되지 않았다.

- `AzureSpeechEvaluator`
- `FrameInterpolationVideoGenerator`
- JPA 리포지토리들

이 말은 현재 구조가 "레이어드 아키텍처"처럼 보이지만, 실제로는 여러 독립 실험 컴포넌트가 한 저장소에 공존하는 상태라는 뜻이다.

### 4. viseme 관련 구현이 두 갈래로 갈라져 있음

프로젝트에는 viseme 관련 접근이 둘 있다.

- `SyllableMappingUtil.createFramePairs()`
- `VisemeExtractorUtil`

그런데 `VisemeExtractorUtil`은 이름과 주석상 "URL"을 다룬다고 하지만 실제 반환값은 `SyllableMappingUtil.SyllableMapping`의 raw filename이다. 반면 `SyllableMappingUtil.getImageUrl()`은 full S3 URL을 반환한다.

즉 두 유틸의 출력 계약이 다르다.

또한 `VisemeExtractorUtil`은 `ㅇ`, `ㅎ`를 묵음처럼 제외하는데, 다른 경로에서는 종성 `ㅇ`을 포함한 프레임쌍 생성을 가정하는 테스트가 있다. 이 역시 음운 규칙과 시각화 규칙이 한 기준으로 통합되지 않았다는 신호다.

### 5. 동기식 장시간 작업이 요청 처리 스레드를 점유할 가능성

비디오 생성 파이프라인은 현재 전부 동기식이다.

- `WebClient.block()`
- `Thread.sleep()` 폴링
- URL 다운로드
- FFmpeg 프로세스 대기
- S3 업로드

이 흐름이 HTTP 요청 안에서 직접 실행되면 응답 시간이 길고 스레드 점유가 심해진다. 비디오 생성은 비동기 작업 큐나 배치 잡으로 분리하는 쪽이 자연스럽다.

### 6. 엔티티 헬퍼 메서드가 불완전함

`EvaluationLog.addSyllable()`는 리스트에 추가하지 않고 역참조만 세팅한다.

```java
public void addSyllable(EvaluationSyllable syllable) {
    syllable.setEvaluationLog(this);
}
```

이 메서드는 이름상 컬렉션 유지까지 해줄 것처럼 보이지만 실제로는 `syllableList`에 넣지 않는다. 양방향 연관관계를 안전하게 유지하는 헬퍼로는 불완전하다.

### 7. 로그에 민감 정보 일부가 노출됨

`ReplicateApiClient`는 API key 앞 5자리를 로그로 남긴다. 전체 키는 아니지만 운영 로그에 인증 정보 일부를 흘리는 습관은 피하는 편이 낫다.

## 실제로 강한 부분

문제점이 많아 보여도, 전부가 부실한 것은 아니다. 특히 아래는 재사용 가치가 높다.

- `KoreanPhonemeUtil`: 한글 분해/재조합과 발음 규칙 구현이 깔끔함
- `SyllableMappingUtil`: 자모 매핑 로딩과 프레임쌍 생성 책임이 명확함
- `FrameInterpolationVideoGenerator`: static image / single segment / multi segment 분기가 실용적임
- `VideoGenerationException`: 멀티미디어 파이프라인 에러를 한 타입으로 정리함

즉 "핵심 알고리즘과 미디어 파이프라인의 뼈대"는 이미 있고, 문제는 이들을 제품 레벨의 유스케이스와 운영 가능 설정으로 묶는 부분이다.

## 추천 정비 순서

### 1단계: 운영 리스크 제거

- 저장소에서 민감 정보 제거
- 노출된 키 전면 폐기/재발급
- `application.yaml` 대신 `application.example.yaml`만 추적
- 실제 실행값은 `.env` 또는 secret manager로 이동

### 2단계: 설정 구조 통합

- `application.yaml`, `application.example.yaml`, `@ConfigurationProperties` 필드명 일치
- AWS 설정을 nested 구조로 통일
- Replicate 설정 위치 정리
- DB 설정이 실제 DataSource/JPA와 연결되도록 명확화

### 3단계: 테스트 복구

- 존재하지 않는 레거시 테스트 제거 또는 현재 구조로 마이그레이션
- 외부 연동 테스트와 순수 단위 테스트 분리
- 네트워크가 필요한 테스트는 profile/tag로 분리

### 4단계: 유스케이스 재조립

다음과 같은 상위 서비스 하나로 묶는 것이 자연스럽다.

- 입력 문장 수신
- 표준 발음 변환
- 음소/음절 분해
- viseme 리소스 생성
- 필요 시 Azure 평가
- 결과 저장

현재 `EvaluationService`는 이름에 비해 책임이 너무 작으므로, 실제 애플리케이션 서비스로 확장하거나 이름을 `PronunciationService` 수준으로 줄이는 편이 낫다.

### 5단계: 장시간 작업 비동기화

- 비디오 생성은 요청-응답에서 분리
- job status polling 또는 callback 구조 도입
- 생성 결과를 DB/S3에 저장 후 조회 API 제공

## 결론

이 프로젝트는 "외국인 대상 한국어 발음 학습 앱"의 핵심 실험 코드가 이미 들어 있는 저장소다. 현재 완성된 기능은 표준 발음 문자열 변환뿐이지만, 코드베이스 안에는 Azure 평가, viseme 추출, 프레임 보간, FFmpeg 병합, S3 업로드까지 이어지는 확장 경로가 분명히 존재한다.

반면 제품화 관점에서는 아직 해결해야 할 문제가 분명하다.

- 설정 구조 불일치
- 민감 정보 커밋
- 테스트 붕괴
- 상위 유스케이스 부재
- 동기식 장시간 처리

따라서 이 코드는 "버릴 상태"가 아니라 "재조립이 필요한 상태"라고 보는 게 맞다. 특히 자모 단위 자산 재사용, 표준 발음 변환, 발음 평가 파이프라인이라는 세 축은 제품 아이디어와 잘 맞는다. 우선순위는 새 기능을 넓게 붙이기보다, 설정/보안/테스트를 먼저 정리하고 그 위에 "문장 선택 -> 발음 제공 -> 녹음 평가 -> 조음 가이드"라는 완결된 학습 플로우를 API 단위로 재구성하는 데 있다.
