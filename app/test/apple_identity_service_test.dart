// 파일 의도: Apple nonce와 native credential 변환의 보안 계약을 검증한다.
// 선택 이유: 실제 계정 UI 없이 원문 nonce 비노출과 token 필수 조건을 결정적으로 재현한다.

import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/services/apple_identity_service.dart';

// raw nonce 자체가 Apple에 노출되지 않고 hash만 전달되며 결과가 다시 Backend 검증값과 묶이는지 보장한다.
void main() {
  test(
    'hashes a fresh nonce and returns the raw nonce with identity token',
    () async {
      String? requestedNonce;
      final service = SignInWithAppleIdentityService(
        generateRawNonce: () => 'raw-nonce-012345678901234567890123',
        requestCredential: (nonce) async {
          requestedNonce = nonce;
          return const ApplePlatformCredential(
            identityToken: 'apple-identity-token',
            givenName: ' Apple ',
            familyName: ' Learner ',
          );
        },
      );

      final credential = await service.signIn();

      expect(
        requestedNonce,
        sha256
            .convert(utf8.encode('raw-nonce-012345678901234567890123'))
            .toString(),
      );
      expect(credential.identityToken, 'apple-identity-token');
      expect(credential.rawNonce, 'raw-nonce-012345678901234567890123');
      expect(credential.displayName, 'Apple Learner');
    },
  );

  test('rejects a credential without an identity token', () async {
    final service = SignInWithAppleIdentityService(
      generateRawNonce: () => 'raw-nonce-012345678901234567890123',
      requestCredential: (_) async => const ApplePlatformCredential(),
    );

    await expectLater(
      service.signIn(),
      throwsA(isA<AppleSignInUnavailableException>()),
    );
  });
}
