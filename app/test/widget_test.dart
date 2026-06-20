import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lingko_app/api/evaluation_api.dart';
import 'package:lingko_app/api/pronunciation_api.dart';
import 'package:lingko_app/api/sentence_api.dart';
import 'package:lingko_app/app/lingko_app.dart';
import 'package:lingko_app/models/practice_result.dart';
import 'package:lingko_app/models/practice_sentence.dart';
import 'package:lingko_app/services/audio_recorder_service.dart';

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

  @override
  Future<PracticeResult> evaluate({
    required String audioPath,
    String? practiceToken,
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
      scoreBreakdown: PracticeScoreBreakdown(
        accuracy: 92,
        fluency: 90,
        completeness: 93,
      ),
      weakCharacters: [],
      characters: [],
    );
  }
}

class FakeAudioRecorderService implements AudioRecorderService {
  FakeAudioRecorderService({this.permission = true});

  final bool permission;
  bool started = false;
  bool cancelled = false;

  @override
  Future<bool> hasPermission() async => permission;

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
    cancelled = true;
  }

  @override
  Future<void> dispose() async {}
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

void main() {
  testWidgets('LingKo prototype opens recommended sentences', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        audioRecorderService: FakeAudioRecorderService(),
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
        audioRecorderService: FakeAudioRecorderService(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Cannot load sentences'), findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });

  testWidgets('Practice tab shows microphone permission errors', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      LingKoApp(
        pronunciationApi: FakePronunciationApi(),
        sentenceApi: FakeSentenceApi(),
        evaluationApi: FakeEvaluationApi(),
        audioRecorderService: FakeAudioRecorderService(permission: false),
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
  });
}
