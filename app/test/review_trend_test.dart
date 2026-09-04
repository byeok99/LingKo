// 파일 의도: Review 그래프가 가장 최근 점수를 올바른 시간 방향으로 표시하는지 보장한다.

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/models/practice_history.dart';
import 'package:lingko_app/models/practice_result.dart';
import 'package:lingko_app/screens/review_screen.dart';

void main() {
  test('recent trend keeps seven newest scores in chronological order', () {
    final newestFirstItems = [93, 88, 81, 79, 74, 70, 65, 60]
        .asMap()
        .entries
        .map(
          (entry) => PracticeHistoryItem(
            evaluationLogId: 100 - entry.key,
            source: 'CUSTOM',
            originalText: '문장 ${entry.key}',
            standardPronunciation: '문장 ${entry.key}',
            recognizedText: '문장 ${entry.key}',
            overallScore: entry.value,
            gradeLabel: 'Score',
            summary: 'Summary',
            scoreBreakdown: const PracticeScoreBreakdown(
              accuracy: 0,
              fluency: 0,
              completeness: 0,
            ),
            characters: const [],
          ),
        )
        .toList(growable: false);

    expect(recentTrendScores(newestFirstItems), [65, 70, 74, 79, 81, 88, 93]);
    expect(recentTrendScores(const []), isEmpty);
  });
}
