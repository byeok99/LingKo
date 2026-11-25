// 파일 의도: E2E 테스트 빌드 전용 진입점이다.
// 실행할 때마다 로컬 백엔드에서 테스트 세션을 받아 저장한 뒤 앱을 띄운다.
// 주의: lib/main.dart는 이 파일을 import하지 않는다. 운영 빌드에 섞이지 않게 하기 위해서다.

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'app/lingko_app.dart';
import 'models/auth_session.dart';
import 'services/auth_session_store.dart';

// 동의 흐름을 테스트할 때만 --dart-define=LINGKO_E2E_CONSENTED=false로 빌드한다.
const _consented = bool.fromEnvironment(
  'LINGKO_E2E_CONSENTED',
  defaultValue: true,
);

const _localHosts = {'127.0.0.1', 'localhost', '10.0.2.2'};

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 안전장치 1: release 빌드에서는 실행하지 않는다.
  if (kReleaseMode) {
    throw StateError('E2E 진입점은 release 빌드에서 실행할 수 없다.');
  }

  // 안전장치 2: 로컬 백엔드가 아니면 시작하지 않는다.
  final baseUrl = Uri.parse(resolveLingKoApiBaseUrl());
  if (!_localHosts.contains(baseUrl.host)) {
    throw StateError('E2E는 로컬 백엔드에서만 실행한다: $baseUrl');
  }

  final json = await ApiClient(baseUrl: baseUrl.toString()).postJson(
    '/test-support/sessions?consented=$_consented',
    const <String, Object?>{},
  );
  await AuthSessionStore().save(AuthSession.fromJson(json));

  runApp(const LingKoApp());
}