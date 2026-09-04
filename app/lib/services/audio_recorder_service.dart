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

  /// 현재 입력 레벨을 0.0~1.0으로 정규화해 흘려보낸다.
  ///
  /// 화면이 "마이크가 실제로 소리를 받고 있다"를 보여줄 수 있어야 사용자가 녹음 실패를
  /// 평가 기회를 쓰기 전에 알아차린다. dBFS 원값을 그대로 노출하면 화면이 플러그인 단위에
  /// 결합되므로 서비스 경계에서 표시용 비율로 바꿔 전달한다.
  Stream<double> amplitudeStream();

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

  /// 조용한 방의 잡음이 파형을 흔들지 않도록 이 값보다 작은 입력은 0으로 본다.
  static const _silenceFloorDb = -45.0;

  @override
  Stream<double> amplitudeStream() {
    return _recorder
        .onAmplitudeChanged(const Duration(milliseconds: 100))
        .map((amplitude) {
          // 플러그인은 dBFS를 주므로 0dB를 최대로 두고 무음 기준선까지를 0~1로 편다.
          final current = amplitude.current;
          if (!current.isFinite || current <= _silenceFloorDb) {
            return 0.0;
          }
          final normalized = 1 - (current / _silenceFloorDb);
          return normalized.clamp(0.0, 1.0);
        });
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
