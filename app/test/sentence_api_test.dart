import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/sentence_api.dart';

void main() {
  test(
    'fetchRecommendedSentences sends optional query and maps items',
    () async {
      Uri? requestedUri;
      final api = DartIoSentenceApi(
        client: ApiClient(
          baseUrl: 'http://localhost:8080',
          getJsonTransport: (uri, timeout, headers) async {
            requestedUri = uri;

            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'items': [
                  {
                    'sentenceId': 1,
                    'source': 'RECOMMENDED',
                    'originalText': '맛있겠다.',
                    'standardPronunciation': '마싯게따.',
                    'translation': 'It looks delicious.',
                    'categoryLabel': 'Food',
                    'learningPoint': 'Final consonant linking',
                    'initialScore': 0,
                    'characters': [],
                  },
                ],
              }),
            );
          },
        ),
      );

      final sentences = await api.fetchRecommendedSentences(
        limit: 20,
        category: 'FOOD',
      );

      expect(
        requestedUri.toString(),
        'http://localhost:8080/api/sentences/recommended?limit=20&category=FOOD',
      );
      expect(sentences.single.text, '맛있겠다.');
      expect(sentences.single.pronunciation, '마싯게따.');
      expect(sentences.single.category, 'Food');
    },
  );

  test('fetchSentence maps a single recommended sentence', () async {
    final api = DartIoSentenceApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'sentenceId': 2,
              'source': 'RECOMMENDED',
              'originalText': '물 한 잔 주세요.',
              'standardPronunciation': '물 한 잔 주세요.',
              'translation': 'Please give me a glass of water.',
              'categoryLabel': 'Food',
              'learningPoint': 'Final consonant clarity',
              'initialScore': 0,
              'characters': [],
            }),
          );
        },
      ),
    );

    final sentence = await api.fetchSentence(2);

    expect(sentence.sentenceId, 2);
    expect(sentence.text, '물 한 잔 주세요.');
  });

  test(
    'fetchRecommendedSentences fails when any item has invalid shape',
    () async {
      final api = DartIoSentenceApi(
        client: ApiClient(
          baseUrl: 'http://localhost:8080',
          getJsonTransport: (uri, timeout, headers) async {
            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'items': [
                  {
                    'sentenceId': 1,
                    'source': 'RECOMMENDED',
                    'originalText': '맛있겠다.',
                    'standardPronunciation': '마싯게따.',
                    'translation': 'It looks delicious.',
                    'categoryLabel': 'Food',
                    'learningPoint': 'Final consonant linking',
                    'initialScore': 0,
                    'characters': [],
                  },
                  'invalid',
                ],
              }),
            );
          },
        ),
      );

      expect(
        () => api.fetchRecommendedSentences(),
        throwsA(
          isA<FormatException>().having(
            (exception) => exception.message,
            'message',
            'Invalid recommended sentence item',
          ),
        ),
      );
    },
  );
}
