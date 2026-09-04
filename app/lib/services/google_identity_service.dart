// 파일 의도: google identity 서비스 플랫폼·생명주기 기능을 추상화한다.
// 선택 이유: 플러그인 세부사항을 UI에서 격리해 오류 처리와 테스트 대역 교체를 가능하게 한다.

import 'package:google_sign_in/google_sign_in.dart';

/// Google Identity 서비스 플랫폼 기능 계약을 정의한다.
/// 플러그인 구현을 교체하고 unit test에서 대역을 주입할 수 있도록 추상 경계를 선택했다.
abstract class GoogleIdentityService {
  Future<String> signInAndGetIdToken();
}

/// Google Sign In Identity 서비스 플랫폼·세션 생명주기 동작을 구현한다.
/// UI가 플러그인 API와 보안 저장 세부사항에 직접 결합되지 않도록 서비스에 캡슐화했다.
class GoogleSignInIdentityService implements GoogleIdentityService {
  GoogleSignInIdentityService({
    GoogleSignIn? googleSignIn,
    String serverClientId = const String.fromEnvironment(
      'GOOGLE_SERVER_CLIENT_ID',
    ),
  }) : _googleSignIn = googleSignIn ?? GoogleSignIn.instance,
       _serverClientId = serverClientId;

  final GoogleSignIn _googleSignIn;
  final String _serverClientId;
  bool _initialized = false;

  @override
  Future<String> signInAndGetIdToken() async {
    await _initializeOnce();

    if (!_googleSignIn.supportsAuthenticate()) {
      throw const GoogleSignInUnavailableException();
    }

    final account = await _googleSignIn.authenticate();
    final idToken = account.authentication.idToken;

    if (idToken == null || idToken.trim().isEmpty) {
      throw const GoogleSignInUnavailableException();
    }

    return idToken;
  }

  Future<void> _initializeOnce() async {
    if (_initialized) {
      return;
    }

    // 서버 클라이언트 ID는 백엔드가 검증할 ID 토큰의 audience를 지정한다.
    // 값이 없는 개발 환경도 초기화 자체는 가능하게 하되, 운영 실행에서는 dart-define으로 주입한다.
    await _googleSignIn.initialize(
      serverClientId: _serverClientId.trim().isEmpty ? null : _serverClientId,
    );
    _initialized = true;
  }
}

/// Google Sign In Unavailable 예외 플랫폼·세션 생명주기 동작을 구현한다.
/// UI가 플러그인 API와 보안 저장 세부사항에 직접 결합되지 않도록 서비스에 캡슐화했다.
class GoogleSignInUnavailableException implements Exception {
  const GoogleSignInUnavailableException();

  @override
  String toString() => 'Google sign-in is unavailable';
}
