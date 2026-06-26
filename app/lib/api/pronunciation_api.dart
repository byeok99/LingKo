import '../models/practice_sentence.dart';
import 'api_client.dart';

abstract class PronunciationApi {
  Future<PracticeSentence> prepareCustomSentence(String text);
}

class DartIoPronunciationApi implements PronunciationApi {
  DartIoPronunciationApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<PracticeSentence> prepareCustomSentence(String text) async {
    final json = await _client.postJson('/api/pronunciation/prepare', {
      'source': 'CUSTOM',
      'text': text,
    });

    return PracticeSentence.fromPrepareResponse(json);
  }
}
