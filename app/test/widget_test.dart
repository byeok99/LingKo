import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lingko_app/api/evaluation_api.dart';
import 'package:lingko_app/api/pronunciation_api.dart';
import 'package:lingko_app/api/sentence_api.dart';
import 'package:lingko_app/app/lingko_app.dart';
import 'package:lingko_app/models/auth_session.dart';
import 'package:lingko_app/models/practice_history.dart';
import 'package:lingko_app/models/practice_result.dart';
import 'package:lingko_app/models/practice_sentence.dart';
import 'package:lingko_app/services/audio_recorder_service.dart';
import 'package:lingko_app/services/app_auth_service.dart';
import 'package:lingko_app/widgets/result_tile.dart';

class FakePronunciationApi implements PronunciationApi {
  FakePronunciationApi({this.error});

  final Object? error;
  String? lastText;

  @override
  Future<PracticeSentence> prepareCustomSentence(String text) async {
    lastText = text;

    if (error != null) {
      throw error!;
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

class FakeEvaluationApi implements EvaluationApi {
  String? lastAudioPath;
  int? lastSentenceId;
  String? lastText;
  Object? error;
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
  Future<PracticeResult> evaluate({
    required String audioPath,
    int? sentenceId,
    String? text,
  }) async {
    lastAudioPath = audioPath;
    lastSentenceId = sentenceId;
    lastText = text;

    if (error != null) {
      throw error!;
    }

    return const PracticeResult(
      overallScore: 91,
      gradeLabel: 'Excellent',
      summary: 'Clear pronunciation.',
      recognizedText: '마싯게따.',
      characterScoreStatus: 'UNAVAILABLE',
      scoreBreakdown: PracticeScoreBreakdown(
        accuracy: 92,
        fluency: 90,
        completeness: 93,
      ),
      weakCharacters: [],
      characters: [],
    );
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

class FakeAppAuthService implements AppAuthService {
  FakeAppAuthService({this.restoreExistingSession = false});

  final bool restoreExistingSession;
  bool signInCalled = false;
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
  Future<void> signOut() async {
    session = null;
  }
}

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

void main() {
  testWidgets('LingKo prototype opens recommended sentences', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: recorder,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('LingKo'), findsOneWidget);
    expect(find.text('Recommended'), findsOneWidget);
    expect(find.text('맛있겠다.'), findsOneWidget);

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();

    expect(find.text('Practice'), findsWidgets);
    final recommendedInput = tester.widget<TextField>(find.byType(TextField));
    expect(recommendedInput.controller?.text, '맛있겠다.');

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Start recording'), findsOneWidget);

    await tester.tap(find.text('Start recording'));
    await tester.pumpAndSettle();
    expect(find.text('Stop recording'), findsOneWidget);

    await tester.tap(find.text('Stop recording'));
    await tester.pumpAndSettle();
    expect(find.text('Upload and score'), findsOneWidget);

    await tester.tap(find.text('Upload and score'));
    await tester.pumpAndSettle();

    expect(find.text('Result'), findsOneWidget);
    expect(find.text('Excellent'), findsOneWidget);
    expect(find.text('91'), findsOneWidget);
    expect(find.text('Recognized speech: 마싯게따.'), findsOneWidget);
    expect(
      find.text('Character-level scores are unavailable for this evaluation.'),
      findsOneWidget,
    );
    expect(recorder.deletedPaths, ['/tmp/lingko-test.wav']);
  });

  testWidgets('Practice tab accepts a custom sentence', (
    WidgetTester tester,
  ) async {
    final api = FakePronunciationApi();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: api,
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Practice'));
    await tester.pumpAndSettle();

    expect(find.text('Practice your own sentence'), findsOneWidget);
    expect(find.text('맛있겠다.'), findsNothing);
    expect(find.text('Start recording'), findsNothing);

    await tester.enterText(find.byType(TextField), '오늘 날씨가 좋아요.');
    await tester.pump();
    await tester.tap(find.text('Use this sentence'));
    await tester.pumpAndSettle();

    expect(api.lastText, '오늘 날씨가 좋아요.');
    expect(find.text('오늘 날씨가 좋아요.'), findsWidgets);
    expect(find.text('서버 표준 발음'), findsOneWidget);
  });

  testWidgets('Practice tab shows API errors for custom sentences', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(error: 'Validation failed'),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Practice'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), '너무 긴 문장');
    await tester.pump();
    await tester.tap(find.text('Use this sentence'));
    await tester.pumpAndSettle();

    expect(find.text('Validation failed'), findsOneWidget);
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
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );

    expect(find.text('Loading recommended sentences'), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.text('No recommended sentences yet.'), findsOneWidget);

    await tester.pumpWidget(
      LingKoApp(
        key: UniqueKey(),
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(error: 'Cannot load sentences'),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(),
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Cannot load sentences'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
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
        authService: FakeAppAuthService(),
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

    expect(find.text('Microphone permission is required.'), findsOneWidget);
    expect(find.text('Retry microphone permission'), findsOneWidget);

    await tester.tap(find.text('Retry microphone permission'));
    await tester.pumpAndSettle();

    expect(recorder.permissionChecks, 2);
    expect(find.text('Stop recording'), findsOneWidget);
  });

  testWidgets('leaving Practice tab cancels active recording', (
    WidgetTester tester,
  ) async {
    final recorder = FakeAudioRecorderService();
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(),
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

    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();

    expect(recorder.cancelCount, 1);
  });

  testWidgets('Profile shows recent practice history and opens retry', (
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

    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();

    expect(find.text('Practice history'), findsOneWidget);
    expect(find.text('Best score'), findsOneWidget);
    expect(find.text('91'), findsWidgets);
    expect(find.text('맛있겠다.'), findsOneWidget);
    expect(find.text('Recognized: 마싯게따.'), findsOneWidget);

    await tester.tap(find.text('Practice again'));
    await tester.pumpAndSettle();

    expect(find.text('Practice'), findsWidgets);
    final retryInput = tester.widget<TextField>(find.byType(TextField));
    expect(retryInput.controller?.text, '맛있겠다.');
  });

  testWidgets('Profile signs in with Google and shows account email', (
    WidgetTester tester,
  ) async {
    final authService = FakeAppAuthService();
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

    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();

    expect(find.text('Sign in with Google'), findsOneWidget);

    await tester.tap(find.text('Sign in with Google'));
    await tester.pumpAndSettle();

    expect(authService.signInCalled, isTrue);
    expect(find.text('LingKo User'), findsOneWidget);
    expect(find.text('user@example.com'), findsOneWidget);
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
        authService: FakeAppAuthService(),
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
    await tester.tap(find.text('Stop recording'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Profile'));
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
        evaluationApi: FakeEvaluationApi(),
        authService: FakeAppAuthService(),
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
    await tester.tap(find.text('Stop recording'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), '새 문장입니다.');
    await tester.pump();
    await tester.ensureVisible(find.text('Use this sentence'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Use this sentence'));
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
        authService: FakeAppAuthService(),
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
    await tester.tap(find.text('Stop recording'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Upload and score'));
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
    expect(find.text('Play'), findsOneWidget);
    expect(find.text('Repeat'), findsOneWidget);
  });
}
