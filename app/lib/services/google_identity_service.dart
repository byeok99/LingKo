import 'package:google_sign_in/google_sign_in.dart';

abstract class GoogleIdentityService {
  Future<String> signInAndGetIdToken();
}

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

    await _googleSignIn.initialize(
      serverClientId: _serverClientId.trim().isEmpty ? null : _serverClientId,
    );
    _initialized = true;
  }
}

class GoogleSignInUnavailableException implements Exception {
  const GoogleSignInUnavailableException();

  @override
  String toString() => 'Google sign-in is unavailable';
}
