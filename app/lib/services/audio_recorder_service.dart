// 파일 의도: audio recorder 서비스 플랫폼·생명주기 기능을 추상화한다.
// 선택 이유: 플러그인 세부사항을 UI에서 격리해 오류 처리와 테스트 대역 교체를 가능하게 한다.

import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

/// Audio Recorder 서비스 플랫폼 기능 계약을 정의한다.
/// 플러그인 구현을 교체하고 unit test에서 대역을 주입할 수 있도록 추상 경계를 선택했다.
abstract class AudioRecorderService {
  Future<bool> hasPermission();

  Future<String> start();

  Future<String?> stop();

  Future<void> cancel();

  Future<void> delete(String path);

  Future<void> dispose();
}

/// Record Audio Recorder 서비스 플랫폼·세션 생명주기 동작을 구현한다.
/// UI가 플러그인 API와 보안 저장 세부사항에 직접 결합되지 않도록 서비스에 캡슐화했다.
class RecordAudioRecorderService implements AudioRecorderService {
  RecordAudioRecorderService({AudioRecorder? recorder})
    : _recorder = recorder ?? AudioRecorder();

  final AudioRecorder _recorder;

  @override
  Future<bool> hasPermission() {
    return _recorder.hasPermission();
  }

  @override
  Future<String> start() async {
    final directory = await getTemporaryDirectory();
    final path =
        '${directory.path}${Platform.pathSeparator}lingko-${DateTime.now().millisecondsSinceEpoch}.wav';
    // 백엔드 WAV 검증 및 음성 평가 입력과 일치하도록 16kHz 단일 채널 PCM WAV로 고정한다.
    await _recorder.start(
      const RecordConfig(
        encoder: AudioEncoder.wav,
        sampleRate: 16000,
        numChannels: 1,
      ),
      path: path,
    );
    return path;
  }

  @override
  Future<String?> stop() {
    return _recorder.stop();
  }

  @override
  Future<void> cancel() {
    return _recorder.cancel();
  }

  @override
  Future<void> delete(String path) async {
    final file = File(path);
    if (await file.exists()) {
      await file.delete();
    }
  }

  @override
  Future<void> dispose() {
    return _recorder.dispose();
  }
}
