import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

abstract class AudioRecorderService {
  Future<bool> hasPermission();

  Future<String> start();

  Future<String?> stop();

  Future<void> cancel();

  Future<void> dispose();
}

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
    final directory = await getApplicationDocumentsDirectory();
    final path =
        '${directory.path}${Platform.pathSeparator}lingko-${DateTime.now().millisecondsSinceEpoch}.wav';
    await _recorder.start(
      const RecordConfig(encoder: AudioEncoder.wav),
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
  Future<void> dispose() {
    return _recorder.dispose();
  }
}
