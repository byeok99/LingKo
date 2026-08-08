# 작업 이력

## 2026-08-07 - 인증 갱신 경계에 법무 동의 요청 연결

- 변경 파일: `app_auth_service.dart`
- 내용: 동의 상태 조회·제출도 기존 401 refresh와 1회 재시도 경계를 사용하도록 연결했다.
- 검증: `flutter analyze`, `flutter test --coverage` 113개 통과(라인 81.26%)
- 리스크: 없음

## 2026-08-07 - 문서 열기 실패 원인을 개발 빌드에서 확인 가능하게 수정

- 변경 파일: `legal_document_launcher.dart`
- 내용: 실패를 bool로만 돌려주고 예외를 통째로 삼켜 원인을 알 수 없었다. 개발 빌드에서만 대상 URL과 가장 흔한 원인(Android `<queries>`, iOS `LSApplicationQueriesSchemes`)을 함께 남기도록 했다. 호출자에게 주는 계약(bool)은 바꾸지 않았다.
- 검증: `flutter analyze`, `flutter test` 106개 통과
- 리스크: 없음

## 2026-08-07 - 법무 문서 열기 서비스 추가

- 변경 파일: `legal_document_launcher.dart`
- 내용: 백엔드가 서빙하는 공개 URL을 외부 브라우저로 여는 경계를 추가했다. 앱 안에서 문서를 렌더링하지 않는 이유는 약관 개정 시 앱 업데이트를 기다리지 않고 최신본이 보여야 하기 때문이고, 같은 URL이 스토어 심사용 공개 주소로도 쓰인다. 브라우저가 없는 기기를 앱 오류로 다루지 않고 false를 돌려줘 호출자가 대체 안내를 하게 했다.
- 검증: `flutter analyze`, `flutter test --coverage` 106개 통과(라인 81.22%)
- 리스크: 문서 경로 구간이 백엔드 `LegalDocument`의 path와 문자열로만 맞춰져 있다. 어긋나면 404가 되므로 테스트로 고정했다

## 2026-08-04 - 마이크 입력 레벨 스트림 노출

- 변경 파일: `audio_recorder_service.dart`
- 내용: 화면이 실제 입력 레벨을 보여줄 수 있도록 amplitudeStream을 서비스 계약에 추가했다. dBFS 원값 대신 0~1 표시용 비율로 변환해 UI가 플러그인 단위에 결합되지 않게 했다.
- 검증: `flutter analyze`, `flutter test` 81개 통과
- 리스크: 촉각 피드백과 실제 마이크 레벨은 시뮬레이터가 아닌 실기기 확인이 필요함

## 2026-07-30 - 한국어 문장 TTS 서비스 추가

- 변경 파일: `sentence_speech_service.dart`, `WORK_LOG.md`
- 내용: `ko-KR` 기기 TTS 초기화, Normal·Slow 속도, 중복 발화 방지와 재시도 가능한 오류 처리를 UI에서 분리했다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과, iOS·Android Debug build
- 리스크: 제조사·설치 음성별 실제 발화 속도와 품질 차이는 실기기 확인 필요

## 2026-07-29 - 계정 삭제 성공 후 로컬 세션 정리

- 변경 파일: `app_auth_service.dart`, `WORK_LOG.md`
- 내용: 서버 탈퇴 성공 때만 Secure Storage 세션을 삭제하고 실패 시 동일 세션으로 재시도할 수 있게 보존했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 네트워크 중단 실기기 재시도 UX는 수동 확인 필요

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `app_auth_service.dart`, `audio_recorder_service.dart`, `auth_session_store.dart`, `google_identity_service.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 인증 동시성 코드 목적 주석 보완

- 변경 파일: `app_auth_service.dart`, `WORK_LOG.md`
- 내용: single-flight refresh와 세션 revision 경쟁 조건 방어의 목적을 Dartdoc과 블록 주석으로 명시했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 동작 변경 없음

## 2026-07-23 - 동시 요청 안전 Refresh Token 자동 갱신

- 변경 파일: `app_auth_service.dart`, `WORK_LOG.md`
- 내용: 401 후 refresh와 1회 재시도, 동시 refresh single-flight, 회전 완료 후 늦은 401 경합 처리, 실패 시 Secure Storage 삭제를 구현했다. 세션 revision으로 로그아웃 중 늦은 refresh 응답이 세션을 복원하는 경쟁 조건도 차단했다.
- 검증: 인증 서비스 단위 테스트 및 Flutter 전체 테스트
- 리스크: refresh 실패는 보안상 재로그인을 요구하므로 일시적 서버 장애에도 로그인 화면으로 전환됨

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
