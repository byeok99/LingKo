// 파일 의도: 학습 문장 TTS 재생과 속도 선택을 플랫폼 플러그인에서 격리한다.
// 선택 이유: 화면은 발화 의도만 전달하고 플랫폼 오류·설정은 서비스 경계에서 처리한다.

import 'package:flutter_tts/flutter_tts.dart';

/// 학습 문장을 들을 때 지원하는 발화 속도다.
enum SentenceSpeechRate { normal, slow }

/// 문장 듣기 기능의 발화·중지·생명주기 계약을 정의한다.
abstract class SentenceSpeechService {
  Future<void> speak(String text, {required SentenceSpeechRate rate});

  Future<void> stop();

  Future<void> dispose();
}

/// 기기의 한국어 TTS 음성을 이용해 문장을 재생한다.
///
/// 자유 문장도 별도 음원 생성·저장 없이 즉시 들을 수 있도록 기기 내장 음성을
/// 사용하며, 새 요청 전에 기존 발화를 중지해 문장이 겹치지 않도록 한다.
class FlutterTtsSentenceSpeechService implements SentenceSpeechService {
  FlutterTtsSentenceSpeechService({FlutterTts? flutterTts})
    : _flutterTts = flutterTts ?? FlutterTts();

  static const _language = 'ko-KR';
  static const _normalRate = 0.5;
  static const _slowRate = 0.35;

  final FlutterTts _flutterTts;
  Future<void>? _initialization;
  bool _isInitialized = false;

  Future<void> _initialize() async {
    if (_isInitialized) {
      return;
    }
    final initialization = _initialization ??= _configure();
    try {
      await initialization;
    } catch (_) {
      // 사용자가 기기 음성 설정을 보완한 뒤 앱 재시작 없이 다시 시도할 수 있게 한다.
      _initialization = null;
      rethrow;
    }
  }

  Future<void> _configure() async {
    await _flutterTts.awaitSpeakCompletion(true);
    await _flutterTts.setLanguage(_language);
    await _flutterTts.setVolume(1);
    await _flutterTts.setPitch(1);
    _isInitialized = true;
  }

  @override
  Future<void> speak(String text, {required SentenceSpeechRate rate}) async {
    final normalizedText = text.trim();
    if (normalizedText.isEmpty) {
      throw ArgumentError.value(text, 'text', '발화할 문장이 비어 있습니다.');
    }

    await _initialize();
    await _flutterTts.stop();
    await _flutterTts.setSpeechRate(
      rate == SentenceSpeechRate.normal ? _normalRate : _slowRate,
    );
    final result = await _flutterTts.speak(normalizedText);
    if (result != 1) {
      throw StateError('기기 TTS가 문장 재생을 시작하지 못했습니다.');
    }
  }

  @override
  Future<void> stop() async {
    if (!_isInitialized) {
      return;
    }
    await _flutterTts.stop();
  }

  @override
  Future<void> dispose() async {
    try {
      await stop();
    } catch (_) {
      // 앱 종료 중 플랫폼 채널 정리 실패는 사용자 흐름에 노출하지 않는다.
    }
  }
}
