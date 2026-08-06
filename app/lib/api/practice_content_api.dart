// 파일 의도: 저장 문장과 취약 음절 관련 백엔드 통신 경계를 정의한다.
// 선택 이유: 두 기능 모두 "다음에 무엇을 연습할지"를 다루므로 한 경계로 묶어 화면 주입을 단순화한다.

import '../models/practice_sentence.dart';
import '../models/weak_sound.dart';
import 'api_client.dart';

/// Practice Content Api 백엔드 통신 계약을 정의한다.
/// 화면과 서비스가 HTTP 구현이 아닌 추상 계약에 의존하도록 추상 클래스를 선택했다.
abstract class PracticeContentApi {
  /// 저장한 문장을 최근 저장 순으로 가져온다.
  Future<List<PracticeSentence>> fetchSavedSentences({
    required String accessToken,
  });

  /// 저장 상태를 뒤집고 뒤집힌 결과를 돌려준다.
  ///
  /// 원하는 상태를 보내지 않고 서버가 실제 상태를 뒤집는다. 두 기기에서 동시에 누를 때
  /// 뒤늦게 도착한 요청이 이전 상태를 되살리는 것을 막기 위해서다.
  Future<bool> toggleSavedSentence({
    required String accessToken,
    required int sentenceId,
  });

  /// 반복해서 틀리는 음절을 평균 점수가 낮은 순으로 가져온다.
  Future<List<WeakSound>> fetchWeakSounds({
    required String accessToken,
    int limit = 3,
  });

  /// 음절 하나의 누적 성적과 과거 시도·다음 후보를 한 번에 가져온다.
  Future<SoundDetail> fetchSoundDetail({
    required String accessToken,
    required String character,
  });
}

/// Dart Io Practice Content Api 백엔드 요청·응답 매핑을 구현한다.
class DartIoPracticeContentApi implements PracticeContentApi {
  DartIoPracticeContentApi({ApiClient? client})
    : _client = client ?? ApiClient();

  final ApiClient _client;

  Map<String, String> _auth(String accessToken) => {
    'Authorization': 'Bearer $accessToken',
  };

  @override
  Future<List<PracticeSentence>> fetchSavedSentences({
    required String accessToken,
  }) async {
    final json = await _client.getJson(
      '/api/sentences/saved',
      const {},
      _auth(accessToken),
    );
    final items = json['items'];
    if (items is! List) {
      throw const FormatException('Missing saved sentences');
    }
    return [
      for (final item in items)
        if (item is Map<String, Object?>)
          PracticeSentence.fromSentenceJson(item)
        else
          throw const FormatException('Invalid saved sentence item'),
    ];
  }

  @override
  Future<bool> toggleSavedSentence({
    required String accessToken,
    required int sentenceId,
  }) async {
    final json = await _client.patchJson(
      '/api/sentences/saved/$sentenceId',
      const {},
      _auth(accessToken),
    );
    return json['saved'] == true;
  }

  @override
  Future<List<WeakSound>> fetchWeakSounds({
    required String accessToken,
    int limit = 3,
  }) async {
    final json = await _client.getJson('/api/evaluations/me/weak-sounds', {
      'limit': limit,
    }, _auth(accessToken));
    final items = json['items'];
    if (items is! List) {
      throw const FormatException('Missing weak sounds');
    }
    return [
      for (final item in items)
        if (item is Map<String, Object?>)
          WeakSound.fromJson(item)
        else
          throw const FormatException('Invalid weak sound item'),
    ];
  }

  @override
  Future<SoundDetail> fetchSoundDetail({
    required String accessToken,
    required String character,
  }) async {
    final json = await _client.getJson(
      // 경로에 한글 한 글자가 들어가므로 반드시 인코딩한다.
      '/api/evaluations/me/sounds/${Uri.encodeComponent(character)}',
      const {},
      _auth(accessToken),
    );
    return SoundDetail.fromJson(json);
  }
}
