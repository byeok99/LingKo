// 파일 의도: app auth 서비스 플랫폼·생명주기 기능을 추상화한다.
// 선택 이유: 플러그인 세부사항을 UI에서 격리해 오류 처리와 테스트 대역 교체를 가능하게 한다.

import '../api/auth_api.dart';
import '../api/api_client.dart';
import '../api/legal_consent_api.dart';
import '../models/auth_session.dart';
import '../models/consent_selection.dart';
import '../models/legal_consent_status.dart';
import 'auth_session_store.dart';
import 'apple_identity_service.dart';
import 'google_identity_service.dart';

/// 로컬·서버 인증 세션의 앱 수준 생명주기를 소유한다.
abstract class AppAuthService {
  /// 안전한 로컬 저장소에서 이전 LingKo 세션을 복원하며 없으면 null을 반환한다.
  Future<AuthSession?> restoreSession();

  /// Google 계정 인증을 완료하고 새 LingKo 세션을 저장한다.
  Future<AuthSession> signInWithGoogle();

  /// 요청별 nonce가 결합된 Apple credential을 교환하고 새 LingKo 세션을 저장한다.
  Future<AuthSession> signInWithApple();

  /// 현재 로그인 사용자가 최신 문서 버전에 동의했는지 서버에서 확인한다.
  Future<LegalConsentStatus> fetchLegalConsentStatus();

  /// 화면에서 받은 선택을 현재 Bearer token 사용자에게 귀속해 서버에 기록한다.
  Future<LegalConsentStatus> recordLegalConsent(ConsentSelection selection);

  /// 보호 요청을 실행하고 401이면 갱신와 재시도를 최대 한 번 수행한다.
  Future<T> runAuthenticated<T>(Future<T> Function(String accessToken) request);

  /// 현재 기기 세션을 폐기하고 로컬 인증 정보을 삭제한다.
  Future<void> signOut();

  /// 서버 계정 삭제가 성공한 경우에만 로컬 인증 정보를 제거한다.
  Future<void> deleteAccount();
}

/// Google·Apple 신원, LingKo 토큰, 안전한 로컬 저장을 조율한다.
class DefaultAppAuthService implements AppAuthService {
  DefaultAppAuthService({
    AuthApi? authApi,
    LegalConsentApi? legalConsentApi,
    GoogleIdentityService? googleIdentityService,
    AppleIdentityService? appleIdentityService,
    AuthSessionStore? sessionStore,
  }) : _authApi = authApi ?? DartIoAuthApi(),
       _legalConsentApi = legalConsentApi ?? DartIoLegalConsentApi(),
       _googleIdentityService =
           googleIdentityService ?? GoogleSignInIdentityService(),
       _appleIdentityService =
           appleIdentityService ?? SignInWithAppleIdentityService(),
       _sessionStore = sessionStore ?? AuthSessionStore();

  final AuthApi _authApi;
  final LegalConsentApi _legalConsentApi;
  final GoogleIdentityService _googleIdentityService;
  final AppleIdentityService _appleIdentityService;
  final AuthSessionStore _sessionStore;
  // 동시 401 응답이 한 번만 회전하도록 모든 호출자가 같은 Future를 공유한다.
  Future<AuthSession>? _refreshInFlight;
  // 로그인·로그아웃 상태 전이 전에 시작된 네트워크 응답을 무효화한다.
  int _sessionRevision = 0;

  @override
  Future<AuthSession?> restoreSession() {
    return _sessionStore.read();
  }

  @override
  Future<AuthSession> signInWithGoogle() async {
    final revision = ++_sessionRevision;
    final idToken = await _googleIdentityService.signInAndGetIdToken();
    final session = await _authApi.loginWithGoogleIdToken(idToken);
    if (revision != _sessionRevision) {
      throw const AuthSessionExpiredException();
    }
    await _sessionStore.save(session);

    return session;
  }

  @override
  Future<AuthSession> signInWithApple() async {
    final revision = ++_sessionRevision;
    final credential = await _appleIdentityService.signIn();
    final session = await _authApi.loginWithAppleCredential(
      identityToken: credential.identityToken,
      rawNonce: credential.rawNonce,
      displayName: credential.displayName,
    );
    if (revision != _sessionRevision) {
      throw const AuthSessionExpiredException();
    }
    await _sessionStore.save(session);
    return session;
  }

  @override
  Future<LegalConsentStatus> fetchLegalConsentStatus() {
    return runAuthenticated(
      (accessToken) => _legalConsentApi.fetchStatus(accessToken: accessToken),
    );
  }

  @override
  Future<LegalConsentStatus> recordLegalConsent(ConsentSelection selection) {
    return runAuthenticated(
      (accessToken) => _legalConsentApi.record(
        accessToken: accessToken,
        selection: selection,
      ),
    );
  }

  @override
  Future<T> runAuthenticated<T>(
    Future<T> Function(String accessToken) request,
  ) async {
    final session = await _requireSession();

    try {
      return await request(session.accessToken);
    } on ApiException catch (error) {
      if (error.statusCode != 401) {
        rethrow;
      }
    }

    // 다른 요청이 이미 회전된 최신 토큰 쌍을 저장했을 수 있어 다시 조회한다.
    final latestSession = await _sessionStore.read();
    final refreshed =
        latestSession != null &&
                latestSession.refreshToken != session.refreshToken
            ? latestSession
            : await _refreshSession(session.refreshToken);

    try {
      return await request(refreshed.accessToken);
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        await _sessionStore.clear();
        throw const AuthSessionExpiredException();
      }
      rethrow;
    }
  }

  @override
  Future<void> signOut() async {
    // 원격 폐기를 시작하기 전에 진행 중인 갱신 응답을 무효화한다.
    _sessionRevision++;
    final session = await _sessionStore.read();

    try {
      if (session != null) {
        await _authApi.logout(session.refreshToken);
      }
    } finally {
      await _sessionStore.clear();
    }
  }

  @override
  Future<void> deleteAccount() async {
    // 탈퇴 전에 시작된 token 갱신이 삭제된 계정을 로컬에 복원하지 못하게 무효화한다.
    _sessionRevision++;
    final session = await _requireSession();

    await _authApi.deleteAccount(session.accessToken, session.refreshToken);
    await _clearIfCurrent(session);
  }

  Future<AuthSession> _requireSession() async {
    final session = await _sessionStore.read();
    if (session == null) {
      throw const AuthSessionExpiredException();
    }
    return session;
  }

  Future<AuthSession> _refreshSession(String refreshToken) {
    final activeRefresh = _refreshInFlight;
    if (activeRefresh != null) {
      return activeRefresh;
    }

    final refresh = _performRefresh(refreshToken, _sessionRevision);
    _refreshInFlight = refresh;
    return refresh.whenComplete(() {
      if (identical(_refreshInFlight, refresh)) {
        _refreshInFlight = null;
      }
    });
  }

  Future<AuthSession> _performRefresh(String refreshToken, int revision) async {
    late final AuthSession session;
    try {
      session = await _authApi.refreshSession(refreshToken);
    } catch (_) {
      if (revision == _sessionRevision) {
        await _sessionStore.clear();
      }
      throw const AuthSessionExpiredException();
    }

    // 로그아웃 시작 후 완료된 갱신 응답은 절대 저장하지 않는다.
    if (revision != _sessionRevision) {
      throw const AuthSessionExpiredException();
    }

    await _sessionStore.save(session);
    // 보안 저장소 쓰기 중 로그아웃이 경합할 수 있어 저장 후 revision을 다시 확인한다.
    if (revision != _sessionRevision) {
      await _clearIfCurrent(session);
      throw const AuthSessionExpiredException();
    }
    return session;
  }

  Future<void> _clearIfCurrent(AuthSession session) async {
    // 오래된 갱신 응답을 대체한 최신 로그인 세션을 삭제하지 않도록 현재 토큰을 비교한다.
    final storedSession = await _sessionStore.read();
    if (storedSession?.refreshToken == session.refreshToken) {
      await _sessionStore.clear();
    }
  }
}

/// 보호 요청을 중단하고 로그인 화면을 표시해야 함을 나타낸다.
class AuthSessionExpiredException implements Exception {
  const AuthSessionExpiredException();

  @override
  String toString() => 'Authentication session expired';
}
