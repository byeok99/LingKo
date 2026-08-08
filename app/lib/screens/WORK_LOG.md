# 작업 이력

## 2026-08-07 - 재동의 gate 오류·진행 상태 추가

- 변경 파일: `consent_screen.dart`
- 내용: 신규 가입뿐 아니라 복원 세션 재동의에도 화면을 재사용하고 서버 문서 버전, 안전한 오류, 중복 제출 방지 loading을 표시한다.
- 검증: `flutter analyze`, `flutter test --coverage` 113개 통과(라인 81.26%)
- 리스크: 실제 기기에서 외부 문서 열람 후 복귀 UX 수동 확인 필요

## 2026-08-07 - Profile에 약관·처리방침 등 설정 항목 추가

- 변경 파일: `profile_screen.dart`
- 내용: `Legal & privacy` 섹션을 만들고 Terms of Service, Privacy Policy, Ad privacy settings, Contact us 행을 넣었다. 가입할 때 동의한 문서를 나중에 다시 읽을 수 없으면 동의가 형식 절차가 되므로 동의 화면과 같은 `onOpenDocument` 처리로 연결했다. 동작하지 않던 `Audio & privacy` 행은 실제 Privacy Policy 행으로 대체했고 About은 버전 행만 남겼다. 광고·문의는 기능 자체가 없어 기존 규칙대로 눌리지 않게 두었다.
- 검증: `flutter analyze`, `flutter test --coverage` 102개 통과(라인 81.27%)
- 리스크: 문서 열람 경로가 없어 두 행 모두 미공개 안내만 띄운다. 회원 탈퇴는 기존 버튼을 유지했다

## 2026-08-07 - 회원가입 동의 화면 추가

- 변경 파일: `consent_screen.dart`
- 내용: 로그인 수단을 고르기 전에 필수 2건(이용약관 동의, 개인정보 처리 관련 확인)과 선택 1건(마케팅 수신)을 받는 화면을 추가했다. 계정 생성 전에 동의를 받아 "거부한 사용자의 계정이 이미 만들어진" 상태를 만들지 않는다. 필수를 채우기 전에는 계속하기를 비활성으로 두고, 선택 거부에 불이익이 없다는 문구와 16세 이상 안내를 함께 노출한다. 전체 동의는 선택 항목까지 포함할 때만 켜진 것으로 표시한다.
- 검증: `flutter analyze`, `flutter test --coverage` 99개 통과(라인 81.10%)
- 리스크: 약관·처리방침 전문을 열 경로가 없어 `onOpenDocument`가 미공개 안내만 띄운다. 읽지 못한 채 동의하게 되므로 문서 배포 전에는 출시할 수 없다. 연령 안내는 고지일 뿐 실제 차단 수단이 아니다

## 2026-08-07 - 새 06 Result 디자인과 3단계 점수색 적용

- 변경 파일: `result_screen.dart`, `home_screen.dart`, `review_screen.dart`, `sound_detail_screen.dart`
- 내용: Result를 compact standard pronunciation 카드, 세로 어절 목록, 하단 고정 재연습 CTA로 재구성했다. 80점 이상 파랑·60점 이상 주황·60점 미만 빨강 규칙을 Home 취약음, Review 배지·상세, Sound 평균·기록에도 동일하게 적용했다.
- 검증: `flutter analyze`, `flutter test --coverage` 90개 통과(라인 80.40%), 좁은 화면·큰 글자 Result 테스트 통과
- 리스크: 실제 기기에서 Result 한 화면의 밀도와 하단 CTA 위치를 수동 확인해야 함

## 2026-08-07 - Home 취약 발음 타일을 붉은 tint로 변경

- 변경 파일: `home_screen.dart`
- 내용: 취약 음절 tile의 파란 면·파란 테두리를 제거하고 첨부 시안의 연한 붉은 면과 저채도 붉은 음절로 바꿨다. 로마자·점수는 읽기 대비를 유지한 청회색으로 표시한다.
- 검증: `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.49%)
- 리스크: 실제 기기 렌더링은 수동 확인 필요

## 2026-08-07 - Profile 그룹 카드의 내부 여백 복구

- 변경 파일: `profile_screen.dart`
- 내용: `padding: EdgeInsets.zero`인 그룹 카드 안의 설정 행이 테두리에 붙어 보였다. 구분선은 카드 폭 전체를 유지하면서 행 내용만 시안대로 좌우 14px 안쪽에 배치했다.
- 검증: `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.63%)
- 리스크: 없음

## 2026-08-07 - 도안에 없는 반경·자간 값 정리

- 변경 파일: `home_screen.dart`, `review_screen.dart`
- 내용: Direction A 시절 값이 남아 있었다. 취약 음절 타일의 반경 14→15, 타일 간격 9→8, 세로 여백 12→11, 로마자 굵기 w700→900, 자간 1.32→1.1로 도안에 맞췄다. Review 점수 배지는 도안이 `border-radius:50%`인데 반경 14의 둥근 사각형이라 원형으로 바꿨다. 도안의 반경은 999·18·15·12뿐이고 14는 존재하지 않는다.
- 검증: `flutter analyze`, `flutter test` 89개 통과
- 리스크: Review 점수 배지 색이 도안은 단색(softBlue/primaryDark)인데 구현은 점수별 초록·빨강이다. 정보량이 더 많은 쪽이라 판단 보류하고 그대로 뒀다

## 2026-08-06 - design-repair 11개 화면의 파란 카드형 UI 적용

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `review_screen.dart`, `saved_sentences_screen.dart`, `sound_detail_screen.dart`
- 내용: Sign in sample, Home 카테고리·추천 카드, Practice/Recording/Scoring, Result/Review 어절 탐색, Profile 그룹 카드, Sound detail, Saved 필터를 LingKo Blue 시안에 맞췄다. Recording은 임시 파일을 폐기하고 같은 문장을 다시 녹음하는 Restart를 추가했다.
- 검증: `flutter analyze`, `flutter test --coverage` 89개 통과(라인 80.64%)
- 리스크: Apple 로그인과 광고 SDK는 미연결 상태를 그대로 표시하며, 실제 기기 녹음·TTS·Google 로그인·media 재생 확인 필요

## 2026-08-06 - Home 취약 음절 타일을 도안 값에 맞춤

- 변경 파일: `home_screen.dart`, `review_screen.dart`
- 내용: 핸드오프 HTML과 대조해 8곳을 맞췄다. 라벨 문구('Your weakest sounds · tap for the guide'), 내용 가운데 정렬(좌측이었음), 음절 26px(22였음), 로마자 11px·w600·자간 1.32(10.5·w500·자간 없음), 반경 14(16이었음), 세로 여백 12·가로 0(10/10이었음), 음절-로마자 간격 5(3이었음), 블록 상단 여백 24와 하단 패딩 18. `review_screen.dart`는 같은 반경 리터럴 14를 새 토큰으로 바꾸기만 했다(시각 변화 없음).
- 검증: `flutter analyze`, `flutter test` 87개 통과
- 리스크: 도안 라벨이 'tap for the guide'인데 실제 탭은 07 가이드 시트가 아니라 10 Sound detail로 간다. 핸드오프 README의 흐름 정의와 HTML 문구가 서로 다르며, 이번에는 문구를 도안대로 따랐다

## 2026-08-06 - 취약 점수 단위를 어절에서 음절로 변경

- 변경 파일: `home_screen.dart`, `word_detail_screen.dart` → `sound_detail_screen.dart`
- 내용: Home 타일이 어절 대신 음절 하나를 보여준다. 라벨을 'Sounds to fix · tap for detail'로 바꾸고 한 글자라 글자 크기를 22px로 키웠다. 점수는 로마자와 한 줄에 묶어 음절 자체를 측정한 값처럼 보이지 않게 했다. 상세 화면은 제목 'Sound', 키 접두사 `sound-detail-*`로 정리했다.
- 검증: `flutter analyze`, `flutter test` 87개 통과
- 리스크: 핸드오프 02는 취약 음절 타일 1개("Sound to fix this week")를 말하는데 현재는 3개다. 개수 조정은 별도 판단

## 2026-08-06 - Review 상세에서 사라진 재연습 진입점 복구

- 변경 파일: `review_screen.dart`, `result_screen.dart`
- 내용: 리디자인 중 Review 상세의 'Practice again' 버튼이 빠져 `onRetryPractice`가 아무 데서도 호출되지 않는 죽은 콜백이 되어 있었다. 기록을 보고 나서 다시 연습할 방법이 없었다. 상세 시트 하단에 되살리고 시트를 먼저 닫은 뒤 Practice로 넘긴다. 상세 시트의 SectionHeader도 결과 화면과 같은 EyebrowLabel 체계로 통일했고, 추이 라벨의 'Last 1 tries' 단수 처리를 했다. Result의 로마자 표기에는 사라졌던 `result-romanized-pronunciation` 키를 복구했다.
- 검증: `flutter analyze`, `flutter test` 86개 통과
- 리스크: 없음

## 2026-08-06 - Result에서 죽은 재생 카드 제거

- 변경 파일: `result_screen.dart`, `practice_screen.dart`, `lingko_app.dart`
- 내용: 리디자인 후 쓰이지 않게 된 _PronunciationGuideCard가 남아 있었다. 재생은 Practice에서만 한다는 규칙을 어기는 Normal/Slow 버튼이 그 안에 들어 있었다. 함께 ResultScreen의 TTS 의존성도 끊었다. 버튼에 강제로 붙던 아이콘도 걷어 라벨만 남겼다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 그라디언트·그림자 0건, 버튼 아이콘 0건 확인
- 리스크: 없음

## 2026-08-06 - 04 Recording · 08 Review 마무리

- 변경 파일: `practice_screen.dart`, `review_screen.dart`
- 내용: Recording의 정지 버튼을 68px 흰 원 안의 붉은 사각형으로 바꿨다. 파랑으로 채우면 인터랙티브 색 규칙과 어긋나고, 붉은 사각형은 이 앱에서 유일한 붉은 컨트롤이라 형태만으로 구분된다. 녹음 중 안내 문구는 제거했다. Review는 기록을 카드 하나로 묶고 행을 [44px 점수 배지 · 문장·로마자·날짜 · 화살표]로 바꿔 세로로 훑을 때 숫자만 따라가게 했다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 위젯 테스트는 이전 레이아웃·문구 기준이라 갱신이 필요하다

## 2026-08-06 - Home·Result에 북마크 진입점 연결

- 변경 파일: `home_screen.dart`, `result_screen.dart`
- 내용: 문장 행과 Result 헤더에서 저장을 켤 수 있게 했다. Result에 둔 이유는 방금 연습한 문장을 다시 하고 싶을 때가 저장할 마음이 가장 큰 순간이기 때문이다. 직접 입력한 문장은 서버 식별자가 없어 토글 자체를 두지 않는다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 저장 상태 조회 실패 시 북마크가 꺼진 것처럼 보인다. 연습 자체는 막지 않는다

## 2026-08-06 - Saved sentences·Word detail 화면 추가

- 변경 파일: `saved_sentences_screen.dart`, `word_detail_screen.dart`, `home_screen.dart`, `profile_screen.dart`
- 내용: 저장 문장 목록과 취약 어절 상세 화면을 만들었다. Home에 취약 어절 타일 3개를 붙여 상세로 보내고, Profile의 Saved sentences 행을 실제 화면에 연결했다. 상세 화면은 연습 이력이 없으면 Suggested 탭으로 열어 진입 직후 할 수 있는 일이 화면에 있게 했다.
- 검증: `flutter analyze`, `./gradlew compileJava` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 북마크를 켜는 진입점(Home·Result)은 아직 서버와 연결되지 않아 목록이 비어 보일 수 있음

## 2026-08-06 - 03 Practice · 09 Profile 리디자인

- 변경 파일: `practice_screen.dart`, `profile_screen.dart`
- 내용: Practice는 입력만 카드로 채우고 표준 발음을 위아래 선으로 묶었다. 입력 글자를 21px로 키우고 테두리를 걷어 카드 자체가 입력 영역임을 드러냈다. Listen의 Normal·Slow를 동등한 secondary로 맞추고 CTA를 'Record'로 줄였다. Profile은 계정 카드를 구분선 블록으로 바꾸고 Your content·About 섹션과 설정 행을 넣었다. 아직 화면이 없는 항목은 눌리지 않게 흐리게 뒀다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: Saved sentences 화면(11)이 아직 없어 Profile의 해당 행은 비활성 상태다

## 2026-08-06 - 06 Result 리디자인

- 변경 파일: `result_screen.dart`
- 내용: 점수 카드(총점 60px + 세부 게이지 3개 + 판정 문구)를 한 덩어리로 묶고, How it should sound에서 원문과 표준 발음을 위아래로 붙여 차이를 눈으로 비교하게 했다. 어절 목록은 아코디언으로 바꿔 누른 어절의 음절만 펼친다. CTA를 'Say it again' 하나로 줄이고 Review 이동은 텍스트 버튼으로 내렸다. 화면 좌우 패딩을 20px로 통일했다.
- 검증: `flutter analyze` 통과. 화면 확인은 사용자가 직접 수행
- 리스크: 위젯 테스트는 이전 레이아웃 기준이라 갱신이 필요할 수 있음

## 2026-08-06 - Sign in 상단·하단 고정 배치 수정

- 변경 파일: `auth_gate_screen.dart`
- 내용: spaceBetween을 써도 바깥 Center가 Column을 통째로 세로 중앙에 놓아 배치가 그대로였다. 상단 내용을 Expanded 안의 스크롤 영역으로 만들어 위에 붙이고, 로그인 수단은 그 아래 바닥에 남게 했다.
- 검증: `flutter analyze` 통과. 테스트는 사용자가 직접 확인
- 리스크: 없음

## 2026-08-06 - Sign in 상단 정렬로 배치 수정

- 변경 파일: `auth_gate_screen.dart`
- 내용: 세로 중앙 정렬이던 로그인 화면을 상단 그룹(워드마크·헤드라인·샘플)과 하단 그룹(로그인 수단·각주)으로 나눴다. 화면이 길면 위아래로 벌리고 짧으면 스크롤한다. 시작 화면은 읽을 것이 없어 중앙 정렬을 유지한다.
- 검증: `flutter analyze`, `flutter test` 85개 통과
- 리스크: 없음

## 2026-08-06 - 01 Sign in · 02 Home 리디자인

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`
- 내용: Sign in을 카드 없는 전체 화면으로 바꾸고 헤드라인·안녕하세요 샘플·Google/Apple 버튼·각주로 재구성했다. Home은 워드마크와 에너지 캡슐 헤더, 인사 h1 26px, 카테고리 밑줄 탭, 구분선 기반 문장 목록, 하단 텍스트 버튼 2개로 바꿨다. 카테고리 칩을 밑줄 탭으로 바꾼 이유는 칩이 그 자체로 눌리는 덩어리처럼 보여 문장 목록과 시선을 나눠 갖기 때문이다.
- 검증: `flutter analyze`, `flutter test` 84개 통과
- 리스크: 06 Result·03 Practice·09 Profile 리디자인과 신규 화면(10·11)은 후속 작업

## 2026-08-05 - 로마자 발음 가이드 화면 연결

- 변경 파일: `practice_screen.dart`, `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: Practice·Recording·Result·Review 상세에서 표준 발음 아래 로마자 읽기 가이드를 표시한다.
- 검증: `flutter analyze`, `flutter test --coverage` 통과
- 리스크: 없음

## 2026-08-04 - Profile에서 언어 설정 섹션 제거

- 변경 파일: `profile_screen.dart`
- 내용: 저장만 되고 읽는 코드가 없던 언어 설정 섹션과 관련 상태·조회·저장 흐름을 모두 제거했다. Profile은 계정 정보와 로그아웃·탈퇴만 다룬다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 81개 통과
- 리스크: 기존 앱 빌드가 호출하던 preferences endpoint가 404가 됨

## 2026-08-04 - 표시 언어 설정 제거

- 변경 파일: `profile_screen.dart`
- 내용: 저장만 되고 화면에는 아무 영향이 없던 표시 언어 설정 행을 제거하고, 남은 모국어 설정만 다루도록 선택 흐름을 단순화했다.
- 검증: `./gradlew test integrationTest` 통과, `flutter analyze`, `flutter test` 83개 통과
- 리스크: `users.display_language` 컬럼이 남아 있어 후속 마이그레이션이 필요함

## 2026-08-04 - 화면 색 참조를 테마 기반으로 전환

- 변경 파일: `home_screen.dart`, `practice_screen.dart`, `result_screen.dart`, `review_screen.dart`, `profile_screen.dart`, `auth_gate_screen.dart`
- 내용: AppColors 상수 직접 참조를 context.palette로 바꿔 밝기에 따라 색이 따라오게 했다. BuildContext가 없는 CustomPainter에는 팔레트를 생성자로 주입했다.
- 검증: `flutter analyze`, `flutter test` 83개 통과, 어두운 팔레트 8개 색 조합 WCAG AA 본문 기준 충족 확인
- 리스크: 실제 기기의 다크 모드 렌더링과 이미지 가이드 대비는 수동 확인이 필요함

## 2026-08-04 - 녹음 화면 피드백 실데이터 연결과 햅틱

- 변경 파일: `practice_screen.dart`
- 내용: 고정값 0.38이던 진행 링을 실제 경과 비율로, 정지 그림이던 파형을 마이크 레벨 반응형으로 바꿨다. 표시만 하고 지켜지지 않던 10초 상한을 실제 종료로 강제하고, 60초를 넘기면 깨지던 타이머 표기를 고쳤다. 녹음 시작·종료에 촉각 피드백을 추가했다.
- 검증: `flutter analyze`, `flutter test` 81개 통과
- 리스크: 촉각 피드백과 실제 마이크 레벨은 시뮬레이터가 아닌 실기기 확인이 필요함

## 2026-08-04 - Result 화면 중복 안내 제거

- 변경 파일: `result_screen.dart`
- 내용: 버튼처럼 보이는 요소에 대한 조작 안내 문장을 제거해 단어별 발음 영역이 먼저 보이게 했다.
- 검증: `flutter analyze`, `flutter test` 80개 통과, `./gradlew test integrationTest` 통과
- 리스크: 없음

## 2026-08-04 - 상태 비교를 enum으로 교체

- 변경 파일: `result_screen.dart`
- 내용: 결과 화면의 점수 노출 판단을 ScoreStatus.isAvailable로 바꿨다.
- 검증: `./gradlew test integrationTest` 전체 통과, `flutter analyze`, `flutter test` 78개 통과
- 리스크: 동작 변경 없음

## 2026-08-04 - Practice Result·Review 단어 피드백 전환

- 변경 파일: `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: 전체 음절 점수 그리드를 제거하고 두 화면 모두 단어 선택 후 해당 음절 가이드만 보여주도록 변경했다.
- 검증: `flutter analyze`, 전체 `flutter test` 통과
- 리스크: 실제 기기 scroll·bottom sheet 동작 확인 필요

## 2026-08-04 - Review 목록·상세 정보 계층 개선

- 변경 파일: `review_screen.dart`, `WORK_LOG.md`
- 내용: 최근 연습을 독립 카드로 분리하고 점수·문장·표준 발음·등급·일시를 구분했다. 상세 sheet에 표준 발음, 평가 일시, accuracy·fluency·completeness와 실제 음절 점수를 구조화했다.
- 검증: Review widget 회귀 테스트, 320px·1.8배 글꼴 응답형 테스트, `flutter analyze`, `flutter test --coverage` 전체 73개 통과, line coverage 85.20%
- 리스크: 실제 기기의 긴 문장·확대 글꼴 배치는 수동 확인 필요

## 2026-08-03 - MAX 캡슐 가변 폭 허용

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 우측 정렬과 최대 166px 제한은 유지하면서 자식 capsule이 내용 너비로 축소될 수 있게 했다.
- 검증: MAX·충전 중 우측 정렬 포함 전체 Flutter test 72개 통과
- 리스크: 없음

## 2026-08-03 - 에너지 캡슐 우측 정렬 강화

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 상단 capsule 폭을 166px로 고정하고 Home 콘텐츠의 가장 오른쪽에 정렬했다.
- 검증: 크기·우측 좌표 포함 전체 Flutter test 72개 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - Home 에너지 소형 배치 재복원

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 전체 폭 배치 변경을 취소하고 LingKo 우측 같은 줄의 소형 capsule로 되돌렸다.
- 검증: 대상·반응형 포함 전체 Flutter test 72개 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - Home 에너지 배치 디자인 롤백

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 충전 기능은 유지하고 소형 우측 캡슐을 인사말 아래의 독립된 전체 폭 영역으로 복원했다.
- 검증: 에너지 callback 및 작은 화면·큰 글씨 widget test 통과
- 리스크: 실제 기기 시각 확인 필요

## 2026-08-03 - Home 상황별 추천 탐색 구성

- 변경 파일: `home_screen.dart`, `WORK_LOG.md`
- 내용: 추천 문장을 Daily·Food·Travel·Study·Work·Health 칩으로 구분하고 카테고리별 3개 미리보기, 전체 보기, 빈 직접 입력 진입을 추가했다. 진행 중인 평가는 추천 영역보다 위에 유지했다.
- 검증: `flutter analyze`, `flutter test --coverage` 전체 71개 통과, line coverage 83.33%, 500×924 Chrome 렌더링 확인
- 리스크: 실제 iPhone의 글꼴·가로 칩 스크롤 감각은 실기기 확인 필요

## 2026-08-03 - Profile 목표 레벨 UI 제거

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 실제 기능에 연결되지 않은 Target level 설정과 learner 배지를 제거하고 설정 영역을 Language preferences로 정리했다.
- 검증: `flutter analyze`, `flutter test --coverage` 전체 70개 통과, line coverage 83.20%
- 리스크: 없음

## 2026-07-30 - 평가·결과 화면 과도한 간소화 롤백

- 변경 파일: `home_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 아이콘 위주 구성으로 축소했던 Home 평가 카드와 Result를 직전 정보형 카드·summary·범례·상세 feedback 구성으로 복원했다. 점수 없는 가이드 진입과 표준 발음 기능은 유지했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 설명 밀도와 긴 문장 줄바꿈은 실제 iPhone에서 수동 확인 필요

## 2026-07-30 - Practice 정규화와 Result 가이드 유지

- 변경 파일: `practice_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 추천 선택·직접 입력·준비 응답의 기호 제거를 동일 경계에 적용하고, 점수 없는 음절도 URL이 있으면 Result 가이드 진입을 유지했다.
- 검증: `flutter analyze`, `flutter test` 전체 70개 통과
- 리스크: 실제 IME 조합과 작은 iPhone 화면은 실기기 확인 필요

## 2026-07-30 - 점수 없는 Result 가이드 진입 복구

- 변경 파일: `result_screen.dart`, `WORK_LOG.md`
- 내용: 문자 점수 `UNAVAILABLE`과 가이드 존재 여부를 분리해, `characters`가 있으면 점수 대신 `—`를 표시하면서도 음절 타일과 입·혀 가이드 진입을 유지하도록 수정했다.
- 검증: `flutter analyze`, `flutter test` 전체 66개 통과
- 리스크: 실제 평가 응답의 가이드 URL과 긴 이미지 비율은 실기기 확인 필요

## 2026-07-30 - Practice 표준 발음 상시 노출과 안내 축소

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 표준 발음 확인·숨김 토글을 제거하고 준비 완료 즉시 항상 표시했다. 하드코딩 진행률, 자동 준비 설명, 완료 배지, 번역과 학습 팁을 제거해 문장 입력·표준 발음·듣기·녹음에 집중하도록 정리했다.
- 검증: `flutter analyze`, `flutter test` 전체 65개 통과
- 리스크: 긴 표준 발음 줄바꿈은 실제 iPhone에서 수동 확인 필요

## 2026-07-30 - Practice 문장 준비 흐름 통합

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 추천·직접 입력 내부 탭과 수동 확정 버튼을 제거하고 하나의 편집 가능한 문장 입력으로 통합했다. 입력 중 700ms debounce 후 표준 발음을 자동 준비하며, 현재 입력과 일치하는 최신 응답에만 듣기·녹음을 허용한다.
- 검증: `flutter analyze`, `flutter test` 전체 65개 통과
- 리스크: 실제 한국어 IME 조합 중 debounce 체감과 키보드 노출 화면은 iPhone에서 수동 확인 필요

## 2026-07-30 - Practice 표준 발음 확인 토글 복원

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 상세 음절 가이드는 평가 후에 유지하면서 녹음 전에도 `Check standard pronunciation`으로 표준 발음을 선택해 확인할 수 있게 했다. 문장·모드 변경과 녹음 시작 때는 다시 접힌다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과, 펼친 Practice 402px phone 렌더링 확인
- 리스크: 실제 iPhone에서 긴 표준 발음 줄바꿈은 수동 확인 필요

## 2026-07-30 - 평가 후 표준 발음 가이드 재배치

- 변경 파일: `practice_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 녹음 전 Practice에서 표준 발음과 음절 가이드를 숨기고, Result에 원문·표준 발음만 비교하는 카드와 Normal·Slow 듣기를 추가했다. 사용자 인식 문장 표시는 제거했다.
- 검증: `flutter analyze`, `flutter test` 전체 63개 통과, Result 프로토타입 402px phone 렌더링 확인
- 리스크: 긴 표준 발음 문장의 실제 iPhone 줄바꿈과 기기 TTS 음질은 수동 확인 필요

## 2026-07-30 - Practice 문장 듣기 활성화

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 비활성 Normal·Slow 버튼을 현재 한국어 원문 TTS에 연결하고 문장 변경·녹음 시작·화면 종료 시 기존 발화를 중지하며 재생 실패 안내를 추가했다.
- 검증: 자유 문장 속도 선택·안전한 오류 안내 widget test, `flutter analyze`, 전체 63개 테스트
- 리스크: 실제 iPhone 스피커·무음 모드·한국어 음성 설정 조합은 수동 확인 필요

## 2026-07-30 - Review 최근 점수 추이 시간축 수정

- 변경 파일: `review_screen.dart`, `WORK_LOG.md`
- 내용: 최신순 API 응답에서 최근 7개 점수만 선택하고 그래프를 오래된 점수에서 최신 점수 방향으로 그리며 각 점수 값을 표시하도록 수정했다.
- 검증: 최근 점수 정렬 단위 테스트, `flutter analyze`
- 리스크: 기록 API 첫 page를 기준으로 하므로 7개보다 오래된 추이는 표시하지 않음

## 2026-07-30 - preview v2 화면 정보 계층 적용

- 변경 파일: `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: Home의 실제 quota 진행률·추천 목록, Practice 단계·녹음 타이머, Result 5열 음절·약점 피드백, Review 추세, Profile 설정을 새 프로토타입 밀도에 맞췄다. 평가 중에는 명시적 백그라운드 이동을 제공한다.
- 검증: iPhone 15 비율 Home·Result PNG 렌더링 확인, `flutter analyze`, `flutter test` 61개 통과
- 리스크: 키보드가 열린 자유 문장 입력과 실제 녹음 권한 sheet는 시뮬레이터 수동 확인 필요

## 2026-07-29 - 자유 문장 특수 기호 자동 제거

- 변경 파일: `practice_screen.dart`, `WORK_LOG.md`
- 내용: 자유 문장 입력·붙여넣기의 Unicode 문장부호·기호를 즉시 제거하고 제출 직전에도 같은 정규화를 적용해 평가 글자로 전달되지 않게 했다.
- 검증: 자유 문장 widget test 통과
- 리스크: 서버 직접 호출 정규화는 현재 범위에 포함하지 않음

## 2026-07-29 - 프로필 회원 탈퇴 확인 흐름

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 삭제 범위 경고와 명시적 확인 후 계정을 삭제하고, 실패 시 로그인 상태를 유지하며 재시도를 안내한다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 실제 기기 접근성·스토어 심사 문구는 수동 확인 필요

## 2026-07-28 - UI 최종 접근성 검토

- 변경 파일: `auth_gate_screen.dart`, `practice_screen.dart`, `WORK_LOG.md`
- 내용: 로그인 버튼 radius와 녹음 파형 pill radius를 공통 토큰으로 통일하고 작은 화면·확대 글자 흐름을 재검토했다.
- 검증: 320px 화면, 1.8배 글자 확대 widget 테스트 및 전체 Flutter 검증
- 리스크: 실제 기기별 Safe Area와 키보드 노출은 수동 확인 필요

## 2026-07-28 - 전체 앱 화면 구조와 상태 UX 재구성

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `review_screen.dart`, `WORK_LOG.md`
- 내용: 블루 시안에 맞춰 로그인·홈·연습·녹음·평가·결과·기록·설정을 재구성하고 녹음 종료 후 자동 평가, 전체 음절 결과, 전체 문장 재연습을 연결했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 문장 듣기와 가이드 동영상 재생은 실제 재생 계층이 없어 비활성 안내로 유지

## 2026-07-24 - 한국어 의도 중심 주석 보강

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `result_screen.dart`, `WORK_LOG.md`
- 내용: 해당 폴더의 코드에 의도, 업무 의미, 구현 이유, 선택 기준을 설명하는 한국어 주석을 보강했다.
- 검증: `flutter analyze`, `flutter test` 통과
- 리스크: 동작 변경 없음


## 2026-07-23 - 프로필 세션 처리 목적 주석 보완

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 오프라인 로그아웃과 인증 만료 시 프로필 상태 삭제 목적을 명시했다.
- 검증: `flutter analyze`, `flutter test`
- 리스크: 동작 변경 없음

## 2026-07-23 - Profile 보호 API 갱신·로그아웃 연결

- 변경 파일: `profile_screen.dart`, `WORK_LOG.md`
- 내용: 기록·설정 API의 401 자동 갱신과 서버 로그아웃을 연결하고 refresh 실패 시 로그인 화면으로 전환했다.
- 검증: Flutter widget 테스트 및 전체 테스트
- 리스크: 네트워크 단절 중 로그아웃은 로컬 세션만 즉시 삭제되고 서버 세션은 절대 만료까지 남을 수 있음

## 2026-07-20 - 로그인 게이트, Google 버튼, quota 화면 이력 이전

- 변경 파일: `auth_gate_screen.dart`, `home_screen.dart`, `practice_screen.dart`, `profile_screen.dart`, `WORK_LOG.md`
- 내용: 세션 복원 중 로고 스플래시와 로그인 필수 게이트를 추가하고 Google 공식 로고 버튼을 적용했다. Home의 잔여 quota 표시, Practice의 quota 소진 시 녹음 제한, Profile 인증 변경 후 quota 갱신을 연결했다.
- 검증: `cd app && flutter test --reporter compact`, `cd app && flutter analyze`
- 리스크: 실제 Google 로그인과 quota API 연동은 emulator/device에서 수동 확인 필요

## 2026-07-20 - 작업 이력 파일 초기화

- 변경 파일: `WORK_LOG.md`
- 내용: 이 디렉터리에서 수행한 변경과 검증 이력을 최소 경로 단위로 관리하기 위해 작업 이력 파일을 생성했다.
- 검증: 파일 생성 여부 확인
- 리스크: 없음
## 2026-08-03 - Home 상단 에너지 배치

- 변경 파일: `home_screen.dart`, `practice_screen.dart`
- 내용: LingKo 제목 반대편에 작은 에너지 캡슐을 배치하고 0회 안내 문구를 갱신했다.
- 검증: 작은 화면·큰 글씨 및 전체 widget test 통과
- 리스크: 실제 기기에서 최종 시각 확인 필요
