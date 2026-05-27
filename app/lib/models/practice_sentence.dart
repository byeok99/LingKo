// 화면에 표시할 학습 문장 하나를 표현하는 임시 데이터 모델입니다.
// 나중에 백엔드 API가 붙으면 이 값들은 서버 응답 DTO와 매핑됩니다.
class PracticeSentence {
  const PracticeSentence({
    required this.text,
    required this.pronunciation,
    required this.translation,
    required this.level,
    required this.category,
    required this.point,
    required this.score,
    required this.characters,
  });

  // 사용자가 직접 입력한 문장을 임시 연습 데이터로 바꿉니다.
  // 실제 구현에서는 백엔드가 표준 발음, 번역, 글자별 가이드를 계산해 내려줍니다.
  factory PracticeSentence.custom(String text) {
    final trimmed = text.trim();

    return PracticeSentence(
      text: trimmed,
      pronunciation: 'Custom sentence',
      translation: 'Practice with your own sentence.',
      level: 'Custom',
      category: 'Free practice',
      point: 'Record your voice to receive pronunciation feedback.',
      score: 0,
      characters: _buildCustomCharacters(trimmed),
    );
  }

  final String text;
  final String pronunciation;
  final String translation;
  final String level;
  final String category;
  final String point;
  final int score;
  final List<CharacterResult> characters;
}

List<CharacterResult> _buildCustomCharacters(String text) {
  final visibleCharacters =
      text.runes
          .map(String.fromCharCode)
          .where((character) => character.trim().isNotEmpty)
          .take(8)
          .toList();

  if (visibleCharacters.isEmpty) {
    return const [];
  }

  return [
    for (final character in visibleCharacters)
      CharacterResult(
        character: character,
        score: 0,
        note: 'Guide will be generated after pronunciation analysis.',
        kind: 'Custom',
      ),
  ];
}

// 평가 결과에서 글자별 점수와 피드백을 보여주기 위한 모델입니다.
class CharacterResult {
  const CharacterResult({
    required this.character,
    required this.score,
    required this.note,
    required this.kind,
  });

  final String character;
  final int score;
  final String note;
  final String kind;
}
