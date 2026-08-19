// 파일 의도: widget test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/evaluation_api.dart';
import 'package:lingko_app/models/score_status.dart';
import 'package:lingko_app/api/practice_content_api.dart';
import 'package:lingko_app/api/practice_quota_api.dart';
import 'package:lingko_app/api/pronunciation_api.dart';
import 'package:lingko_app/api/sentence_api.dart';
import 'package:lingko_app/app/lingko_app.dart';
import 'package:lingko_app/models/auth_session.dart';
import 'package:lingko_app/models/evaluation_job.dart';
import 'package:lingko_app/models/practice_history.dart';
import 'package:lingko_app/models/practice_quota.dart';
import 'package:lingko_app/models/ad_reward_session.dart';
import 'package:lingko_app/models/practice_result.dart';
import 'package:lingko_app/models/practice_sentence.dart';
import 'package:lingko_app/models/weak_sound.dart';
import 'package:lingko_app/screens/result_screen.dart';
import 'package:lingko_app/services/audio_recorder_service.dart';
import 'package:lingko_app/services/app_auth_service.dart';
import 'package:lingko_app/models/consent_selection.dart';
import 'package:lingko_app/models/legal_consent_status.dart';
import 'package:lingko_app/services/legal_document_launcher.dart';
import 'package:lingko_app/services/sentence_speech_service.dart';
import 'package:lingko_app/services/rewarded_ad_service.dart';
import 'package:lingko_app/widgets/guide_sheet.dart';
import 'package:lingko_app/widgets/result_tile.dart';
import 'package:lingko_app/widgets/sentence_card.dart';
import 'package:lingko_app/widgets/shared_widgets.dart';

/// 화면 아래쪽 컨트롤을 누르기 전에 보이는 위치로 스크롤한다.
///
/// 기본 테스트 뷰포트(800x600)는 디자인 기준 기기(402x772)보다 짧아, 하단 CTA가
/// 화면 밖에 있을 수 있다. 실제 기기에서는 보이지만 테스트에서만 안 보이는 상황이라
/// 레이아웃을 테스트에 맞춰 줄이지 않고 스크롤로 맞춘다.
Future<void> _tapVisible(WidgetTester tester, Finder finder) async {
  await tester.ensureVisible(finder);
  await tester.pumpAndSettle();
  await tester.tap(finder);
  await tester.pumpAndSettle();
}

/// 취약 음절과 저장 문장 조회를 결정적으로 대체한다.
///
/// 취약 음절은 서버가 어절 점수를 음절에 귀속시켜 만든 값이라, 앱은 받은 대로 표시하고
/// 다시 계산하지 않는다. 그 계약을 지키는지 보려면 화면에 무엇이 그려지는지만 확인하면 된다.
class FakePracticeContentApi implements PracticeContentApi {
  FakePracticeContentApi({
    this.weakSounds = const [],
    this.detail,
    this.savedSentences = const [],
  });

  final List<WeakSound> weakSounds;
  final SoundDetail? detail;
  final List<PracticeSentence> savedSentences;
  final List<int> toggledSentenceIds = [];

  /// 상세 화면이 어떤 음절을 요청했는지 확인하기 위해 마지막 인자를 남긴다.
  String? lastRequestedCharacter;

  @override
  Future<List<PracticeSentence>> fetchSavedSentences({
    required String accessToken,
  }) async => savedSentences;

  @override
  Future<bool> toggleSavedSentence({
    required String accessToken,
    required int sentenceId,
  }) async {
    toggledSentenceIds.add(sentenceId);
    return false;
  }

  @override
  Future<List<WeakSound>> fetchWeakSounds({
    required String accessToken,
    int limit = 3,
  }) async => weakSounds;

  @override
  Future<SoundDetail> fetchSoundDetail({
    required String accessToken,
    required String character,
  }) async {
    lastRequestedCharacter = character;
    return detail ??
        SoundDetail(
          text: character,
          romanization: 'ssi',
          averageScore: 62,
          attemptCount: 8,
          practiced: const [],
          suggested: const [],
        );
  }
}

/// 테스트에서 Fake Pronunciation Api 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakePronunciationApi implements PronunciationApi {
  FakePronunciationApi({this.error, this.prepareHandler});

  final Object? error;
  final Future<PracticeSentence> Function(String text)? prepareHandler;
  String? lastText;
  final List<String> requestedTexts = [];

  @override
  Future<PracticeSentence> prepareCustomSentence(String text) async {
    lastText = text;
    requestedTexts.add(text);

    if (error != null) {
      throw error!;
    }
    if (prepareHandler != null) {
      return prepareHandler!(text);
    }

    return PracticeSentence(
      text: text,
      pronunciation: '서버 표준 발음',
      translation: 'Practice with your own sentence.',
      level: 'CUSTOM',
      category: 'Free practice',
      point: 'Linking across syllables',
      score: 0,
      characters: const [
        CharacterResult(
          character: '서',
          score: 0,
          note: 'Focus on tongue placement',
          kind: 'TONGUE',
        ),
      ],
    );
  }
}

/// 테스트에서 Fake Evaluation Api 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeEvaluationApi implements EvaluationApi {
  String? lastAudioPath;
  int? lastSentenceId;
  String? lastText;
  Object? error;
  Completer<EvaluationJob>? createJobCompleter;
  PracticeHistory history = PracticeHistory(
    items: [
      PracticeHistoryItem(
        evaluationLogId: 10,
        sentenceId: 1,
        source: 'RECOMMENDED',
        originalText: '맛있겠다.',
        standardPronunciation: '마싯게따.',
        romanizedPronunciation: 'ma-sit-ge-tta',
        recognizedText: '마싯게따.',
        overallScore: 91,
        gradeLabel: 'Excellent',
        summary: 'Clear pronunciation.',
        scoreBreakdown: const PracticeScoreBreakdown(
          accuracy: 92,
          fluency: 90,
          completeness: 93,
        ),
        characters: const [
          CharacterResult(
            character: '맛',
            score: 88,
            note: 'Keep the final consonant clear.',
            kind: 'NONE',
          ),
        ],
        words: const [
          PracticeWordResult(
            position: 0,
            text: '마싯게따',
            score: 88,
            scoreStatus: ScoreStatus.available,
            syllables: [
              CharacterResult(
                character: '맛',
                score: 0,
                scoreStatus: ScoreStatus.unavailable,
                note: 'Keep the final consonant clear.',
                kind: 'NONE',
              ),
            ],
          ),
        ],
        createdAt: DateTime(2026, 6, 26, 9, 30),
      ),
    ],
    page: 0,
    size: 10,
    totalItems: 1,
    totalPages: 1,
    hasNext: false,
    bestScore: 91,
  );

  @override
  Future<EvaluationUpload> prepareUpload({
    required String accessToken,
    required String audioPath,
  }) async {
    lastAudioPath = audioPath;
    if (error != null) {
      throw error!;
    }
    return EvaluationUpload(
      objectKey: 'evaluation-audio/7/test.wav',
      uploadUrl: 'https://signed.example/test',
      expiresAt: DateTime.utc(2026, 7, 27, 1, 10),
    );
  }

  @override
  Future<void> uploadAudio({
    required EvaluationUpload upload,
    required String audioPath,
  }) async {
    if (error != null) {
      throw error!;
    }
  }

  @override
  Future<EvaluationJob> createJob({
    required String accessToken,
    required String idempotencyKey,
    required String objectKey,
    int? sentenceId,
    String? text,
  }) async {
    lastSentenceId = sentenceId;
    lastText = text;

    if (error != null) {
      throw error!;
    }
    if (createJobCompleter != null) {
      return createJobCompleter!.future;
    }

    return const EvaluationJob(
      jobId: 'job-id',
      status: EvaluationJobStatus.succeeded,
      result: PracticeResult(
        overallScore: 91,
        gradeLabel: 'Excellent',
        summary: 'Clear pronunciation.',
        recognizedText: '사용자 발음',
        characterScoreStatus: ScoreStatus.unavailable,
        scoreBreakdown: PracticeScoreBreakdown(
          accuracy: 92,
          fluency: 90,
          completeness: 93,
        ),
        weakCharacters: [],
        characters: [],
      ),
    );
  }

  @override
  Future<EvaluationJob> fetchJob({
    required String accessToken,
    required String jobId,
  }) async {
    throw StateError('completed fake jobs must not be polled');
  }

  @override
  Future<PracticeHistory> fetchHistory({
    required String accessToken,
    int page = 0,
    int size = 10,
  }) async {
    if (error != null) {
      throw error!;
    }

    return history;
  }
}

/// 테스트에서 Fake Practice 할당량 Api 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakePracticeQuotaApi implements PracticeQuotaApi {
  FakePracticeQuotaApi({
    this.quota = const PracticeQuota(
      date: '2026-06-17',
      freeLimit: 5,
      freeUsed: 2,
      rewardedAvailable: 0,
      remainingPractices: 3,
      nextRefillAt: null,
      serverTime: null,
    ),
  });

  PracticeQuota quota;
  PracticeQuota? rewardQuota;
  String? lastAccessToken;
  Object? error;
  String? lastSessionToken;
  int sessionStatusFetchCount = 0;

  @override
  Future<PracticeQuota> fetchTodayQuota({required String accessToken}) async {
    lastAccessToken = accessToken;

    if (error != null) {
      throw error!;
    }

    return quota;
  }

  @override
  Future<AdRewardSession> createAdRewardSession({
    required String accessToken,
  }) async {
    lastAccessToken = accessToken;
    if (error != null) {
      throw error!;
    }
    lastSessionToken = 'ssv-session-token';
    return AdRewardSession(
      sessionToken: lastSessionToken!,
      expiresAt: DateTime.parse('2026-06-17T13:00:00+09:00'),
    );
  }

  @override
  Future<AdRewardSessionStatus> fetchAdRewardSessionStatus({
    required String accessToken,
    required String sessionToken,
  }) async {
    lastAccessToken = accessToken;
    lastSessionToken = sessionToken;
    sessionStatusFetchCount++;
    quota = rewardQuota ?? quota;
    return const AdRewardSessionStatus(
      status: AdRewardStatus.completed,
      credited: true,
    );
  }
}

/// 실제 Mobile Ads SDK 없이 reward callback의 획득·취소 결과를 shell에 전달한다.
class FakePracticeRewardAdService implements PracticeRewardAdService {
  FakePracticeRewardAdService({
    this.result = RewardedAdResult.earned,
    this.isConfigured = true,
  });

  final RewardedAdResult result;

  @override
  final bool isConfigured;

  int showCount = 0;
  int privacyOptionsCount = 0;
  String? lastCustomData;

  @override
  Future<RewardedAdResult> show({required String customData}) async {
    showCount++;
    lastCustomData = customData;
    return result;
  }

  @override
  Future<void> showPrivacyOptions() async {
    privacyOptionsCount++;
  }
}

/// 테스트에서 Fake Audio Recorder 서비스 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
/// 실제 브라우저를 띄우지 않고 어떤 문서를 요청했는지만 기록한다.
class FakeLegalDocumentLauncher implements LegalDocumentLauncher {
  FakeLegalDocumentLauncher({this.succeeds = true});

  /// 브라우저를 열 수 없는 기기를 흉내 낸다.
  final bool succeeds;

  final List<ConsentDocument> opened = [];

  @override
  Future<bool> open(ConsentDocument document, {String language = 'en'}) async {
    opened.add(document);
    return succeeds;
  }
}

class FakeAudioRecorderService implements AudioRecorderService {
  FakeAudioRecorderService({
    List<bool> permissions = const [true],
    this.deleteError,
  }) : _permissions = [...permissions];

  final List<bool> _permissions;
  final Object? deleteError;
  bool started = false;
  bool cancelled = false;
  int startCount = 0;
  int cancelCount = 0;
  final List<String> deletedPaths = [];
  int permissionChecks = 0;

  @override
  Future<bool> hasPermission() async {
    final index =
        permissionChecks < _permissions.length
            ? permissionChecks
            : _permissions.length - 1;
    permissionChecks++;
    return _permissions[index];
  }

  @override
  Future<String> start() async {
    started = true;
    startCount++;
    return '/tmp/lingko-test.wav';
  }

  /// 테스트가 마이크 레벨을 직접 밀어넣어 파형 반응을 검증할 수 있게 한다.
  final StreamController<double> amplitudeController =
      StreamController<double>.broadcast();

  @override
  Stream<double> amplitudeStream() => amplitudeController.stream;

  @override
  Future<String?> stop() async {
    return '/tmp/lingko-test.wav';
  }

  @override
  Future<void> cancel() async {
    cancelCount++;
    cancelled = true;
  }

  @override
  Future<void> dispose() async {}

  @override
  Future<void> delete(String path) async {
    if (deleteError != null) {
      throw deleteError!;
    }
    deletedPaths.add(path);
  }
}

/// 문장 듣기 테스트가 기기 TTS 없이 발화 문장과 속도 계약을 검증하도록 대체한다.
class FakeSentenceSpeechService implements SentenceSpeechService {
  String? lastText;
  SentenceSpeechRate? lastRate;
  int stopCount = 0;
  Object? error;

  @override
  Future<void> speak(String text, {required SentenceSpeechRate rate}) async {
    if (error != null) {
      throw error!;
    }
    lastText = text;
    lastRate = rate;
  }

  @override
  Future<void> stop() async {
    stopCount++;
  }

  @override
  Future<void> dispose() async {}
}

/// 테스트에서 Fake App Auth 서비스 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeAppAuthService implements AppAuthService {
  FakeAppAuthService({
    this.restoreExistingSession = false,
    this.restoreCompleter,
    this.expireAuthenticatedRequests = false,
    this.legalConsentRequired = false,
    this.legalConsentStatusError,
    this.legalConsentRecordError,
  });

  final bool restoreExistingSession;
  final Completer<AuthSession?>? restoreCompleter;
  final bool expireAuthenticatedRequests;
  final bool legalConsentRequired;
  final Object? legalConsentStatusError;
  final Object? legalConsentRecordError;
  bool signInCalled = false;
  bool appleSignInCalled = false;
  final reviewAccessCodes = <String>[];
  int legalConsentRecordCount = 0;
  ConsentSelection? recordedConsent;
  bool deleteAccountCalled = false;
  Object? deleteAccountError;
  Object? error;
  AuthSession? session = const AuthSession(
    tokenType: 'Bearer',
    accessToken: 'access.jwt',
    refreshToken: 'refresh.jwt',
    expiresInSeconds: 1800,
    user: AuthUser(
      userId: 7,
      email: 'user@example.com',
      name: 'LingKo User',
      profileImageUrl: 'https://example.com/profile.png',
    ),
  );

  @override
  Future<AuthSession?> restoreSession() async {
    if (restoreCompleter != null) {
      return restoreCompleter!.future;
    }
    return restoreExistingSession ? session : null;
  }

  @override
  Future<AuthSession> signInWithGoogle() async {
    signInCalled = true;

    if (error != null) {
      throw error!;
    }

    return session!;
  }

  @override
  Future<AuthSession> signInWithApple() async {
    appleSignInCalled = true;
    if (error != null) {
      throw error!;
    }
    return session!;
  }

  @override
  Future<AuthSession> signInForReview(String accessCode) async {
    reviewAccessCodes.add(accessCode);
    if (error != null) {
      throw error!;
    }
    return session!;
  }

  @override
  Future<T> runAuthenticated<T>(
    Future<T> Function(String accessToken) request,
  ) async {
    if (expireAuthenticatedRequests) {
      session = null;
      throw const AuthSessionExpiredException();
    }
    final currentSession = session;
    if (currentSession == null) {
      throw const AuthSessionExpiredException();
    }
    return request(currentSession.accessToken);
  }

  @override
  Future<LegalConsentStatus> fetchLegalConsentStatus() async {
    if (legalConsentStatusError != null) {
      throw legalConsentStatusError!;
    }
    return LegalConsentStatus(
      required: legalConsentRequired && recordedConsent == null,
      documentVersion: consentDocumentVersion,
    );
  }

  @override
  Future<LegalConsentStatus> recordLegalConsent(
    ConsentSelection selection,
  ) async {
    legalConsentRecordCount++;
    if (legalConsentRecordError != null) {
      throw legalConsentRecordError!;
    }
    recordedConsent = selection;
    return const LegalConsentStatus(
      required: false,
      documentVersion: consentDocumentVersion,
    );
  }

  @override
  Future<void> signOut() async {
    session = null;
  }

  @override
  Future<void> deleteAccount() async {
    deleteAccountCalled = true;
    if (deleteAccountError != null) {
      throw deleteAccountError!;
    }
    session = null;
  }
}

/// 테스트에서 Fake Sentence Api 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeSentenceApi implements SentenceApi {
  FakeSentenceApi({this.error, this.sentences = _defaultSentences});

  final Object? error;
  final List<PracticeSentence> sentences;
  int? lastLimit;
  String? lastCategory;

  @override
  Future<List<PracticeSentence>> fetchRecommendedSentences({
    int limit = 20,
    String? category,
  }) async {
    lastLimit = limit;
    lastCategory = category;
    if (error != null) {
      throw error!;
    }

    return sentences;
  }

  @override
  Future<PracticeSentence> fetchSentence(int sentenceId) async {
    return sentences.firstWhere(
      (sentence) => sentence.sentenceId == sentenceId,
    );
  }
}

const _defaultSentences = [
  PracticeSentence(
    sentenceId: 1,
    source: 'RECOMMENDED',
    text: '맛있겠다.',
    pronunciation: '마싣껟따.',
    romanization: 'ma-sit-kket-tta',
    romanizedPronunciation: 'ma-sit-kket-tta',
    translation: 'It looks delicious.',
    level: 'RECOMMENDED',
    category: 'Food',
    point: 'Final consonant linking and tense sound',
    score: 0,
    characters: [],
  ),
];

const _twoSentences = [
  ..._defaultSentences,
  PracticeSentence(
    sentenceId: 2,
    source: 'RECOMMENDED',
    text: '사진 찍어도 돼요?',
    pronunciation: '사진 찌거도 돼요?',
    translation: 'May I take a photo?',
    level: 'RECOMMENDED',
    category: 'Travel',
    point: 'Tense consonant in connected speech',
    score: 0,
    characters: [],
  ),
];

const _categorizedSentences = [
  PracticeSentence(
    sentenceId: 6,
    source: 'RECOMMENDED',
    text: '천천히 말씀해 주세요.',
    pronunciation: '천처니 말쓰매 주세요.',
    translation: 'Please speak slowly.',
    level: 'RECOMMENDED',
    category: 'Daily',
    point: 'Aspirated consonants and linking',
    score: 0,
    characters: [],
  ),
  PracticeSentence(
    sentenceId: 7,
    source: 'RECOMMENDED',
    text: '오늘 날씨가 좋아요.',
    pronunciation: '오늘 날씨가 조아요.',
    translation: 'The weather is nice today.',
    level: 'RECOMMENDED',
    category: 'Daily',
    point: 'Natural sentence rhythm',
    score: 0,
    characters: [],
  ),
  PracticeSentence(
    sentenceId: 15,
    source: 'RECOMMENDED',
    text: '친구를 만났어요.',
    pronunciation: '친구를 만나써요.',
    translation: 'I met a friend.',
    level: 'RECOMMENDED',
    category: 'Daily',
    point: 'Past tense ending with tense sound',
    score: 0,
    characters: [],
  ),
  ..._defaultSentences,
  PracticeSentence(
    sentenceId: 2,
    source: 'RECOMMENDED',
    text: '김치찌개 하나 주세요.',
    pronunciation: '김치찌개 하나 주세요.',
    translation: 'Please give me one kimchi stew.',
    level: 'RECOMMENDED',
    category: 'Food',
    point: 'Tense consonants in food ordering',
    score: 0,
    characters: [],
  ),
  PracticeSentence(
    sentenceId: 3,
    source: 'RECOMMENDED',
    text: '물 한 잔 주세요.',
    pronunciation: '물 한 잔 주세요.',
    translation: 'Please give me a glass of water.',
    level: 'RECOMMENDED',
    category: 'Food',
    point: 'Final consonant clarity in short requests',
    score: 0,
    characters: [],
  ),
  PracticeSentence(
    sentenceId: 4,
    source: 'RECOMMENDED',
    text: '커피가 뜨거워요.',
    pronunciation: '커피가 뜨거워요.',
    translation: 'The coffee is hot.',
    level: 'RECOMMENDED',
    category: 'Food',
    point: 'Aspirated consonant and rounded vowel practice',
    score: 0,
    characters: [],
  ),
  PracticeSentence(
    sentenceId: 8,
    source: 'RECOMMENDED',
    text: '지하철역이 어디예요?',
    pronunciation: '지하철려기 어디예요?',
    translation: 'Where is the subway station?',
    level: 'RECOMMENDED',
    category: 'Travel',
    point: 'Linking across compound words',
    score: 0,
    characters: [],
  ),
];

Finder _navigationLabel(String label) {
  return find.descendant(
    of: find.byType(NavigationBar),
    matching: find.text(label),
  );
}

Finder _verticalScrollable() {
  return find.byWidgetPredicate(
    (widget) =>
        widget is Scrollable && widget.axisDirection == AxisDirection.down,
  );
}

/// 로그인 수단을 누른 뒤 나타나는 동의 화면에서 필수 항목만 채우고 진행한다.
///
/// 선택 항목(마케팅)은 켜지 않는다. 인증 gate를 검증하는 테스트가 선택 동의 여부에
/// 영향을 받지 않아야 한다.
Future<void> agreeToConsent(WidgetTester tester) async {
  await tester.tap(find.byKey(const Key('consent-terms')));
  await tester.tap(find.byKey(const Key('consent-privacy')));
  await tester.pump();
  await tester.tap(find.byKey(const Key('consent-continue')));
  await tester.pumpAndSettle();
}

// 앱 workflow의 인증 gate와 갱신 토큰 만료 동작을 검증한다.
void main() {
  testWidgets('four consecutive wordmark taps keep review access hidden', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    for (var tap = 0; tap < 4; tap++) {
      await tester.tap(find.byKey(const Key('review-access-trigger')));
    }
    await tester.pump();

    expect(find.byKey(const Key('review-access-dialog')), findsNothing);
  });

  testWidgets('review wordmark tap sequence resets after three seconds', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    for (var tap = 0; tap < 4; tap++) {
      await tester.tap(find.byKey(const Key('review-access-trigger')));
    }
    await tester.pump(const Duration(seconds: 4));
    await tester.tap(find.byKey(const Key('review-access-trigger')));
    await tester.pump();

    expect(find.byKey(const Key('review-access-dialog')), findsNothing);
  });

  testWidgets('fifth consecutive wordmark tap opens review code login', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    for (var tap = 0; tap < 5; tap++) {
      await tester.tap(find.byKey(const Key('review-access-trigger')));
    }
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('review-access-dialog')), findsOneWidget);
    await tester.enterText(
      find.byKey(const Key('review-access-code-field')),
      '0000',
    );
    await tester.pump();
    await tester.tap(find.byKey(const Key('review-access-submit')));
    await tester.pumpAndSettle();

    expect(authService.reviewAccessCodes, ['0000']);
    expect(find.text('Practice by situation'), findsOneWidget);
  });

  testWidgets('review access failure hides server authentication details', (
    WidgetTester tester,
  ) async {
    final authService =
        FakeAppAuthService()
          ..error = const ApiException(
            'sensitive review hash mismatch',
            statusCode: 401,
          );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    for (var tap = 0; tap < 5; tap++) {
      await tester.tap(find.byKey(const Key('review-access-trigger')));
    }
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('review-access-code-field')),
      'wrong-review-code-with-enough-length',
    );
    await tester.pump();
    await tester.tap(find.byKey(const Key('review-access-submit')));
    await tester.pumpAndSettle();

    expect(
      find.text('Unable to verify review access. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('sensitive review hash mismatch'), findsNothing);
  });

  testWidgets(
    'iOS Apple login waits for consent and then uses Apple provider',
    (WidgetTester tester) async {
      debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
      try {
        final authService = FakeAppAuthService();
        await tester.pumpWidget(
          LingKoApp(
            pronunciationApi: FakePronunciationApi(),
            sentenceApi: FakeSentenceApi(),
            evaluationApi: FakeEvaluationApi(),
            practiceQuotaApi: FakePracticeQuotaApi(),
            authService: authService,
            audioRecorderService: FakeAudioRecorderService(),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Continue with Apple'), findsOneWidget);
        await tester.tap(find.text('Continue with Apple'));
        await tester.pumpAndSettle();
        expect(authService.appleSignInCalled, isFalse);

        await agreeToConsent(tester);

        expect(authService.appleSignInCalled, isTrue);
        expect(authService.signInCalled, isFalse);
        expect(authService.legalConsentRecordCount, 1);
      } finally {
        debugDefaultTargetPlatformOverride = null;
      }
    },
  );

  testWidgets('login provider buttons use balanced icon and label sizes', (
    WidgetTester tester,
  ) async {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    try {
      await tester.pumpWidget(
        LingKoApp(
          pronunciationApi: FakePronunciationApi(),
          sentenceApi: FakeSentenceApi(),
          evaluationApi: FakeEvaluationApi(),
          practiceQuotaApi: FakePracticeQuotaApi(),
          authService: FakeAppAuthService(),
          audioRecorderService: FakeAudioRecorderService(),
        ),
      );
      await tester.pumpAndSettle();

      final googleIconSlot = find.byKey(const Key('google-sign-in-icon-slot'));
      final appleIconSlot = find.byKey(const Key('apple-sign-in-icon-slot'));
      final googleLabel = find.byKey(const Key('google-sign-in-label'));
      final appleLabel = tester.widget<Text>(
        find.byKey(const Key('apple-sign-in-label')),
      );

      expect(tester.getSize(googleIconSlot), const Size.square(24));
      expect(tester.getSize(appleIconSlot), const Size.square(24));
      expect(
        tester.getCenter(googleIconSlot).dx,
        closeTo(tester.getCenter(appleIconSlot).dx, 0.01),
      );
      expect(
        tester.getCenter(googleLabel).dx,
        closeTo(
          tester.getCenter(find.byKey(const Key('apple-sign-in-label'))).dx,
          0.01,
        ),
      );
      expect(appleLabel.style?.fontSize, 15);
    } finally {
      debugDefaultTargetPlatformOverride = null;
    }
  });

  testWidgets('Android does not offer Apple login without web redirect setup', (
    WidgetTester tester,
  ) async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    try {
      await tester.pumpWidget(
        LingKoApp(
          pronunciationApi: FakePronunciationApi(),
          sentenceApi: FakeSentenceApi(),
          evaluationApi: FakeEvaluationApi(),
          practiceQuotaApi: FakePracticeQuotaApi(),
          authService: FakeAppAuthService(),
          audioRecorderService: FakeAudioRecorderService(),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Continue with Apple'), findsNothing);
    } finally {
      debugDefaultTargetPlatformOverride = null;
    }
  });

  testWidgets('App shows logo splash while restoring session', (
    WidgetTester tester,
  ) async {
    final restoreCompleter = Completer<AuthSession?>();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreCompleter: restoreCompleter),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pump();

    expect(find.byKey(const Key('splash-logo')), findsOneWidget);
    expect(find.text('Continue with Google'), findsNothing);

    restoreCompleter.complete(null);
    await tester.pumpAndSettle();

    expect(find.text('Continue with Google'), findsOneWidget);
  });

  testWidgets('App requires login before showing Home', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Continue with Google'), findsOneWidget);
    expect(find.byKey(const Key('google-sign-in-logo')), findsOneWidget);
    expect(find.text('Practice by situation'), findsNothing);

    await tester.tap(find.text('Continue with Google'));
    await tester.pumpAndSettle();

    // 로그인 수단을 눌러도 곧바로 인증하지 않는다. 계정이 만들어지기 전에
    // 필수 동의를 먼저 받는다.
    expect(authService.signInCalled, isFalse);
    await agreeToConsent(tester);

    expect(authService.signInCalled, isTrue);
    expect(authService.legalConsentRecordCount, 1);
    await tester.drag(find.byType(ListView).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Practice by situation'), findsOneWidget);
    expect(find.text('LingKo User'), findsNothing);
  });

  testWidgets('Restored session without current consent shows agreement gate', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService(
      restoreExistingSession: true,
      legalConsentRequired: true,
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Before you start'), findsOneWidget);
    expect(find.text('Practice by situation'), findsNothing);
    expect(authService.signInCalled, isFalse);

    await agreeToConsent(tester);

    expect(authService.signInCalled, isFalse);
    expect(authService.legalConsentRecordCount, 1);
    expect(find.text('Practice by situation'), findsOneWidget);
  });

  testWidgets('Consent save failure keeps restored session behind the gate', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService(
      restoreExistingSession: true,
      legalConsentRequired: true,
      legalConsentRecordError: StateError('offline'),
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();
    await agreeToConsent(tester);

    expect(find.text('Before you start'), findsOneWidget);
    expect(
      find.text('Could not save your agreement. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('Practice by situation'), findsNothing);
  });

  testWidgets('Consent status failure fails closed before Home', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(
          restoreExistingSession: true,
          legalConsentStatusError: StateError('offline'),
        ),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Before you start'), findsOneWidget);
    expect(
      find.text('Could not verify your agreement. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('Practice by situation'), findsNothing);
  });

  testWidgets('Expired session while saving consent returns to login', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(
          restoreExistingSession: true,
          legalConsentRequired: true,
          legalConsentRecordError: const AuthSessionExpiredException(),
        ),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();
    await agreeToConsent(tester);

    expect(find.text('Continue with Google'), findsOneWidget);
    expect(
      find.text('Your session expired. Please sign in again.'),
      findsOneWidget,
    );
    expect(find.text('Practice by situation'), findsNothing);
  });

  testWidgets('Expired refresh session returns the app to login', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(
          restoreExistingSession: true,
          expireAuthenticatedRequests: true,
        ),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Continue with Google'), findsOneWidget);
    expect(find.text('Practice by situation'), findsNothing);
  });

  testWidgets('Login anchors the wordmark to the top, not the middle', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    final screenHeight = tester.getSize(find.byType(Scaffold)).height;
    final wordmarkTop = tester.getTopLeft(find.text('LingKo')).dy;
    final buttonTop = tester.getTopLeft(find.text('Continue with Google')).dy;

    // 전체를 세로 중앙에 모으면 화면 크기마다 시작 위치가 달라져 첫인상이 흔들린다.
    // 워드마크는 상단에 붙고, 로그인 수단은 손이 닿는 아래쪽에 있어야 한다.
    expect(wordmarkTop, lessThan(screenHeight * 0.2));
    expect(buttonTop, greaterThan(screenHeight * 0.5));
  });

  testWidgets('Login hides authentication error details', (
    WidgetTester tester,
  ) async {
    final authService =
        FakeAppAuthService()..error = 'sensitive provider response';
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Continue with Google'));
    await tester.pumpAndSettle();
    await agreeToConsent(tester);

    expect(
      find.text('Unable to sign in with Google. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('sensitive provider response'), findsNothing);
  });

  testWidgets('LingKo prototype opens recommended sentences', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    final speechService = FakeSentenceSpeechService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
        sentenceSpeechService: speechService,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('LingKo'), findsOneWidget);
    expect(find.text('Practice by situation'), findsOneWidget);
    expect(find.text('맛있겠다.'), findsOneWidget);

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();

    expect(find.text('Practice'), findsWidgets);
    expect(find.text('Recommended'), findsNothing);
    expect(find.text('My sentence'), findsNothing);
    expect(find.text('Use this sentence'), findsNothing);
    final recommendedSentenceField = tester.widget<TextField>(
      find.byKey(const ValueKey('practice-sentence-field')),
    );
    expect(recommendedSentenceField.controller?.text, '맛있겠다');
    expect(find.text('Standard pronunciation ready'), findsNothing);
    expect(
      find.textContaining('Standard pronunciation updates automatically'),
      findsNothing,
    );
    expect(find.text('Pronunciation guide'), findsNothing);
    expect(find.text('마싣껟따'), findsOneWidget);
    expect(
      find.byKey(const ValueKey('practice-romanized-pronunciation')),
      findsOneWidget,
    );
    expect(find.text('ma-sit-kket-tta'), findsOneWidget);
    expect(find.text('Check standard pronunciation'), findsNothing);
    expect(find.text('Hide standard pronunciation'), findsNothing);
    expect(find.text('It looks delicious.'), findsNothing);
    expect(find.text('Final consonant linking and tense sound'), findsNothing);
    expect(find.text('1 / 10'), findsNothing);

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Record'), findsOneWidget);

    await _tapVisible(tester, find.text('Record'));
    expect(find.bySemanticsLabel('Stop and analyze'), findsOneWidget);
    expect(
      find.byKey(const ValueKey('recording-romanized-pronunciation')),
      findsOneWidget,
    );
    expect(find.text('마싣껟따.'), findsNothing);

    await _tapVisible(tester, find.bySemanticsLabel('Stop and analyze'));

    expect(find.text('Result'), findsOneWidget);
    // 등급 라벨 대신 한 줄 판정을 보여준다. 'Excellent' 같은 단어는
    // 점수와 같은 말을 두 번 하는 것이라 다음 행동을 알려주지 않는다.
    expect(find.text('Clear pronunciation.'), findsOneWidget);
    expect(find.text('91'), findsOneWidget);
    // 새 06 Result는 별도 섹션 제목 없이 카드 안 label로 표준 발음을 설명한다.
    expect(find.text('Pronunciation guide'), findsNothing);
    expect(find.text('Standard pronunciation'), findsOneWidget);
    expect(find.text('Recognized speech'), findsNothing);
    expect(find.text('사용자 발음'), findsNothing);
    expect(find.text('마싣껟따'), findsOneWidget);
    expect(
      find.byKey(const ValueKey('result-romanized-pronunciation')),
      findsOneWidget,
    );
    // 듣기(보통·느리게)는 Practice 화면에만 둔다. Result는 결과를 읽는 자리라
    // 재생 버튼을 다시 얹으면 다음에 할 일(다시 말하기)이 묻힌다.
    expect(
      find.byKey(const ValueKey('result-play-pronunciation-normal')),
      findsNothing,
    );
    await tester.scrollUntilVisible(
      find.text('Word feedback is unavailable'),
      300,
    );
    expect(find.text('Word feedback is unavailable'), findsOneWidget);
    expect(recorder.deletedPaths, ['/tmp/lingko-test.wav']);
  });

  testWidgets('Home lists weak sounds by syllable and opens their detail', (
    WidgetTester tester,
  ) async {
    final contentApi = FakePracticeContentApi(
      weakSounds: const [
        WeakSound(
          text: '씨',
          romanization: 'ssi',
          averageScore: 62,
          attemptCount: 8,
        ),
        WeakSound(
          text: '늘',
          romanization: 'neul',
          averageScore: 71,
          attemptCount: 4,
        ),
      ],
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        practiceContentApi: contentApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    // 취약 단위는 어절이 아니라 음절이다. 타일에는 한 글자만 오고, 점수는 그 음절이
    // 들어간 연습들의 평균이라 로마자와 한 줄에 묶어 측정값처럼 보이지 않게 한다.
    expect(find.text('Your weakest sounds'), findsOneWidget);
    expect(find.byKey(const ValueKey('home-weak-sound-씨')), findsOneWidget);
    expect(find.text('씨'), findsOneWidget);
    expect(find.text('ssi · 62'), findsOneWidget);
    expect(find.text('neul · 71'), findsOneWidget);

    // 도안 값을 고정한다. 눈으로만 보면 22px과 26px, 좌측/가운데 정렬을 구분하기
    // 어려워 조용히 어긋난 채로 남는다.
    final syllable = tester.widget<Text>(
      find.descendant(
        of: find.byKey(const ValueKey('home-weak-sound-씨')),
        matching: find.text('씨'),
      ),
    );
    expect(syllable.style?.fontSize, 26);
    expect(syllable.style?.color, const Color(0xFF96590C));
    final weakSoundTile = find.byKey(const ValueKey('home-weak-sound-씨'));
    final tileMaterial = tester.widget<Material>(
      find.descendant(of: weakSoundTile, matching: find.byType(Material)).first,
    );
    expect(tileMaterial.color, const Color(0xFFFDF5EA));
    final tileContainer = tester.widget<Container>(
      find
          .descendant(of: weakSoundTile, matching: find.byType(Container))
          .first,
    );
    final tileDecoration = tileContainer.decoration! as BoxDecoration;
    expect(
      (tileDecoration.border! as Border).top.color,
      const Color(0xFFE3C9A0),
    );
    final scoreLabel = tester.widget<Text>(find.text('ssi · 62'));
    expect(scoreLabel.style?.color, const Color(0xFF5C7386));
    final tileColumn = tester.widget<Column>(
      find.descendant(of: weakSoundTile, matching: find.byType(Column)).first,
    );
    expect(tileColumn.crossAxisAlignment, CrossAxisAlignment.center);

    await _tapVisible(tester, find.byKey(const ValueKey('home-weak-sound-씨')));

    // 상세는 어절이 아니라 그 음절 하나를 요청해야 한다.
    expect(contentApi.lastRequestedCharacter, '씨');
    expect(find.text('Sound'), findsOneWidget);
    expect(find.text('Average 62 across 8 tries'), findsOneWidget);
  });

  testWidgets('Home browses compact recommendations by situation', (
    WidgetTester tester,
  ) async {
    final sentenceApi = FakeSentenceApi(sentences: _categorizedSentences);
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: sentenceApi,
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(sentenceApi.lastLimit, 50);
    expect(sentenceApi.lastCategory, isNull);
    expect(find.text('Practice by situation'), findsOneWidget);
    // 카테고리는 파란 tint의 pill로 고르고 문장은 하나의 카드 안에서 훑는다.
    expect(find.text('Daily'), findsOneWidget);
    expect(find.text('천천히 말씀해 주세요.'), findsOneWidget);
    expect(find.text('Start Practice'), findsNothing);
    // 핸드오프의 문장 행은 카드 테두리에서 좌우 12px 안쪽에 놓인다.
    final sentenceRowLeft =
        tester.getTopLeft(find.byType(SentenceCard).first).dx;
    final sentenceTextLeft = tester.getTopLeft(find.text('천천히 말씀해 주세요.')).dx;
    expect(sentenceTextLeft - sentenceRowLeft, closeTo(12, 0.1));

    await tester.tap(find.byKey(const ValueKey('home-category-food')));
    await tester.pumpAndSettle();

    // 카테고리 전환은 탭 라벨과 목록 내용으로 확인한다.
    expect(find.text('Food'), findsOneWidget);
    expect(find.text('맛있겠다.'), findsOneWidget);
    expect(find.text('물 한 잔 주세요.'), findsOneWidget);
    expect(find.text('커피가 뜨거워요.'), findsNothing);
    expect(find.text('Show 1 more sentence'), findsOneWidget);

    await tester.drag(_verticalScrollable().first, const Offset(0, -250));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Show 1 more sentence'));
    await tester.pumpAndSettle();

    expect(find.text('커피가 뜨거워요.'), findsOneWidget);
    expect(find.text('Show fewer'), findsOneWidget);

    await tester.drag(_verticalScrollable().first, const Offset(0, -400));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Practice my own sentence'));
    await tester.pumpAndSettle();

    final customSentenceField = tester.widget<TextField>(
      find.byKey(const ValueKey('practice-sentence-field')),
    );
    expect(customSentenceField.controller?.text, isEmpty);
  });

  testWidgets('Home shows server-backed practice energy and ad callback', (
    WidgetTester tester,
  ) async {
    final serverTime = DateTime.parse('2026-06-17T12:17:42+09:00');
    final nextRefillAt = DateTime.parse('2026-06-17T13:00:00+09:00');
    final quotaApi = FakePracticeQuotaApi(
      quota: PracticeQuota(
        date: '2026-06-17',
        freeLimit: 5,
        freeUsed: 2,
        rewardedAvailable: 0,
        remainingPractices: 3,
        nextRefillAt: nextRefillAt,
        serverTime: serverTime,
      ),
    );
    var adRequests = 0;
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: quotaApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        onRequestPracticeReward: () async {
          adRequests++;
          quotaApi.quota = PracticeQuota(
            date: '2026-06-17',
            freeLimit: 5,
            freeUsed: 1,
            rewardedAvailable: 0,
            remainingPractices: 4,
            nextRefillAt: nextRefillAt,
            serverTime: serverTime.add(const Duration(seconds: 1)),
          );
        },
      ),
    );
    await tester.pumpAndSettle();

    expect(quotaApi.lastAccessToken, 'access.jwt');
    // 하단 탭에도 마이크 아이콘이 있어 캡슐 안으로 범위를 좁힌다.
    expect(
      find.descendant(
        of: find.byKey(const ValueKey('practice-energy-capsule')),
        matching: find.byIcon(Icons.mic_none_rounded),
      ),
      findsOneWidget,
    );
    expect(find.text('3/5'), findsOneWidget);
    expect(find.text('42:18'), findsOneWidget);
    expect(
      find.byKey(const ValueKey('practice-energy-ad-button')),
      findsOneWidget,
    );
    final capsule = find.byKey(const ValueKey('practice-energy-capsule'));
    // 충전 버튼의 히트 영역이 44px이라 캡슐 높이는 그보다 작아질 수 없다.
    expect(tester.getSize(capsule).height, lessThanOrEqualTo(46));
    // 수량·timer를 세로로 둔 capsule이 header의 wordmark를 밀어내지 않는 선을 고정한다.
    expect(tester.getSize(capsule).width, lessThanOrEqualTo(215));
    expect(tester.getTopRight(capsule).dx, closeTo(782, 0.1));
    expect(
      (tester.getCenter(find.text('LingKo')).dy - tester.getCenter(capsule).dy)
          .abs(),
      lessThan(4),
    );

    await tester.pump(const Duration(seconds: 1));
    expect(find.text('42:17'), findsOneWidget);

    await tester.tap(find.byKey(const ValueKey('practice-energy-ad-button')));
    await tester.pumpAndSettle();

    expect(adRequests, 1);
    expect(find.text('4/5'), findsOneWidget);
    expect(find.text('42:17'), findsOneWidget);
  });

  testWidgets(
    'earned AdMob reward is claimed once and updates energy immediately',
    (WidgetTester tester) async {
      final quotaApi = FakePracticeQuotaApi(
          quota: const PracticeQuota(
            date: '2026-06-17',
            freeLimit: 5,
            freeUsed: 2,
            rewardedAvailable: 0,
            remainingPractices: 3,
            nextRefillAt: null,
            serverTime: null,
          ),
        )
        ..rewardQuota = const PracticeQuota(
          date: '2026-06-17',
          freeLimit: 5,
          freeUsed: 2,
          rewardedAvailable: 1,
          remainingPractices: 4,
          nextRefillAt: null,
          serverTime: null,
        );
      final adService = FakePracticeRewardAdService();
      await tester.pumpWidget(
        LingKoApp(
          pronunciationApi: FakePronunciationApi(),
          sentenceApi: FakeSentenceApi(),
          evaluationApi: FakeEvaluationApi(),
          practiceQuotaApi: quotaApi,
          authService: FakeAppAuthService(restoreExistingSession: true),
          audioRecorderService: FakeAudioRecorderService(),
          practiceRewardAdService: adService,
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const ValueKey('practice-energy-ad-button')));
      await tester.pumpAndSettle();

      expect(adService.showCount, 1);
      expect(adService.lastCustomData, 'ssv-session-token');
      expect(quotaApi.sessionStatusFetchCount, 1);
      expect(find.text('4/5'), findsOneWidget);
    },
  );

  testWidgets('dismissed rewarded ad does not call the quota reward endpoint', (
    WidgetTester tester,
  ) async {
    final quotaApi = FakePracticeQuotaApi();
    final adService = FakePracticeRewardAdService(
      result: RewardedAdResult.dismissed,
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: quotaApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        practiceRewardAdService: adService,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey('practice-energy-ad-button')));
    await tester.pumpAndSettle();

    expect(adService.showCount, 1);
    expect(quotaApi.sessionStatusFetchCount, 0);
    expect(find.text('3/5'), findsOneWidget);
  });

  testWidgets(
    'Home refreshes energy when the server refill countdown expires',
    (WidgetTester tester) async {
      final serverTime = DateTime.parse('2026-06-17T12:59:58+09:00');
      final quotaApi = FakePracticeQuotaApi(
        quota: PracticeQuota(
          date: '2026-06-17',
          freeLimit: 5,
          freeUsed: 1,
          rewardedAvailable: 0,
          remainingPractices: 4,
          nextRefillAt: DateTime.parse('2026-06-17T13:00:00+09:00'),
          serverTime: serverTime,
        ),
      );
      await tester.pumpWidget(
        LingKoApp(
          pronunciationApi: FakePronunciationApi(),
          sentenceApi: FakeSentenceApi(),
          evaluationApi: FakeEvaluationApi(),
          practiceQuotaApi: quotaApi,
          authService: FakeAppAuthService(restoreExistingSession: true),
          audioRecorderService: FakeAudioRecorderService(),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('00:02'), findsOneWidget);
      final capsule = find.byKey(const ValueKey('practice-energy-capsule'));
      final alignmentFrame = find.byKey(
        const ValueKey('practice-energy-alignment-frame'),
      );
      // 큰 글자에서도 수량·timer·충전 버튼을 보존할 header 폭을 유지한다.
      expect(tester.getSize(alignmentFrame).width, 215);
      final refillWidth = tester.getSize(capsule).width;

      quotaApi.quota = const PracticeQuota(
        date: '2026-06-17',
        freeLimit: 5,
        freeUsed: 0,
        rewardedAvailable: 0,
        remainingPractices: 5,
        nextRefillAt: null,
        serverTime: null,
      );
      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();

      // 최대치에서는 timer 대신 MAX를 보여주고 광고 버튼을 숨긴다.
      expect(find.text('5/5'), findsOneWidget);
      expect(find.text('MAX'), findsOneWidget);
      expect(
        find.byKey(const ValueKey('practice-energy-ad-button')),
        findsNothing,
      );
      expect(tester.getSize(capsule).width, lessThan(refillWidth));
      expect(tester.getSize(capsule).width, lessThanOrEqualTo(120));
      expect(tester.getTopRight(capsule).dx, closeTo(782, 0.1));
      expect(
        tester.getTopRight(capsule).dx,
        tester.getTopRight(alignmentFrame).dx,
      );
      final trailingInset =
          tester.getTopRight(capsule).dx -
          tester.getTopRight(find.text('5/5')).dx;
      expect(trailingInset, inInclusiveRange(8, 12));
    },
  );

  testWidgets('recording success shows evaluation progress before Result', (
    WidgetTester tester,
  ) async {
    final createCompleter = Completer<EvaluationJob>();
    final evaluationApi =
        FakeEvaluationApi()..createJobCompleter = createCompleter;
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: evaluationApi,
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Record'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -120));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Record'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Record'));
    await tester.pump();
    // 중간 상태를 관찰해야 해서 pumpAndSettle을 쓰지 않는다. 위치만 먼저 확보한다.
    await tester.ensureVisible(find.bySemanticsLabel('Stop and analyze'));
    await tester.pump();
    await tester.tap(find.bySemanticsLabel('Stop and analyze'));
    await tester.pump();

    expect(find.byKey(const ValueKey('evaluation-progress')), findsOneWidget);
    // 단계 이름은 내부 용어가 아니라 사용자가 읽을 수 있는 문장이어야 한다.
    expect(find.text('Sending it for evaluation'), findsOneWidget);
    expect(find.text('Evaluation job'), findsNothing);
    expect(find.text('Result'), findsNothing);

    createCompleter.complete(
      const EvaluationJob(
        jobId: 'delayed-job',
        status: EvaluationJobStatus.succeeded,
        result: PracticeResult(
          overallScore: 91,
          gradeLabel: 'Excellent',
          summary: 'Clear pronunciation.',
          scoreBreakdown: PracticeScoreBreakdown(
            accuracy: 92,
            fluency: 90,
            completeness: 93,
          ),
          weakCharacters: [],
          characters: [],
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Result'), findsOneWidget);
    expect(find.text('Clear pronunciation.'), findsOneWidget);
  });

  testWidgets('evaluation job survives tab changes until Result is ready', (
    WidgetTester tester,
  ) async {
    final createCompleter = Completer<EvaluationJob>();
    final evaluationApi =
        FakeEvaluationApi()..createJobCompleter = createCompleter;
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: evaluationApi,
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Record'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -120));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Record'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Record'));
    await tester.pump();
    // 중간 상태를 관찰해야 해서 pumpAndSettle을 쓰지 않는다. 위치만 먼저 확보한다.
    await tester.ensureVisible(find.bySemanticsLabel('Stop and analyze'));
    await tester.pump();
    await tester.tap(find.bySemanticsLabel('Stop and analyze'));
    await tester.pump();

    expect(find.byType(NavigationBar), findsNothing);
    // 채점 화면을 빠져나가는 길은 하단 CTA가 아니라 좌상단 뒤로가기다.
    await tester.tap(find.byKey(const ValueKey('scoring-back')));
    await tester.pump();
    expect(find.text('Evaluation in progress'), findsOneWidget);

    await tester.tap(_navigationLabel('Practice'));
    await tester.pump();
    expect(find.byKey(const ValueKey('evaluation-progress')), findsOneWidget);

    await tester.tap(_navigationLabel('Home'));
    createCompleter.complete(
      const EvaluationJob(
        jobId: 'persistent-job',
        status: EvaluationJobStatus.succeeded,
        result: PracticeResult(
          overallScore: 88,
          gradeLabel: 'Great',
          summary: 'Stable pronunciation.',
          scoreBreakdown: PracticeScoreBreakdown(
            accuracy: 89,
            fluency: 86,
            completeness: 90,
          ),
          weakCharacters: [],
          characters: [],
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();
    expect(find.text('Result'), findsOneWidget);
    expect(find.text('88'), findsOneWidget);
  });

  testWidgets('Home hides quota error details', (WidgetTester tester) async {
    final quotaApi = FakePracticeQuotaApi()..error = 'sensitive quota response';
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: quotaApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();
    final semanticsHandle = tester.ensureSemantics();

    expect(find.byTooltip('Retry practice energy'), findsOneWidget);
    expect(
      find.bySemanticsLabel('Unable to load practice quota. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('sensitive quota response'), findsNothing);
    semanticsHandle.dispose();
  });

  testWidgets('Practice tab disables recording when quota is exhausted', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(
          quota: const PracticeQuota(
            date: '2026-06-17',
            freeLimit: 5,
            freeUsed: 5,
            rewardedAvailable: 0,
            remainingPractices: 0,
            nextRefillAt: null,
            serverTime: null,
          ),
        ),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();

    expect(find.text('No practices left'), findsOneWidget);
    await tester.tap(find.text('No practices left'));
    await tester.pumpAndSettle();

    expect(recorder.permissionChecks, 0);
    expect(find.bySemanticsLabel('Stop and analyze'), findsNothing);
  });

  testWidgets('Practice tab accepts a custom sentence', (
    WidgetTester tester,
  ) async {
    final api = FakePronunciationApi();
    final speechService = FakeSentenceSpeechService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: api,
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        sentenceSpeechService: speechService,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();

    expect(find.text('YOUR SENTENCE · TAP TO EDIT'), findsOneWidget);
    expect(find.text('Recommended'), findsNothing);
    expect(find.text('My sentence'), findsNothing);
    expect(find.text('Use this sentence'), findsNothing);
    expect(find.text('맛있겠다.'), findsNothing);
    expect(find.text('Record'), findsNothing);

    await tester.enterText(
      find.byKey(const ValueKey('practice-sentence-field')),
      '안녕하세요! @LingKo #1 (연습)_테스트-좋아요.😊₩',
    );
    await tester.pump();

    final customSentenceField = tester.widget<TextField>(
      find.byKey(const ValueKey('practice-sentence-field')),
    );
    expect(customSentenceField.controller?.text, '안녕하세요 LingKo 1 연습테스트좋아요');
    expect(api.lastText, isNull);
    expect(find.text('Record'), findsNothing);

    await tester.pump(const Duration(milliseconds: 699));
    expect(api.lastText, isNull);
    await tester.pump(const Duration(milliseconds: 1));
    await tester.pumpAndSettle();

    expect(api.lastText, '안녕하세요 LingKo 1 연습테스트좋아요');
    expect(find.text('Standard pronunciation ready'), findsNothing);
    expect(find.text('Record'), findsOneWidget);
    expect(find.text('서버 표준 발음'), findsOneWidget);
    expect(find.text('Practice with your own sentence.'), findsNothing);

    final normalButton = find.byKey(const ValueKey('play-sentence-normal'));
    final slowButton = find.byKey(const ValueKey('play-sentence-slow'));
    tester.widget<SecondaryButton>(normalButton).onPressed?.call();
    await tester.pump();

    expect(speechService.lastText, '안녕하세요 LingKo 1 연습테스트좋아요');
    expect(speechService.lastRate, SentenceSpeechRate.normal);

    tester.widget<SecondaryButton>(slowButton).onPressed?.call();
    await tester.pump();

    expect(speechService.lastRate, SentenceSpeechRate.slow);
  });

  testWidgets('Practice tab shows API errors for custom sentences', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(error: 'Validation failed'),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const ValueKey('practice-sentence-field')),
      '너무 긴 문장',
    );
    await tester.pump(const Duration(milliseconds: 700));
    await tester.pumpAndSettle();

    expect(
      find.text(
        'We could not prepare this sentence. Check the text and connection, then try again.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('Practice shows a safe message when device speech fails', (
    WidgetTester tester,
  ) async {
    final speechService =
        FakeSentenceSpeechService()..error = StateError('voice unavailable');
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        sentenceSpeechService: speechService,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    final normalButton = find.byKey(const ValueKey('play-sentence-normal'));
    tester.widget<SecondaryButton>(normalButton).onPressed?.call();
    await tester.pump();

    expect(find.text('Audio playback needs attention'), findsOneWidget);
    expect(find.textContaining('voice unavailable'), findsNothing);
  });

  testWidgets('Practice tab shows input length validation errors', (
    WidgetTester tester,
  ) async {
    final api = FakePronunciationApi(
      error: 'text must be 100 characters or fewer',
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: api,
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();

    final longText = '가' * 101;
    await tester.enterText(
      find.byKey(const ValueKey('practice-sentence-field')),
      longText,
    );
    await tester.pump(const Duration(milliseconds: 700));
    await tester.pumpAndSettle();

    expect(api.lastText, longText);
    expect(
      find.text(
        'We could not prepare this sentence. Check the text and connection, then try again.',
      ),
      findsOneWidget,
    );
    expect(find.text('Record'), findsNothing);
  });

  testWidgets('Practice ignores stale automatic pronunciation responses', (
    WidgetTester tester,
  ) async {
    final firstResponse = Completer<PracticeSentence>();
    final secondResponse = Completer<PracticeSentence>();
    final api = FakePronunciationApi(
      prepareHandler:
          (text) =>
              text == '첫 문장' ? firstResponse.future : secondResponse.future,
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: api,
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();
    final sentenceField = find.byKey(const ValueKey('practice-sentence-field'));

    await tester.enterText(sentenceField, '첫 문장');
    await tester.pump(const Duration(milliseconds: 700));
    expect(api.requestedTexts, ['첫 문장']);

    await tester.enterText(sentenceField, '둘째 문장');
    await tester.pump(const Duration(milliseconds: 700));
    expect(api.requestedTexts, ['첫 문장', '둘째 문장']);

    secondResponse.complete(
      const PracticeSentence(
        text: '둘째 문장',
        pronunciation: '둘째 표준 발음',
        translation: '',
        level: 'CUSTOM',
        category: 'Free practice',
        point: '',
        score: 0,
        characters: [],
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('둘째 표준 발음'), findsOneWidget);

    firstResponse.complete(
      const PracticeSentence(
        text: '첫 문장',
        pronunciation: '첫 표준 발음',
        translation: '',
        level: 'CUSTOM',
        category: 'Free practice',
        point: '',
        score: 0,
        characters: [],
      ),
    );
    await tester.pumpAndSettle();

    final fieldAfterResponses = tester.widget<TextField>(sentenceField);
    expect(fieldAfterResponses.controller?.text, '둘째 문장');
    expect(find.text('둘째 표준 발음'), findsOneWidget);
    expect(find.text('첫 표준 발음'), findsNothing);
  });

  testWidgets(
    'Selecting the same recommendation replaces an unfinished draft',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        LingKoApp(
          pronunciationApi: FakePronunciationApi(),
          sentenceApi: FakeSentenceApi(),
          evaluationApi: FakeEvaluationApi(),
          authService: FakeAppAuthService(restoreExistingSession: true),
          audioRecorderService: FakeAudioRecorderService(),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('맛있겠다.').first);
      await tester.pumpAndSettle();
      final sentenceField = find.byKey(
        const ValueKey('practice-sentence-field'),
      );
      await tester.enterText(sentenceField, '아직 작성 중');
      await tester.pump(const Duration(milliseconds: 100));

      await tester.tap(_navigationLabel('Home'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('맛있겠다.').first);
      await tester.pumpAndSettle();

      final selectedSentenceField = tester.widget<TextField>(sentenceField);
      expect(selectedSentenceField.controller?.text, '맛있겠다');
      expect(find.text('마싣껟따'), findsOneWidget);
    },
  );

  testWidgets('Practice removes symbols restored by the prepare response', (
    WidgetTester tester,
  ) async {
    final api = FakePronunciationApi(
      prepareHandler:
          (text) async => const PracticeSentence(
            text: '안녕하세요.!?',
            pronunciation: '안녕하세요.!?',
            translation: '',
            level: 'CUSTOM',
            category: 'Free practice',
            point: '',
            score: 0,
            characters: [],
          ),
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: api,
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();
    final sentenceField = find.byKey(const ValueKey('practice-sentence-field'));

    await tester.enterText(sentenceField, '안녕하세요.!?');
    await tester.pump(const Duration(milliseconds: 700));
    await tester.pumpAndSettle();

    expect(api.lastText, '안녕하세요');
    expect(tester.widget<TextField>(sentenceField).controller?.text, '안녕하세요');
    expect(find.text('안녕하세요.!?'), findsNothing);
  });

  testWidgets('Home shows loading, empty, and retryable error states', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        key: UniqueKey(),
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(sentences: const []),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );

    expect(find.byKey(const Key('splash-logo')), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.text('No recommended sentences yet'), findsOneWidget);

    await tester.pumpWidget(
      LingKoApp(
        key: UniqueKey(),
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(error: 'Cannot load sentences'),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Recommendations are unavailable'), findsOneWidget);
    expect(find.text('Practice by situation'), findsOneWidget);
  });

  testWidgets('Practice tab retries microphone permission after denial', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService(permissions: [false, true]);
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Record'));

    expect(
      find.text(
        'Microphone access is required. Allow access in device settings, then retry.',
      ),
      findsOneWidget,
    );
    expect(find.text('Retry microphone permission'), findsOneWidget);

    await tester.ensureVisible(find.text('Retry microphone permission'));
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -120));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Retry microphone permission'));
    await tester.pumpAndSettle();

    expect(recorder.permissionChecks, 2);
    expect(find.bySemanticsLabel('Stop and analyze'), findsOneWidget);
  });

  testWidgets('Practice tab keeps upload retry available after quota failure', (
    WidgetTester tester,
  ) async {
    final evaluationApi =
        FakeEvaluationApi()..error = 'Daily practice quota exceeded';
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: evaluationApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Record'));
    await _tapVisible(tester, find.bySemanticsLabel('Stop and analyze'));

    expect(
      find.text(
        'The evaluation did not finish. Retry with the saved recording when available.',
      ),
      findsOneWidget,
    );
    expect(find.text('Retry with this recording'), findsOneWidget);
    expect(find.text('Result'), findsNothing);
  });

  testWidgets('immersive recording can restart and cancel safely', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Record'));

    expect(find.byType(NavigationBar), findsNothing);
    expect(find.text('Restart'), findsOneWidget);
    await tester.tap(find.text('Restart'));
    await tester.pumpAndSettle();

    expect(recorder.cancelCount, 1);
    expect(recorder.startCount, 2);
    expect(find.byType(NavigationBar), findsNothing);
    await tester.tap(find.bySemanticsLabel('Cancel recording'));
    await tester.pumpAndSettle();

    expect(recorder.cancelCount, 2);
  });

  testWidgets('recording ring and waveform follow real input', (
    WidgetTester tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Record'));
    await tester.pump();

    CircularProgressIndicator ring() =>
        tester.widget<CircularProgressIndicator>(
          find.byType(CircularProgressIndicator).first,
        );

    // 시작 직후에는 진행이 0이어야 한다. 이전 구현은 0.38에 고정돼 있었다.
    expect(ring().value, 0.0);
    expect(find.text('0:00'), findsOneWidget);
    expect(find.text('/ 10 sec'), findsOneWidget);

    await tester.pump(const Duration(seconds: 5));
    expect(ring().value, closeTo(0.5, 0.05));
    expect(find.text('0:05'), findsOneWidget);

    // 무음일 때와 소리가 들어올 때 파형 안내가 달라져야 한다.
    expect(
      find.bySemanticsLabel('No sound is being picked up'),
      findsOneWidget,
    );
    recorder.amplitudeController.add(0.8);
    await tester.pump(const Duration(milliseconds: 200));
    expect(
      find.bySemanticsLabel('Microphone level 80 percent'),
      findsOneWidget,
    );

    // 표시한 상한에 도달하면 실제로 녹음이 멈춰야 한다.
    await tester.pump(const Duration(seconds: 6));
    await tester.pumpAndSettle();
    expect(find.text('/ 10 sec'), findsNothing);

    semantics.dispose();
  });

  testWidgets('Review shows recent practice history and opens retry', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Review'));
    await tester.pumpAndSettle();

    expect(find.text('Recent history'), findsOneWidget);
    expect(find.text('YOUR PROGRESS · LAST 1 TRY'), findsOneWidget);
    expect(find.text('Latest score'), findsOneWidget);
    expect(find.text('91'), findsWidgets);
    expect(find.text('맛있겠다.'), findsOneWidget);
    expect(
      find.byKey(const ValueKey('review-history-card-10')),
      findsOneWidget,
    );
    // 목록 행은 원문·로마자·시각만 담는다. 표준 발음은 상세에서 보여준다.
    expect(find.text('마싯게따.'), findsNothing);
    expect(find.text('2026-06-26 09:30'), findsOneWidget);

    await _tapVisible(
      tester,
      find.byKey(const ValueKey('review-history-card-10')),
    );

    expect(find.text('SCORE DETAILS'), findsOneWidget);
    expect(find.text('마싯게따.'), findsOneWidget);
    expect(find.text('Pronunciation by word'), findsOneWidget);
    expect(
      find.byKey(const ValueKey('review-romanized-pronunciation')),
      findsOneWidget,
    );
    expect(find.text('ma-sit-ge-tta'), findsWidgets);
    expect(find.text('Accuracy'), findsOneWidget);
    expect(find.text('Fluency'), findsOneWidget);
    expect(find.text('Completeness'), findsOneWidget);
    expect(find.text('88'), findsOneWidget);
    await _tapVisible(tester, find.byKey(const ValueKey('word-score-0')));
    expect(find.text('맛'), findsOneWidget);

    // 재연습은 상세 시트 안에서 시작한다. 버튼이 시트를 먼저 닫고 Practice로 넘긴다.
    await _tapVisible(
      tester,
      find.byKey(const ValueKey('review-retry-practice')),
    );

    expect(find.text('Practice'), findsWidgets);
    expect(find.text('Check standard pronunciation'), findsNothing);
    expect(find.text('마싣껟따'), findsOneWidget);
  });

  testWidgets('Review detail remains usable on a narrow large-text screen', (
    WidgetTester tester,
  ) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.8;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
      tester.platformDispatcher.clearTextScaleFactorTestValue();
    });

    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(_navigationLabel('Review'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('review-history-card-10')),
      260,
    );
    await _tapVisible(
      tester,
      find.byKey(const ValueKey('review-history-card-10')),
    );

    expect(find.text('SCORE DETAILS'), findsOneWidget);
    expect(find.text('Completeness'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Profile shows account and sign out returns to login', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService(restoreExistingSession: true);
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();

    expect(find.text('LingKo User'), findsOneWidget);
    expect(find.text('user@example.com'), findsOneWidget);
    // 핸드오프의 설정 행은 첫 아이콘부터 카드 테두리 안쪽 14px에서 시작한다.
    final savedRow = find.byKey(const ValueKey('profile-saved-sentences'));
    final savedIcon = find.descendant(
      of: savedRow,
      matching: find.byIcon(Icons.bookmark_border),
    );
    expect(
      tester.getTopLeft(savedIcon).dx - tester.getTopLeft(savedRow).dx,
      closeTo(14, 0.1),
    );
    await tester.scrollUntilVisible(find.text('Sign out'), 300);
    expect(find.text('Sign out'), findsOneWidget);

    await tester.tap(find.text('Sign out'));
    await tester.pumpAndSettle();

    expect(find.text('Continue with Google'), findsOneWidget);
    expect(find.text('Practice by situation'), findsNothing);
  });

  testWidgets('Profile requires confirmation before deleting the account', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService(restoreExistingSession: true);
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Delete account'), 300);
    await _tapVisible(tester, find.text('Delete account'));

    expect(find.text('Delete your account?'), findsOneWidget);
    expect(
      find.text(
        'Your profile, sessions, practice history, quota, and uploaded audio will be deleted.',
      ),
      findsOneWidget,
    );
    expect(authService.deleteAccountCalled, isFalse);

    await tester.tap(find.widgetWithText(FilledButton, 'Delete account'));
    await tester.pumpAndSettle();

    expect(authService.deleteAccountCalled, isTrue);
    expect(find.text('Continue with Google'), findsOneWidget);
  });

  testWidgets('Saved groups real sentences by category and removes bookmarks', (
    WidgetTester tester,
  ) async {
    final daily = _categorizedSentences.firstWhere(
      (sentence) => sentence.category == 'Daily',
    );
    final travel = _categorizedSentences.firstWhere(
      (sentence) => sentence.category == 'Travel',
    );
    const own = PracticeSentence(
      sentenceId: 99,
      source: 'CUSTOM',
      text: '직접 만든 문장',
      pronunciation: '직쩝 만든 문장',
      translation: 'My own sentence',
      level: 'CUSTOM',
      category: 'Free practice',
      point: 'Custom sentence',
      score: 0,
      characters: [],
    );
    final contentApi = FakePracticeContentApi(
      savedSentences: [daily, travel, own],
    );
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceContentApi: contentApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Saved sentences'));
    await tester.pumpAndSettle();

    expect(find.text('3 sentences'), findsOneWidget);
    expect(find.byKey(const ValueKey('saved-filter-all')), findsOneWidget);
    expect(find.text(daily.text), findsOneWidget);
    expect(find.text(travel.text), findsOneWidget);
    expect(find.text(own.text), findsOneWidget);

    await tester.tap(find.byKey(const ValueKey('saved-filter-travel')));
    await tester.pumpAndSettle();
    expect(find.text(travel.text), findsOneWidget);
    expect(find.text(daily.text), findsNothing);

    final travelRow = find.byKey(
      ValueKey('saved-sentence-${travel.sentenceId}'),
    );
    await tester.tap(
      find.descendant(of: travelRow, matching: find.byIcon(Icons.bookmark)),
    );
    await tester.pumpAndSettle();
    expect(contentApi.toggledSentenceIds, [travel.sentenceId]);
    expect(find.text(travel.text), findsNothing);

    await tester.tap(find.byKey(const ValueKey('saved-filter-own')));
    await tester.pumpAndSettle();
    expect(find.text(own.text), findsOneWidget);
    expect(find.text('2 sentences'), findsOneWidget);
  });

  testWidgets('Profile has no language settings section', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();

    // 언어 설정은 저장만 되고 읽는 코드가 없어 기능 전체를 제거했다.
    // 다시 살아나면 사용자에게 동작하지 않는 설정이 노출되므로 계약으로 고정한다.
    expect(find.text('Language preferences'), findsNothing);
    expect(find.text('Display language'), findsNothing);
    expect(find.text('Native language'), findsNothing);
    // 계정 정보와 세션 조작은 그대로 남아야 한다.
    expect(find.text('Sign out'), findsOneWidget);
    expect(find.text('Delete account'), findsOneWidget);
  });

  testWidgets(
    'Profile always exposes the legal documents and account deletion',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        LingKoApp(
          pronunciationApi: FakePronunciationApi(),
          sentenceApi: FakeSentenceApi(),
          evaluationApi: FakeEvaluationApi(),
          authService: FakeAppAuthService(restoreExistingSession: true),
          audioRecorderService: FakeAudioRecorderService(),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(_navigationLabel('Profile'));
      await tester.pumpAndSettle();

      // 가입할 때 동의한 문서를 나중에 다시 읽을 수 없으면 동의가 형식 절차가 된다.
      // 스토어 심사도 앱 안에서 정책에 닿을 수 있는지를 본다.
      expect(find.byKey(const ValueKey('profile-terms')), findsOneWidget);
      expect(find.byKey(const ValueKey('profile-privacy')), findsOneWidget);
      expect(find.byKey(const ValueKey('profile-ad-privacy')), findsOneWidget);
      expect(find.byKey(const ValueKey('profile-contact')), findsOneWidget);
      expect(find.text('Delete account'), findsOneWidget);
    },
  );

  testWidgets('Profile opens each legal document from its own row', (
    WidgetTester tester,
  ) async {
    final launcher = FakeLegalDocumentLauncher();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        legalDocumentLauncher: launcher,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey('profile-terms')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('profile-privacy')));
    await tester.pumpAndSettle();

    // 각 행이 자기 문서를 연다. 두 행이 같은 문서를 열면 사용자가 동의한 내용과
    // 다른 문서를 읽게 된다.
    expect(launcher.opened, [
      ConsentDocument.termsOfService,
      ConsentDocument.privacyPolicy,
    ]);
  });

  testWidgets('Profile opens UMP ad privacy options when ads are configured', (
    WidgetTester tester,
  ) async {
    final adService = FakePracticeRewardAdService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        practiceRewardAdService: adService,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('profile-ad-privacy')));
    await tester.pumpAndSettle();

    expect(adService.privacyOptionsCount, 1);
  });

  testWidgets('Profile reports when the document cannot be opened', (
    WidgetTester tester,
  ) async {
    final launcher = FakeLegalDocumentLauncher(succeeds: false);
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        legalDocumentLauncher: launcher,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();

    // 브라우저를 열 수 없는 기기가 있다. 아무 반응이 없으면 버튼이 고장난 것으로 보인다.
    await tester.tap(find.byKey(const ValueKey('profile-terms')));
    await tester.pump();
    expect(find.text('Could not open the document.'), findsOneWidget);
  });

  testWidgets('Profile keeps unconnected settings rows untappable', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();

    // 기본 test build에는 광고 ID와 문의 callback이 없다. 눌러도 아무 일이 없는 행은
    // 고장으로 보이므로, 설정이 제공되기 전에는 비활성이어야 한다.
    for (final key in const [
      ValueKey('profile-ad-privacy'),
      ValueKey('profile-contact'),
    ]) {
      final row = tester.widget<InkWell>(
        find.descendant(of: find.byKey(key), matching: find.byType(InkWell)),
      );
      expect(row.onTap, isNull, reason: '$key should stay untappable');
    }
  });

  testWidgets('leaving Practice tab deletes a stopped temporary recording', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Record'));
    await _tapVisible(tester, find.bySemanticsLabel('Stop and analyze'));

    expect(recorder.deletedPaths, ['/tmp/lingko-test.wav']);
  });

  testWidgets('changing the practice sentence cleans up existing recording', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(sentences: _twoSentences),
        evaluationApi:
            FakeEvaluationApi()..error = 'Temporary evaluation failure',
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Record'));
    await _tapVisible(tester, find.bySemanticsLabel('Stop and analyze'));

    // 채점 화면에는 스크롤할 내용이 없다. 좌상단 뒤로가기로 빠져나간다.
    await _tapVisible(tester, find.byKey(const ValueKey('scoring-back')));
    await tester.tap(find.byKey(const ValueKey('home-category-travel')));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('사진 찍어도 돼요?'),
      300,
      scrollable: _verticalScrollable().first,
    );
    await tester.tap(find.text('사진 찍어도 돼요?'));
    await tester.pumpAndSettle();

    expect(recorder.deletedPaths, ['/tmp/lingko-test.wav']);
  });

  testWidgets('cleanup failure after successful upload does not block result', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(
          deleteError: FileSystemException('delete failed'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await _tapVisible(tester, find.text('Record'));
    await _tapVisible(tester, find.bySemanticsLabel('Stop and analyze'));

    expect(find.text('Result'), findsOneWidget);
    expect(find.text('Clear pronunciation.'), findsOneWidget);
  });

  testWidgets('weak character opens its pronunciation guide', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '나',
      score: 55,
      scoreStatus: ScoreStatus.available,
      note: 'Focus on tongue placement',
      kind: 'TONGUE',
    );

    await tester.pumpWidget(
      MaterialApp(home: Scaffold(body: ResultTile(result: character))),
    );

    expect(find.text('55'), findsOneWidget);
    await tester.tap(find.byType(ResultTile));
    await tester.pumpAndSettle();

    // 가이드 URL이 없으면 라벨 붙은 도해 대신 로컬 대체 도해만 그린다.
    // 시트가 어느 음절을 설명하는지는 머리말로 알린다.
    expect(find.text('나'), findsWidgets);
    expect(find.text('TONGUE · SIDE VIEW'), findsNothing);
    // 가이드 종류만 되풀이하는 자동 생성 note와 미디어 형식 설명은 화면 공간만
    // 차지하므로 표시하지 않는다. 사용자가 볼 것은 가이드 자체다.
    expect(find.text('Focus on tongue placement'), findsNothing);
    expect(find.textContaining('did not provide guide media'), findsNothing);
  });

  testWidgets('guide sheet keeps a real articulation hint', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '싯',
      score: 55,
      note: 'Touch the ridge behind your teeth, then release slowly.',
      kind: 'TONGUE',
    );

    await tester.pumpWidget(
      MaterialApp(home: Scaffold(body: ResultTile(result: character))),
    );
    await tester.tap(find.byType(ResultTile));
    await tester.pumpAndSettle();

    // 자동 생성 문구가 아닌 실제 조음 힌트는 그대로 노출되어야 한다.
    expect(
      find.text('Touch the ridge behind your teeth, then release slowly.'),
      findsOneWidget,
    );
  });

  testWidgets('guide sheet stacks the lips and tongue guides together', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '마',
      score: 0,
      note: 'Stable vowel shape',
      kind: 'BOTH',
      guideStatus: 'AVAILABLE',
      mouthGuideUrl: 'https://guides/mouth/vowel-a.png',
      tongueGuideUrl: 'https://guides/tongue/m.png',
    );

    await tester.pumpWidget(
      // showGuideSheet과 같은 조건으로 띄운다. 도해 두 장이 세로로 쌓여
      // 작은 화면에서는 넘치므로 실제 시트도 스크롤 안에 둔다.
      const MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(child: GuideSheet(result: character)),
        ),
      ),
    );

    // 입 모양과 혀 위치는 같은 소리의 두 단면이라 탭으로 번갈아 보지 않고
    // 위아래로 쌓는다. 눈만 옮기면 둘의 관계를 볼 수 있다.
    final images = tester.widgetList<Image>(find.byType(Image)).toList();
    expect(images, hasLength(2));
    expect(images.map((image) => (image.image as NetworkImage).url).toList(), [
      character.mouthGuideUrl,
      character.tongueGuideUrl,
    ]);
    expect(find.text('LIPS · FRONT VIEW'), findsOneWidget);
    expect(find.text('TONGUE · SIDE VIEW'), findsOneWidget);
    // 탭 전환은 더 이상 없다.
    expect(find.byKey(const ValueKey('guide-tab-tongue-guide')), findsNothing);
  });

  testWidgets('guide sheet omits the tab bar when only one guide exists', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '마',
      score: 0,
      note: 'Stable vowel shape',
      kind: 'MOUTH',
      guideStatus: 'AVAILABLE',
      mouthGuideUrl: 'https://guides/mouth/vowel-a.png',
    );

    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: GuideSheet(result: character))),
    );

    // 선택지가 하나뿐이면 전환 UI는 공간만 차지한다.
    expect(find.byKey(const ValueKey('guide-tab-mouth-guide')), findsNothing);
    expect(tester.widgetList<Image>(find.byType(Image)), hasLength(1));
  });

  testWidgets('guide sheet hugs its content at the bottom of the screen', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '마',
      score: 0,
      note: 'Stable vowel shape',
      kind: 'MOUTH',
      guideStatus: 'AVAILABLE',
      mouthGuideUrl: 'https://guides/mouth/vowel-a.png',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder:
                (context) => TextButton(
                  onPressed: () => showGuideSheet(context, character),
                  child: const Text('open guide'),
                ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open guide'));
    await tester.pumpAndSettle();

    final screenHeight =
        tester.view.physicalSize.height / tester.view.devicePixelRatio;
    final sheet = tester.getRect(find.byType(GuideSheet));

    // 시트는 화면 바닥에 붙는다. 도해가 위로 뜨고 아래에 빈 공간이 남으면
    // 손이 닿는 자리가 아무것도 없는 여백이 된다.
    expect(sheet.bottom, closeTo(screenHeight, 0.1));
    // 높이는 비율로 고정하지 않고 내용에 맞춘다. 상한(90%)에 닿지 않아야 한다.
    expect(sheet.height, lessThan(screenHeight * 0.9));
  });

  testWidgets('guide sheet renders MP4 URLs as video media', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '마',
      score: 82,
      note: 'Watch the complete articulation movement.',
      kind: 'MOUTH',
      guideStatus: 'AVAILABLE',
      mouthGuideUrl: 'https://guides/mouth/ma.mp4',
    );

    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: GuideSheet(result: character))),
    );

    expect(
      find.byKey(const ValueKey('guide-video-lips-front-view')),
      findsOneWidget,
    );
    expect(find.byType(Image), findsNothing);
  });

  testWidgets('guides remain available when syllable scores are unavailable', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '마',
      score: 0,
      scoreStatus: ScoreStatus.unavailable,
      note: 'Stable vowel shape',
      kind: 'TONGUE',
      guideStatus: 'AVAILABLE',
      mouthGuideUrl: 'https://guides/mouth/vowel-a.png',
      tongueGuideUrl: 'https://guides/tongue/m.png',
    );
    const sentence = PracticeSentence(
      text: '마',
      pronunciation: '마',
      translation: '',
      level: 'CUSTOM',
      category: 'Free practice',
      point: '',
      score: 0,
      characters: [],
    );
    const result = PracticeResult(
      overallScore: 82,
      gradeLabel: 'Good',
      summary: 'Keep practicing.',
      characterScoreStatus: ScoreStatus.unavailable,
      scoreBreakdown: PracticeScoreBreakdown(
        accuracy: 82,
        fluency: 80,
        completeness: 84,
      ),
      weakCharacters: [],
      characters: [character],
      words: [
        PracticeWordResult(
          position: 0,
          text: '마',
          scoreStatus: ScoreStatus.unavailable,
          syllables: [character],
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: ResultScreen(
            sentence: sentence,
            result: result,
            onTryAgain: () {},
          ),
        ),
      ),
    );

    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('word-score-0')),
      300,
    );
    expect(find.text('Pronunciation by word'), findsOneWidget);
    // 점수가 없을 때도 첫 어절의 guide는 즉시 사용할 수 있어야 한다.
    expect(find.byKey(const ValueKey('syllable-guide-0-0')), findsOneWidget);
    await _tapVisible(tester, find.byKey(const ValueKey('word-score-0')));
    expect(find.byKey(const ValueKey('syllable-guide-0-0')), findsOneWidget);
    expect(find.text('—'), findsWidgets);

    await tester.ensureVisible(
      find.byKey(const ValueKey('syllable-guide-0-0')),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('syllable-guide-0-0')));
    await tester.pumpAndSettle();

    // 가이드 종류를 제목으로 되풀이하지 않는다. 시트가 열렸는지로 확인한다.
    expect(find.byType(GuideSheet), findsOneWidget);
  });

  testWidgets('Result groups word scores and switches guide-only syllables', (
    WidgetTester tester,
  ) async {
    const kim = CharacterResult(
      character: '김',
      score: 0,
      scoreStatus: ScoreStatus.unavailable,
      note: 'Keep the final consonant clear.',
      kind: 'TONGUE',
    );
    const ha = CharacterResult(
      character: '하',
      score: 0,
      scoreStatus: ScoreStatus.unavailable,
      note: 'Open the mouth naturally.',
      kind: 'MOUTH',
      mouthGuideUrl: 'https://guides/mouth/ha.png',
    );
    const sentence = PracticeSentence(
      text: '김치찌개 하나 주세요',
      pronunciation: '김치찌개 하나 주세요',
      translation: '',
      level: 'CUSTOM',
      category: 'Free practice',
      point: '',
      score: 0,
      characters: [],
    );
    const result = PracticeResult(
      overallScore: 84,
      gradeLabel: 'Good',
      summary: 'Keep practicing.',
      wordScoreStatus: ScoreStatus.available,
      scoreBreakdown: PracticeScoreBreakdown(
        accuracy: 84,
        fluency: 82,
        completeness: 86,
      ),
      weakCharacters: [],
      characters: [kim, ha],
      words: [
        PracticeWordResult(
          position: 0,
          text: '김치찌개',
          score: 82,
          scoreStatus: ScoreStatus.available,
          syllables: [kim],
        ),
        PracticeWordResult(
          position: 1,
          text: '하나',
          score: 91,
          scoreStatus: ScoreStatus.available,
          syllables: [ha],
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: ResultScreen(
            sentence: sentence,
            result: result,
            onTryAgain: () {},
          ),
        ),
      ),
    );

    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('word-score-0')),
      300,
    );
    expect(find.text('Pronunciation by word'), findsOneWidget);
    expect(find.text('김치찌개'), findsWidgets);
    expect(
      find.descendant(
        of: find.byKey(const ValueKey('word-score-0')),
        matching: find.text('82'),
      ),
      findsOneWidget,
    );
    // 음절은 어절을 펼쳐야 나온다. 접힌 상태가 기본이다.
    await _tapVisible(tester, find.byKey(const ValueKey('word-score-0')));
    expect(find.byKey(const ValueKey('syllable-guide-0-0')), findsOneWidget);

    await tester.tap(find.byKey(const ValueKey('word-score-1')));
    await tester.pump();

    expect(find.byKey(const ValueKey('syllable-guide-0-0')), findsNothing);
    expect(find.byKey(const ValueKey('syllable-guide-1-0')), findsOneWidget);
    expect(
      find.descendant(
        of: find.byKey(const ValueKey('word-score-1')),
        matching: find.text('91'),
      ),
      findsOneWidget,
    );

    await tester.tap(find.byKey(const ValueKey('syllable-guide-1-0')));
    await tester.pumpAndSettle();
    expect(find.byType(GuideSheet), findsOneWidget);
  });

  testWidgets('authenticated shell exposes Home Practice Review Profile tabs', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(_navigationLabel('Home'), findsOneWidget);
    expect(_navigationLabel('Practice'), findsOneWidget);
    expect(_navigationLabel('Review'), findsOneWidget);
    expect(_navigationLabel('Profile'), findsOneWidget);
  });

  testWidgets('Result shows word scores and retries whole sentence', (
    WidgetTester tester,
  ) async {
    const sentence = PracticeSentence(
      sentenceId: 1,
      source: 'RECOMMENDED',
      text: '저는 커피를 좋아해요.',
      pronunciation: '저는 커피를 조아해요.',
      translation: 'I like coffee.',
      level: 'RECOMMENDED',
      category: 'Daily',
      point: 'Connected speech',
      score: 0,
      characters: [],
    );
    const result = PracticeResult(
      overallScore: 87,
      gradeLabel: 'Great',
      summary: 'Clear and natural pronunciation.',
      characterScoreStatus: ScoreStatus.available,
      scoreBreakdown: PracticeScoreBreakdown(
        accuracy: 90,
        fluency: 85,
        completeness: 88,
      ),
      weakCharacters: [
        CharacterResult(
          character: '피',
          score: 62,
          note: 'Release more air.',
          kind: 'MOUTH',
        ),
      ],
      characters: [
        CharacterResult(
          character: '저',
          score: 94,
          note: 'Clear.',
          kind: 'MOUTH',
        ),
        CharacterResult(
          character: '피',
          score: 62,
          note: 'Release more air.',
          kind: 'MOUTH',
        ),
        CharacterResult(
          character: '는',
          score: 82,
          note: 'Stable.',
          kind: 'TONGUE',
        ),
        CharacterResult(
          character: '커',
          score: 66,
          note: 'Keep practicing.',
          kind: 'MOUTH',
        ),
        CharacterResult(
          character: '를',
          score: 42,
          note: 'Needs improvement.',
          kind: 'TONGUE',
        ),
        CharacterResult(
          character: '요',
          score: 0,
          note: 'Score unavailable.',
          kind: 'NONE',
          scoreStatus: ScoreStatus.unavailable,
        ),
      ],
      words: [
        PracticeWordResult(
          position: 0,
          text: '저는',
          score: 94,
          scoreStatus: ScoreStatus.available,
          syllables: [
            CharacterResult(
              character: '저',
              score: 0,
              scoreStatus: ScoreStatus.unavailable,
              note: 'Clear.',
              kind: 'MOUTH',
            ),
          ],
        ),
        PracticeWordResult(
          position: 1,
          text: '커피를',
          score: 62,
          scoreStatus: ScoreStatus.available,
          syllables: [
            CharacterResult(
              character: '피',
              score: 0,
              scoreStatus: ScoreStatus.unavailable,
              note: 'Release more air.',
              kind: 'MOUTH',
            ),
          ],
        ),
      ],
    );
    var retried = false;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: ResultScreen(
            sentence: sentence,
            result: result,
            onTryAgain: () => retried = true,
            isSaved: false,
            onToggleSaved: () {},
          ),
        ),
      ),
    );

    final resultBounds = tester.getRect(find.byType(ResultScreen));
    final titleCenter = tester.getCenter(find.text('Result'));
    final bookmarkBounds = tester.getRect(
      find.byKey(const ValueKey('result-save-sentence')),
    );
    expect(titleCenter.dx, closeTo(resultBounds.center.dx, 0.1));
    expect(bookmarkBounds.right, closeTo(resultBounds.right - 18, 0.1));

    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('word-score-0')),
      300,
    );

    expect(find.text('Pronunciation by word'), findsOneWidget);
    expect(find.text('Pronunciation guide'), findsNothing);
    expect(find.text('Standard pronunciation'), findsOneWidget);
    final overallScore = tester.widget<Text>(find.text('87'));
    expect(overallScore.style?.color, const Color(0xFF245F9B));
    expect(find.text('저는'), findsWidgets);
    expect(find.text('커피를'), findsWidgets);
    expect(
      find.descendant(
        of: find.byKey(const ValueKey('word-score-0')),
        matching: find.text('94'),
      ),
      findsOneWidget,
    );
    final highScore = tester.widget<Text>(
      find.descendant(
        of: find.byKey(const ValueKey('word-score-0')),
        matching: find.text('94'),
      ),
    );
    final mediumScore = tester.widget<Text>(
      find.descendant(
        of: find.byKey(const ValueKey('word-score-1')),
        matching: find.text('62'),
      ),
    );
    expect(highScore.style?.color, const Color(0xFF245F9B));
    expect(mediumScore.style?.color, const Color(0xFF96590C));
    final firstWordTopLeft = tester.getTopLeft(
      find.byKey(const ValueKey('word-score-0')),
    );
    final secondWordTopLeft = tester.getTopLeft(
      find.byKey(const ValueKey('word-score-1')),
    );
    expect(secondWordTopLeft.dx, closeTo(firstWordTopLeft.dx, 0.1));
    expect(secondWordTopLeft.dy, greaterThan(firstWordTopLeft.dy));
    expect(
      find.descendant(
        of: find.byKey(const ValueKey('word-score-1')),
        matching: find.text('62'),
      ),
      findsOneWidget,
    );
    // 음절은 어절을 펼쳐야 나온다. 접힌 상태가 기본이다.
    await _tapVisible(tester, find.byKey(const ValueKey('word-score-0')));
    expect(find.byKey(const ValueKey('syllable-guide-0-0')), findsOneWidget);
    expect(find.text('Practice 저 Again'), findsNothing);
    expect(find.text('Practice Weak Sound'), findsNothing);

    await _tapVisible(
      tester,
      find.byKey(const ValueKey('retry-whole-sentence')),
    );

    expect(retried, isTrue);
  });

  testWidgets('Result remains usable on a small screen with large text', (
    WidgetTester tester,
  ) async {
    const result = PracticeResult(
      overallScore: 76,
      gradeLabel: 'Good',
      summary:
          'The sentence is complete, and a few connected sounds need another clear attempt.',
      recognizedText: '저는 커피를 좋아해요.',
      characterScoreStatus: ScoreStatus.available,
      scoreBreakdown: PracticeScoreBreakdown(
        accuracy: 78,
        fluency: 72,
        completeness: 84,
      ),
      weakCharacters: [],
      characters: [
        CharacterResult(
          character: '저',
          score: 92,
          note: 'Clear.',
          kind: 'MOUTH',
        ),
        CharacterResult(
          character: '는',
          score: 78,
          note: 'Good.',
          kind: 'TONGUE',
        ),
        CharacterResult(
          character: '커',
          score: 61,
          note: 'Practice.',
          kind: 'MOUTH',
        ),
      ],
    );
    const sentence = PracticeSentence(
      text: '저는 커피를 좋아하고 매일 아침 따뜻하게 마셔요.',
      pronunciation: '저는 커피를 조아하고 매일 아침 따뜨타게 마셔요.',
      translation: 'I like coffee and drink it warm every morning.',
      level: 'RECOMMENDED',
      category: 'Daily',
      point: 'Connected speech',
      score: 0,
      characters: [],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(
            size: Size(320, 640),
            textScaler: TextScaler.linear(2),
          ),
          child: Scaffold(
            body: ResultScreen(
              sentence: sentence,
              result: result,
              onTryAgain: () {},
            ),
          ),
        ),
      ),
    );

    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('retry-whole-sentence')),
      240,
      maxScrolls: 20,
    );

    expect(find.text('Practice this sentence again'), findsOneWidget);
  });

  testWidgets('main tabs remain usable on a small screen with large text', (
    WidgetTester tester,
  ) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.8;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
      tester.platformDispatcher.clearTextScaleFactorTestValue();
    });

    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(_navigationLabel('Home'), findsOneWidget);
    expect(find.textContaining('Good morning'), findsOneWidget);

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();
    expect(find.text('Practice'), findsWidgets);

    await tester.tap(_navigationLabel('Review'));
    await tester.pumpAndSettle();
    expect(find.text('Review'), findsWidgets);

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();
    expect(find.text('Profile'), findsWidgets);
  });
}
