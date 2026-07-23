import '../api/auth_api.dart';
import '../api/api_client.dart';
import '../models/auth_session.dart';
import 'auth_session_store.dart';
import 'google_identity_service.dart';

abstract class AppAuthService {
  Future<AuthSession?> restoreSession();

  Future<AuthSession> signInWithGoogle();

  Future<T> runAuthenticated<T>(Future<T> Function(String accessToken) request);

  Future<void> signOut();
}

class DefaultAppAuthService implements AppAuthService {
  DefaultAppAuthService({
    AuthApi? authApi,
    GoogleIdentityService? googleIdentityService,
    AuthSessionStore? sessionStore,
  }) : _authApi = authApi ?? DartIoAuthApi(),
       _googleIdentityService =
           googleIdentityService ?? GoogleSignInIdentityService(),
       _sessionStore = sessionStore ?? AuthSessionStore();

  final AuthApi _authApi;
  final GoogleIdentityService _googleIdentityService;
  final AuthSessionStore _sessionStore;
  Future<AuthSession>? _refreshInFlight;
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

    if (revision != _sessionRevision) {
      throw const AuthSessionExpiredException();
    }

    await _sessionStore.save(session);
    if (revision != _sessionRevision) {
      await _clearIfCurrent(session);
      throw const AuthSessionExpiredException();
    }
    return session;
  }

  Future<void> _clearIfCurrent(AuthSession session) async {
    final storedSession = await _sessionStore.read();
    if (storedSession?.refreshToken == session.refreshToken) {
      await _sessionStore.clear();
    }
  }
}

class AuthSessionExpiredException implements Exception {
  const AuthSessionExpiredException();

  @override
  String toString() => 'Authentication session expired';
}
