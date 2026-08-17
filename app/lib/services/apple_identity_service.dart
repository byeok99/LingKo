// 파일 의도: Apple native 인증과 nonce 생성을 UI·Backend API에서 분리한다.
// 선택 이유: raw nonce가 플랫폼 요청에 노출되지 않게 하고 테스트에서 계정 UI를 대체할 수 있어야 한다.

import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';

typedef AppleCredentialRequester =
    Future<ApplePlatformCredential> Function(String hashedNonce);
typedef AppleNonceGenerator = String Function();

/// Backend가 Apple identity token의 요청 귀속을 검증하는 데 필요한 credential 묶음이다.
class AppleIdentityCredential {
  const AppleIdentityCredential({
    required this.identityToken,
    required this.rawNonce,
    this.displayName,
  });

  final String identityToken;
  final String rawNonce;

  /// Apple 이름은 최초 승인에만 존재하며 null은 기존 서버 이름을 보존하라는 뜻이다.
  final String? displayName;
}

/// 플러그인 응답 중 LingKo 로그인에 필요한 최소 필드만 노출한다.
class ApplePlatformCredential {
  const ApplePlatformCredential({
    this.identityToken,
    this.givenName,
    this.familyName,
  });

  final String? identityToken;
  final String? givenName;
  final String? familyName;
}

/// Apple 계정 UI를 열고 검증 가능한 credential을 반환하는 플랫폼 경계다.
abstract class AppleIdentityService {
  Future<AppleIdentityCredential> signIn();
}

/// AuthenticationServices 기반 Flutter plugin으로 Apple 인증을 수행한다.
class SignInWithAppleIdentityService implements AppleIdentityService {
  SignInWithAppleIdentityService({
    AppleCredentialRequester? requestCredential,
    AppleNonceGenerator? generateRawNonce,
  }) : _requestCredential = requestCredential ?? _requestNativeCredential,
       _generateRawNonce = generateRawNonce ?? generateNonce;

  final AppleCredentialRequester _requestCredential;
  final AppleNonceGenerator _generateRawNonce;

  @override
  Future<AppleIdentityCredential> signIn() async {
    final rawNonce = _generateRawNonce();
    // Apple에는 원문 대신 SHA-256만 전달하고 원문은 TLS로 Backend에 보내 replay를 막는다.
    final hashedNonce = sha256.convert(utf8.encode(rawNonce)).toString();
    final credential = await _requestCredential(hashedNonce);
    final identityToken = credential.identityToken?.trim();
    if (identityToken == null || identityToken.isEmpty) {
      throw const AppleSignInUnavailableException();
    }

    return AppleIdentityCredential(
      identityToken: identityToken,
      rawNonce: rawNonce,
      displayName: _displayName(credential),
    );
  }

  static Future<ApplePlatformCredential> _requestNativeCredential(
    String hashedNonce,
  ) async {
    final credential = await SignInWithApple.getAppleIDCredential(
      scopes: const [
        AppleIDAuthorizationScopes.email,
        AppleIDAuthorizationScopes.fullName,
      ],
      nonce: hashedNonce,
    );
    return ApplePlatformCredential(
      identityToken: credential.identityToken,
      givenName: credential.givenName,
      familyName: credential.familyName,
    );
  }

  String? _displayName(ApplePlatformCredential credential) {
    final parts = [credential.givenName, credential.familyName]
        .whereType<String>()
        .map((part) => part.trim())
        .where((part) => part.isNotEmpty)
        .toList(growable: false);
    return parts.isEmpty ? null : parts.join(' ');
  }
}

/// 플랫폼이 identity token을 반환하지 않아 로그인을 계속할 수 없음을 나타낸다.
class AppleSignInUnavailableException implements Exception {
  const AppleSignInUnavailableException();

  @override
  String toString() => 'Apple sign-in is unavailable';
}
