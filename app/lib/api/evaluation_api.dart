import '../models/practice_history.dart';
import '../models/practice_result.dart';
import 'api_client.dart';

abstract class EvaluationApi {
  Future<PracticeResult> evaluate({
    required String audioPath,
    int? sentenceId,
    String? text,
  });

  Future<PracticeHistory> fetchHistory({
    required String accessToken,
    int page = 0,
    int size = 10,
  });
}

class DartIoEvaluationApi implements EvaluationApi {
  DartIoEvaluationApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<PracticeResult> evaluate({
    required String audioPath,
    int? sentenceId,
    String? text,
  }) async {
    final fields = <String, String>{
      if (sentenceId != null) 'sentenceId': '$sentenceId',
      if (text != null && text.trim().isNotEmpty) 'text': text.trim(),
    };

    final json = await _client.postMultipart(
      '/api/evaluations',
      MultipartUpload(
        file: MultipartFileData(
          fieldName: 'audio',
          path: audioPath,
          filename: _basename(audioPath),
          contentType: 'audio/wav',
        ),
        fields: fields,
      ),
    );

    return PracticeResult.fromJson(json);
  }

  @override
  Future<PracticeHistory> fetchHistory({
    required String accessToken,
    int page = 0,
    int size = 10,
  }) async {
    final json = await _client.getJson(
      '/api/evaluations/me',
      {'page': page, 'size': size},
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );

    return PracticeHistory.fromJson(json);
  }
}

String _basename(String path) {
  return path.split(RegExp(r'[/\\]')).last;
}
