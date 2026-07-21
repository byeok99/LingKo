import '../api/auth_api.dart';
import '../models/auth_session.dart';
import 'auth_session_store.dart';
import 'google_identity_service.dart';

abstract class AppAuthService {
  Future<AuthSession?> restoreSession();

  Future<AuthSession> signInWithGoogle();

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

  @override
  Future<AuthSession?> restoreSession() {
    return _sessionStore.read();
  }

  @override
  Future<AuthSession> signInWithGoogle() async {
    final idToken = await _googleIdentityService.signInAndGetIdToken();
    final session = await _authApi.loginWithGoogleIdToken(idToken);
    await _sessionStore.save(session);

    return session;
  }

  @override
  Future<void> signOut() {
    return _sessionStore.clear();
  }
}
