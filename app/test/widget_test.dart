// 파일 의도: widget test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lingko_app/api/evaluation_api.dart';
import 'package:lingko_app/api/practice_quota_api.dart';
import 'package:lingko_app/api/pronunciation_api.dart';
import 'package:lingko_app/api/sentence_api.dart';
import 'package:lingko_app/api/user_preferences_api.dart';
import 'package:lingko_app/app/lingko_app.dart';
import 'package:lingko_app/models/auth_session.dart';
import 'package:lingko_app/models/evaluation_job.dart';
import 'package:lingko_app/models/practice_history.dart';
import 'package:lingko_app/models/practice_quota.dart';
import 'package:lingko_app/models/practice_result.dart';
import 'package:lingko_app/models/practice_sentence.dart';
import 'package:lingko_app/models/user_preferences.dart';
import 'package:lingko_app/screens/result_screen.dart';
import 'package:lingko_app/services/audio_recorder_service.dart';
import 'package:lingko_app/services/app_auth_service.dart';
import 'package:lingko_app/services/sentence_speech_service.dart';
import 'package:lingko_app/widgets/guide_sheet.dart';
import 'package:lingko_app/widgets/result_tile.dart';
import 'package:lingko_app/widgets/shared_widgets.dart';

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
  PracticeHistory history = const PracticeHistory(
    items: [
      PracticeHistoryItem(
        evaluationLogId: 10,
        sentenceId: 1,
        source: 'RECOMMENDED',
        originalText: '맛있겠다.',
        standardPronunciation: '마싯게따.',
        recognizedText: '마싯게따.',
        overallScore: 91,
        gradeLabel: 'Excellent',
        summary: 'Clear pronunciation.',
        scoreBreakdown: PracticeScoreBreakdown(
          accuracy: 92,
          fluency: 90,
          completeness: 93,
        ),
        characters: [],
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
        characterScoreStatus: 'UNAVAILABLE',
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

/// 테스트에서 Fake User Preferences Api 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeUserPreferencesApi implements UserPreferencesApi {
  UserPreferences preferences = const UserPreferences(
    displayLanguage: 'ko',
    nativeLanguage: 'en',
    targetLevel: LearningLevel.beginner2,
  );
  UserPreferences? lastUpdatedPreferences;
  String? lastAccessToken;
  Object? error;

  @override
  Future<UserPreferences> fetchPreferences({
    required String accessToken,
  }) async {
    lastAccessToken = accessToken;

    if (error != null) {
      throw error!;
    }

    return preferences;
  }

  @override
  Future<UserPreferences> updatePreferences({
    required String accessToken,
    required UserPreferences preferences,
  }) async {
    lastAccessToken = accessToken;
    lastUpdatedPreferences = preferences;

    if (error != null) {
      throw error!;
    }

    this.preferences = preferences;
    return preferences;
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
      resetAt: null,
    ),
  });

  PracticeQuota quota;
  String? lastAccessToken;
  Object? error;

  @override
  Future<PracticeQuota> fetchTodayQuota({required String accessToken}) async {
    lastAccessToken = accessToken;

    if (error != null) {
      throw error!;
    }

    return quota;
  }
}

/// 테스트에서 Fake Audio Recorder 서비스 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeAudioRecorderService implements AudioRecorderService {
  FakeAudioRecorderService({
    List<bool> permissions = const [true],
    this.deleteError,
  }) : _permissions = [...permissions];

  final List<bool> _permissions;
  final Object? deleteError;
  bool started = false;
  bool cancelled = false;
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
    return '/tmp/lingko-test.wav';
  }

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
  });

  final bool restoreExistingSession;
  final Completer<AuthSession?>? restoreCompleter;
  final bool expireAuthenticatedRequests;
  bool signInCalled = false;
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

  @override
  Future<List<PracticeSentence>> fetchRecommendedSentences({
    int limit = 20,
    String? category,
  }) async {
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
    pronunciation: '마싯게따.',
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

Finder _navigationLabel(String label) {
  return find.descendant(
    of: find.byType(NavigationBar),
    matching: find.text(label),
  );
}

// 앱 workflow의 인증 gate와 갱신 토큰 만료 동작을 검증한다.
void main() {
  testWidgets('App shows logo splash while restoring session', (
    WidgetTester tester,
  ) async {
    final restoreCompleter = Completer<AuthSession?>();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreCompleter: restoreCompleter),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pump();

    expect(find.byKey(const Key('splash-logo')), findsOneWidget);
    expect(find.text('Sign in with Google'), findsNothing);

    restoreCompleter.complete(null);
    await tester.pumpAndSettle();

    expect(find.text('Sign in with Google'), findsOneWidget);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Sign in with Google'), findsOneWidget);
    expect(find.byKey(const Key('google-sign-in-logo')), findsOneWidget);
    expect(find.text('Recommended for you'), findsNothing);

    await tester.tap(find.text('Sign in with Google'));
    await tester.pumpAndSettle();

    expect(authService.signInCalled, isTrue);
    await tester.drag(find.byType(ListView).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Recommended for you'), findsOneWidget);
    expect(find.text('LingKo User'), findsNothing);
  });

  testWidgets('Expired refresh session returns the app to login', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(
          restoreExistingSession: true,
          expireAuthenticatedRequests: true,
        ),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Sign in with Google'), findsOneWidget);
    expect(find.text('Recommended for you'), findsNothing);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Sign in with Google'));
    await tester.pumpAndSettle();

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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
        sentenceSpeechService: speechService,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('LingKo'), findsOneWidget);
    expect(find.text('Recommended for you'), findsOneWidget);
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
    expect(recommendedSentenceField.controller?.text, '맛있겠다.');
    expect(find.text('Standard pronunciation ready'), findsNothing);
    expect(
      find.textContaining('Standard pronunciation updates automatically'),
      findsNothing,
    );
    expect(find.text('Pronunciation guide'), findsNothing);
    expect(find.text('마싯게따.'), findsOneWidget);
    expect(find.text('Check standard pronunciation'), findsNothing);
    expect(find.text('Hide standard pronunciation'), findsNothing);
    expect(find.text('It looks delicious.'), findsNothing);
    expect(find.text('Final consonant linking and tense sound'), findsNothing);
    expect(find.text('1 / 10'), findsNothing);

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Start recording'), findsOneWidget);

    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();
    expect(find.text('Stop and analyze'), findsOneWidget);
    expect(find.text('마싯게따.'), findsNothing);

    await tester.tap(find.text('Stop and analyze'));
    await tester.pumpAndSettle();

    expect(find.text('Result'), findsOneWidget);
    expect(find.text('Excellent'), findsOneWidget);
    expect(find.text('91'), findsOneWidget);
    expect(find.text('Pronunciation guide'), findsOneWidget);
    expect(find.text('Sentence'), findsOneWidget);
    expect(find.text('Standard pronunciation'), findsOneWidget);
    expect(find.text('Recognized speech'), findsNothing);
    expect(find.text('사용자 발음'), findsNothing);
    expect(find.text('마싯게따.'), findsOneWidget);
    final resultNormalButton = find.byKey(
      const ValueKey('result-play-pronunciation-normal'),
    );
    final resultSlowButton = find.byKey(
      const ValueKey('result-play-pronunciation-slow'),
    );
    tester.widget<ActionButton>(resultNormalButton).onPressed?.call();
    await tester.pump();
    expect(speechService.lastText, '마싯게따.');
    expect(speechService.lastRate, SentenceSpeechRate.normal);

    tester.widget<ActionButton>(resultSlowButton).onPressed?.call();
    await tester.pump();
    expect(speechService.lastRate, SentenceSpeechRate.slow);
    await tester.scrollUntilVisible(
      find.text('Character-level scores are unavailable'),
      300,
    );
    expect(find.text('Character-level scores are unavailable'), findsOneWidget);
    expect(recorder.deletedPaths, ['/tmp/lingko-test.wav']);
  });

  testWidgets('Home shows remaining quota for restored session', (
    WidgetTester tester,
  ) async {
    final quotaApi = FakePracticeQuotaApi();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: quotaApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(quotaApi.lastAccessToken, 'access.jwt');
    expect(find.text('3 practices left today'), findsOneWidget);
  });

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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Start recording'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -120));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Start recording'));
    await tester.pump();
    await tester.tap(find.text('Stop and analyze'));
    await tester.pump();

    expect(find.byKey(const ValueKey('evaluation-progress')), findsOneWidget);
    expect(find.text('Evaluation job'), findsOneWidget);
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
    expect(find.text('Excellent'), findsOneWidget);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Start recording'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -120));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Start recording'));
    await tester.pump();
    await tester.tap(find.text('Stop and analyze'));
    await tester.pump();

    expect(find.byType(NavigationBar), findsNothing);
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -400));
    await tester.pump();
    await tester.tap(find.text('Continue in background'));
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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: quotaApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text('Unable to load practice quota. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('sensitive quota response'), findsNothing);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(
          quota: const PracticeQuota(
            date: '2026-06-17',
            freeLimit: 5,
            freeUsed: 5,
            rewardedAvailable: 0,
            remainingPractices: 0,
            resetAt: null,
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

    expect(find.text('No practices left today'), findsOneWidget);
    await tester.tap(find.text('No practices left today'));
    await tester.pumpAndSettle();

    expect(recorder.permissionChecks, 0);
    expect(find.text('Stop and analyze'), findsNothing);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        sentenceSpeechService: speechService,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Practice'));
    await tester.pumpAndSettle();

    expect(find.text('Practice sentence'), findsOneWidget);
    expect(find.text('Recommended'), findsNothing);
    expect(find.text('My sentence'), findsNothing);
    expect(find.text('Use this sentence'), findsNothing);
    expect(find.text('맛있겠다.'), findsNothing);
    expect(find.text('Start recording'), findsNothing);

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
    expect(find.text('Start recording'), findsNothing);

    await tester.pump(const Duration(milliseconds: 699));
    expect(api.lastText, isNull);
    await tester.pump(const Duration(milliseconds: 1));
    await tester.pumpAndSettle();

    expect(api.lastText, '안녕하세요 LingKo 1 연습테스트좋아요');
    expect(find.text('Standard pronunciation ready'), findsNothing);
    expect(find.text('Start recording'), findsOneWidget);
    expect(find.text('서버 표준 발음'), findsOneWidget);
    expect(find.text('Practice with your own sentence.'), findsNothing);

    final normalButton = find.byKey(const ValueKey('play-sentence-normal'));
    final slowButton = find.byKey(const ValueKey('play-sentence-slow'));
    tester.widget<ActionButton>(normalButton).onPressed?.call();
    await tester.pump();

    expect(speechService.lastText, '안녕하세요 LingKo 1 연습테스트좋아요');
    expect(speechService.lastRate, SentenceSpeechRate.normal);

    tester.widget<ActionButton>(slowButton).onPressed?.call();
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
        userPreferencesApi: FakeUserPreferencesApi(),
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
        userPreferencesApi: FakeUserPreferencesApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
        sentenceSpeechService: speechService,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();
    final normalButton = find.byKey(const ValueKey('play-sentence-normal'));
    tester.widget<ActionButton>(normalButton).onPressed?.call();
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
        userPreferencesApi: FakeUserPreferencesApi(),
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
    expect(find.text('Start recording'), findsNothing);
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
        userPreferencesApi: FakeUserPreferencesApi(),
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
          userPreferencesApi: FakeUserPreferencesApi(),
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
      expect(selectedSentenceField.controller?.text, '맛있겠다.');
      expect(find.text('마싯게따.'), findsOneWidget);
    },
  );

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
    expect(find.text('Recommended for you'), findsOneWidget);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();

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
    expect(find.text('Stop and analyze'), findsOneWidget);
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
    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Stop and analyze'));
    await tester.pumpAndSettle();

    expect(
      find.text(
        'The evaluation did not finish. Retry with the saved recording when available.',
      ),
      findsOneWidget,
    );
    expect(find.text('Retry with this recording'), findsOneWidget);
    expect(find.text('Result'), findsNothing);
  });

  testWidgets('immersive recording hides tabs and cancel stops recording', (
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
    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();

    expect(find.byType(NavigationBar), findsNothing);
    await tester.tap(find.text('Cancel recording'));
    await tester.pumpAndSettle();

    expect(recorder.cancelCount, 1);
  });

  testWidgets('Review shows recent practice history and opens retry', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        userPreferencesApi: FakeUserPreferencesApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Review'));
    await tester.pumpAndSettle();

    expect(find.text('Recent practice'), findsOneWidget);
    expect(find.text('Latest score'), findsOneWidget);
    expect(find.text('91'), findsWidgets);
    expect(find.text('맛있겠다.'), findsOneWidget);

    await tester.tap(find.text('Practice again'));
    await tester.pumpAndSettle();

    expect(find.text('Practice'), findsWidgets);
    expect(find.text('Check standard pronunciation'), findsNothing);
    expect(find.text('마싯게따.'), findsOneWidget);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();

    expect(find.text('LingKo User'), findsOneWidget);
    expect(find.text('user@example.com'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Sign out'), 300);
    expect(find.text('Sign out'), findsOneWidget);

    await tester.tap(find.text('Sign out'));
    await tester.pumpAndSettle();

    expect(find.text('Sign in with Google'), findsOneWidget);
    expect(find.text('Recommended for you'), findsNothing);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        authService: authService,
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Delete account'), 300);
    await tester.tap(find.text('Delete account'));
    await tester.pumpAndSettle();

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
    expect(find.text('Sign in with Google'), findsOneWidget);
  });

  testWidgets('Profile loads and updates learning preferences', (
    WidgetTester tester,
  ) async {
    final preferencesApi = FakeUserPreferencesApi();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        userPreferencesApi: preferencesApi,
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(_navigationLabel('Profile'));
    await tester.pumpAndSettle();
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -700));
    await tester.pumpAndSettle();

    expect(preferencesApi.lastAccessToken, 'access.jwt');
    expect(find.text('Display language'), findsOneWidget);
    expect(find.text('Korean'), findsOneWidget);
    expect(find.text('Native language'), findsOneWidget);
    expect(find.text('English'), findsWidgets);
    expect(find.text('Target level'), findsOneWidget);
    expect(find.text('Beginner 2'), findsOneWidget);

    await tester.tap(find.text('Target level'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Intermediate 1').last);
    await tester.pumpAndSettle();

    expect(
      preferencesApi.lastUpdatedPreferences?.targetLevel,
      LearningLevel.intermediate1,
    );
    expect(find.text('Intermediate 1'), findsOneWidget);
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
    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Stop and analyze'));
    await tester.pumpAndSettle();

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
    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Stop and analyze'));
    await tester.pumpAndSettle();

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -400));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continue in background'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('사진 찍어도 돼요?'), 300);
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
    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Stop and analyze'));
    await tester.pumpAndSettle();

    expect(find.text('Result'), findsOneWidget);
    expect(find.text('Excellent'), findsOneWidget);
  });

  testWidgets('weak character opens its pronunciation guide', (
    WidgetTester tester,
  ) async {
    const character = CharacterResult(
      character: '나',
      score: 55,
      scoreStatus: 'AVAILABLE',
      note: 'Focus on tongue placement',
      kind: 'TONGUE',
    );

    await tester.pumpWidget(
      MaterialApp(home: Scaffold(body: ResultTile(result: character))),
    );

    expect(find.text('55'), findsOneWidget);
    await tester.tap(find.byType(ResultTile));
    await tester.pumpAndSettle();

    expect(find.text('TONGUE guide'), findsWidgets);
    expect(find.text('Focus on tongue placement'), findsWidgets);
    expect(
      find.text(
        'Guide media is shown when the evaluation provides it. Audio and video playback are not available yet.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('guide sheet renders available static guide assets first', (
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
      const MaterialApp(home: Scaffold(body: GuideSheet(result: character))),
    );

    final images = tester.widgetList<Image>(find.byType(Image)).toList();

    expect(images, hasLength(2));
    expect((images[0].image as NetworkImage).url, character.mouthGuideUrl);
    expect((images[1].image as NetworkImage).url, character.tongueGuideUrl);
    expect(find.text('Mouth guide'), findsOneWidget);
    expect(find.text('Tongue guide'), findsOneWidget);
  });

  testWidgets('authenticated shell exposes Home Practice Review Profile tabs', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        userPreferencesApi: FakeUserPreferencesApi(),
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

  testWidgets('Result shows every scored syllable and retries whole sentence', (
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
      characterScoreStatus: 'AVAILABLE',
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
          scoreStatus: 'UNAVAILABLE',
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
            sentenceSpeechService: FakeSentenceSpeechService(),
            onTryAgain: () => retried = true,
          ),
        ),
      ),
    );

    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('result-character-grid')),
      300,
    );

    expect(find.text('저'), findsOneWidget);
    expect(find.text('94'), findsOneWidget);
    expect(find.text('피'), findsOneWidget);
    expect(find.text('62'), findsOneWidget);
    expect(find.text('Excellent'), findsWidgets);
    expect(find.text('Good'), findsOneWidget);
    expect(find.text('Keep practicing'), findsWidgets);
    expect(find.text('Needs improvement'), findsOneWidget);
    expect(find.text('No score'), findsOneWidget);
    expect(find.text('Practice 저 Again'), findsNothing);
    expect(find.text('Practice Weak Sound'), findsNothing);

    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('retry-whole-sentence')),
      300,
    );
    await tester.tap(find.text('Try This Sentence Again'));
    await tester.pump();

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
      characterScoreStatus: 'AVAILABLE',
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
              sentenceSpeechService: FakeSentenceSpeechService(),
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

    expect(find.text('Try This Sentence Again'), findsOneWidget);
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
        userPreferencesApi: FakeUserPreferencesApi(),
        practiceQuotaApi: FakePracticeQuotaApi(),
        authService: FakeAppAuthService(restoreExistingSession: true),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(_navigationLabel('Home'), findsOneWidget);
    expect(find.textContaining('Welcome back'), findsOneWidget);

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
