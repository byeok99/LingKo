import '../models/practice_sentence.dart';
import 'api_client.dart';

abstract class SentenceApi {
  Future<List<PracticeSentence>> fetchRecommendedSentences({
    int limit = 20,
    String? category,
  });

  Future<PracticeSentence> fetchSentence(int sentenceId);
}

class DartIoSentenceApi implements SentenceApi {
  DartIoSentenceApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<List<PracticeSentence>> fetchRecommendedSentences({
    int limit = 20,
    String? category,
  }) async {
    final json = await _client.getJson('/api/sentences/recommended', {
      'limit': limit,
      'category': category,
    });
    final items = json['items'];

    if (items is! List) {
      throw const FormatException('Missing recommended sentences');
    }

    return [
      for (final item in items)
        if (item is Map<String, Object?>)
          PracticeSentence.fromSentenceJson(item)
        else
          throw const FormatException('Invalid recommended sentence item'),
    ];
  }

  @override
  Future<PracticeSentence> fetchSentence(int sentenceId) async {
    final json = await _client.getJson('/api/sentences/$sentenceId');

    return PracticeSentence.fromSentenceJson(json);
  }
}
