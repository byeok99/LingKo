enum LearningLevel {
  beginner1('BEGINNER_1', 'Beginner 1'),
  beginner2('BEGINNER_2', 'Beginner 2'),
  intermediate1('INTERMEDIATE_1', 'Intermediate 1'),
  intermediate2('INTERMEDIATE_2', 'Intermediate 2'),
  advanced('ADVANCED', 'Advanced');

  const LearningLevel(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static LearningLevel fromApiValue(Object? value) {
    if (value is String) {
      for (final level in LearningLevel.values) {
        if (level.apiValue == value) {
          return level;
        }
      }
    }

    return LearningLevel.beginner2;
  }
}

class UserPreferences {
  const UserPreferences({
    required this.displayLanguage,
    required this.nativeLanguage,
    required this.targetLevel,
  });

  factory UserPreferences.fromJson(Map<String, Object?> json) {
    return UserPreferences(
      displayLanguage: _stringValue(json['displayLanguage'], fallback: 'en'),
      nativeLanguage: _stringValue(json['nativeLanguage'], fallback: 'en'),
      targetLevel: LearningLevel.fromApiValue(json['targetLevel']),
    );
  }

  static const defaults = UserPreferences(
    displayLanguage: 'en',
    nativeLanguage: 'en',
    targetLevel: LearningLevel.beginner2,
  );

  final String displayLanguage;
  final String nativeLanguage;
  final LearningLevel targetLevel;

  Map<String, Object?> toJson() {
    return {
      'displayLanguage': displayLanguage,
      'nativeLanguage': nativeLanguage,
      'targetLevel': targetLevel.apiValue,
    };
  }

  UserPreferences copyWith({
    String? displayLanguage,
    String? nativeLanguage,
    LearningLevel? targetLevel,
  }) {
    return UserPreferences(
      displayLanguage: displayLanguage ?? this.displayLanguage,
      nativeLanguage: nativeLanguage ?? this.nativeLanguage,
      targetLevel: targetLevel ?? this.targetLevel,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is UserPreferences &&
            other.displayLanguage == displayLanguage &&
            other.nativeLanguage == nativeLanguage &&
            other.targetLevel == targetLevel;
  }

  @override
  int get hashCode {
    return Object.hash(displayLanguage, nativeLanguage, targetLevel);
  }
}

String _stringValue(Object? value, {required String fallback}) {
  if (value is String && value.trim().isNotEmpty) {
    return value;
  }

  return fallback;
}
