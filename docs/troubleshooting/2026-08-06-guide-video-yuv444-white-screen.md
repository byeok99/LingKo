# 가이드 영상이 오류 없이 흰 화면으로만 재생되는 문제

- 상태: 해결
- 최초 발견일: 2026-08-06
- 영향 범위: 발음 가이드 시트에서 영상 가이드를 보는 모든 iOS 사용자. 홀수 해상도로 생성된 음절이 대상이며 실측 7건 중 5건
- 심각도: SEV-2
- 영역: Backend / External / Flutter
- 관련 Issue: 미할당
- 관련 PR: 미할당

## 문제 현상

가이드 시트에서 어떤 음절은 영상이 정상 재생되고, 어떤 음절은 로딩이 끝난 뒤 **흰 화면만** 표시된다. 오류 메시지도 실패 상태도 없고 `_VideoGuideAsset`의 `hasError`는 false, `isReady`는 true가 된다. 즉 `VideoPlayerController.initialize()`가 성공하고 앱은 정상 재생 중이라고 판단한다.

`친구를 만났어요` 기준으로 `만`의 입술과 `친`의 혀만 재생되고 나머지 5개 영상은 모두 흰 화면이었다.

## 사용자 또는 운영 영향

- 발음 가이드는 이 제품의 핵심 학습 수단이다. 영상이 안 보이면 평가 결과를 받고도 교정 방법을 알 수 없다.
- 실패가 실패처럼 보이지 않아 사용자는 앱이 고장 났는지 자기 네트워크 문제인지 판단할 수 없다.
- Replicate 보간 비용과 S3 저장 비용은 이미 지불된 상태에서 결과물이 쓸모없어진다.

## 발생 조건

영상 해상도의 가로 또는 세로가 **홀수**일 때 발생한다. 실측 7건 전수:

| 파일 | 해상도 | 가로 | 세로 | pix_fmt | 결과 |
|---|---|---|---|---|---|
| 만-mouth | 206x154 | 짝 | 짝 | yuv420p | 재생 |
| 친-tongue | 308x156 | 짝 | 짝 | yuv420p | 재생 |
| 구-tongue | 310x157 | 짝 | 홀 | yuv444p | 흰 화면 |
| 를-tongue | 310x157 | 짝 | 홀 | yuv444p | 흰 화면 |
| 만-tongue | 309x152 | 홀 | 짝 | yuv444p | 흰 화면 |
| 나-tongue | 309x152 | 홀 | 짝 | yuv444p | 흰 화면 |
| 써-tongue | 309x157 | 홀 | 홀 | yuv444p | 흰 화면 |

해상도 홀짝과 재생 여부가 100% 일치한다.

## 재현 방법

1. DB에서 대상 음절의 영상 URL을 얻는다.

```sql
SELECT syllable_char, mouth_guide_url, tongue_guide_url
FROM evaluation_syllable
WHERE evaluation_log_idx = <id> AND tongue_guide_url LIKE '%.mp4';
```

2. 내려받아 픽셀 포맷을 확인한다.

```bash
ffprobe -v error -select_streams v:0 \
  -show_entries stream=profile,width,height,pix_fmt -of csv=p=0 <file>.mp4
```

3. `yuv444p` / `High 4:4:4 Predictive`이면 iOS에서 흰 화면으로 재생된다.

## 조사 과정

1. **가설: iOS Simulator의 `video_player` texture 제약** → 기각. 이전 조사에서 이 결론을 냈으나 잘못이었다. 당시 확인한 파일이 우연히 `yuv420p`였다. 같은 기기에서 일부는 정상 재생되므로 기기 차원의 제약이 아니다.
2. **가설: S3 ACL·Content-Type·네트워크 문제** → 기각. 7건 모두 HTTP 200에 정상 바이트 수로 내려온다.
3. **가설: 영상이 아니라 정적 PNG였다** → 부분 기각. 입술 가이드가 대부분 PNG인 것은 사실이지만(자음 17개의 `mouth_url`이 비어 있어 프레임이 1장뿐), 흰 화면 대상은 모두 실제 `.mp4`였다. 두 현상은 별개다.
4. **채택: 픽셀 포맷** → `ffprobe` 전수 확인에서 홀짝과 100% 일치했다.

## 확인한 증거

- 재생되는 2건: `yuv420p`, profile `High`
- 흰 화면 5건: `yuv444p`, profile `High 4:4:4 Predictive`
- 인코더 태그(`Lavf58.29.100` / `Lavf58.76.100`)는 두 그룹에 섞여 있어 판별 요인이 아니다.
- 보정 검증: `써-tongue`(309x157 yuv444p, 20696 bytes)를 재인코딩하면 308x156 yuv420p(9046 bytes)가 되고 프레임 수 9는 보존된다. 정상 재생되는 `친-tongue`과 같은 형식이다.

## 근본 원인

1. 가이드 도해 PNG의 크기가 제각각이고 홀수 변이 섞여 있다.
2. Replicate 프레임 보간 결과가 그 크기를 그대로 따른다.
3. `yuv420p`는 크로마를 가로·세로 2배씩 서브샘플링하므로 **짝수 해상도를 요구**한다. 변이 홀수면 인코더가 `yuv444p`로 떨어진다.
4. Apple VideoToolbox는 **H.264 High 4:4:4 Predictive를 디코딩하지 못한다.** AVPlayer는 오류 없이 ready 상태까지 진행한 뒤 프레임만 내놓지 않는다.

기존 파이프라인에는 이를 바로잡을 지점이 없었다. 세그먼트가 1개면 병합을 건너뛰고(`FrameInterpolationVideoGenerator`), 병합하더라도 `VideoMerger`가 `-c copy`라 재인코딩이 일어나지 않는다.

## 해결 방법

**근본 해결**: `VideoPlaybackNormalizer`를 추가해 S3 업로드 직전에 항상 재인코딩한다.

```
-vf scale=trunc(iw/2)*2:trunc(ih/2)*2
-c:v libx264 -profile:v high -pix_fmt yuv420p -an -movflags +faststart
```

홀수 변은 늘리지 않고 1px 줄인다. 도해 가장자리가 여백이라 손실이 내용에 영향을 주지 않는 반면, 늘리면 없는 픽셀을 만들어야 한다.

보정 실패 시 예외를 던지지 않고 원본 경로를 반환한다. 이 단계는 호환성 보정이지 생성의 일부가 아니므로, 여기서 막히면 Replicate 호출까지 끝난 결과를 통째로 버리게 된다.

## 선택하지 않은 대안

- **조건부 재인코딩(ffprobe로 판별 후 필요할 때만)**: 프로세스 호출이 한 번 더 늘고 분기가 생긴다. 대상이 1초 미만 수십 KB라 항상 재인코딩하는 편이 더 싸다.
- **`VideoMerger`에서 `-c copy` 제거**: 세그먼트 1개 경로가 병합을 건너뛰므로 대상의 상당수를 놓친다.
- **원본 도해 PNG를 모두 짝수로 재작업**: 근본적이지만 디자인 자산 전수 수정이 필요하고, 외부 모델이 크기를 바꿔 내보내면 다시 깨진다. 파이프라인 끝에서 보장하는 편이 견고하다.
- **앱에서 재생 실패를 감지해 대체 표시**: `video_player`가 실패를 보고하지 않으므로(ready·오류 없음) 감지할 방법이 없다.

## 검증 방법

- 자동 test: `VideoPlaybackNormalizerTest`가 FFmpeg 실행 실패 시 원본 경로 반환과 임시 파일 미잔존을 검증한다.
- 수동: 새 평가를 실행해 생성된 영상을 `ffprobe`로 확인하고 앱에서 재생을 확인한다.

## 변경 전후 결과

| 항목 | 변경 전 | 변경 후 |
|---|---:|---:|
| 재생 가능 비율(실측 7건) | 2/7 | 신규 생성분 미측정 |
| `써-tongue` 형식 | 309x157 yuv444p | 308x156 yuv420p (수동 재인코딩 확인) |
| 파일 크기 | 20696 bytes | 9046 bytes (같은 파일 기준) |

신규 생성분의 실제 재생률은 아직 측정하지 않았다. 개선 수치로 표현하지 않는다.

## 롤백 방법

`FrameInterpolationVideoGenerator`에서 `videoPlaybackNormalizer.normalize` 호출을 제거하고 `finalVideoPath`를 그대로 업로드하면 된다. 데이터·설정·migration 변경 없음. 이미 보정되어 저장된 영상은 그대로 재생 가능하므로 되돌려도 손실이 없다.

## 재발 방지

- **이미 S3에 있는 깨진 영상은 자동으로 고쳐지지 않는다.** `s3Uploader.findPublicUrl`이 캐시로 재사용하므로, 기존 객체를 지워야 재생성된다.
- 설계 규칙: 외부 모델이 만든 미디어는 형식을 신뢰하지 말고 전달 경계에서 한 번 정규화한다.
- 후속: 생성 직후 `pix_fmt`를 검사해 `yuv420p`가 아니면 경고하는 방어 로그를 검토한다.

## 모니터링 및 알림

미구축. 현재는 `ffprobe` 수동 확인만 가능하다.

## 남은 위험

- 이미 저장된 깨진 영상의 정리 범위와 시점이 정해지지 않았다.
- 입술 가이드가 대부분 정적 PNG인 것은 **별개 문제**다. `syllable_mapping.json`의 자음 17개(ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ 등)에 `mouth_url`이 비어 있어 프레임이 1장뿐이라 보간 대상이 아니다. 입술 모양이 구별되는 것은 양순음뿐이라는 판단으로 보이며, 자산 확충 여부는 별도 결정이 필요하다.

## 관련 코드와 문서

- `backend/src/main/java/com/lingko/lingko/infra/pronunciation/VideoPlaybackNormalizer.java`
- `backend/src/main/java/com/lingko/lingko/infra/pronunciation/FrameInterpolationVideoGenerator.java`
- `backend/src/main/java/com/lingko/lingko/infra/pronunciation/VideoMerger.java`
- `app/lib/widgets/guide_sheet.dart`
