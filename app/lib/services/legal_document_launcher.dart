// 파일 의도: 약관·처리방침 공개 URL을 기기 브라우저로 여는 경계를 제공한다.
// 선택 이유: 브라우저 실행은 플랫폼 기능이라 화면에서 직접 부르지 않고 서비스로 감싼다.
// 테스트에서 실제 브라우저를 띄우지 않고 요청한 주소만 확인할 수 있게 하려는 목적도 있다.

import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../models/consent_selection.dart';

/// 문서 열기 요청을 기기 동작으로 옮기는 경계다.
abstract class LegalDocumentLauncher {
  /// 문서를 연다.
  ///
  /// @return 실제로 열렸으면 true. 열 수 있는 앱이 없거나 실패하면 false를 돌려주며,
  /// 호출자는 이 경우 사용자에게 대체 안내를 보여줘야 한다. 예외를 던지지 않는 이유는
  /// 브라우저 부재가 앱의 오류가 아니라 기기 상태이기 때문이다.
  Future<bool> open(ConsentDocument document, {String language});
}

/// 백엔드가 서빙하는 공개 URL을 외부 브라우저로 여는 기본 구현이다.
///
/// 앱 안에서 문서를 렌더링하지 않고 서버 페이지를 여는 이유는, 약관이 개정되었을 때
/// 앱 업데이트를 기다리지 않고 즉시 최신본이 보여야 하기 때문이다. 같은 URL이
/// 스토어 심사에 제출하는 공개 주소로도 쓰인다.
class UrlLauncherLegalDocumentLauncher implements LegalDocumentLauncher {
  UrlLauncherLegalDocumentLauncher({String? baseUrl})
    : baseUrl = Uri.parse(baseUrl ?? resolveLingKoApiBaseUrl());

  /// 문서를 서빙하는 서버 주소다. API와 같은 호스트를 쓴다.
  final Uri baseUrl;

  /// 문서 종류를 서버 경로 구간으로 옮긴다. 백엔드 `LegalDocument`의 path와 같아야 한다.
  static String pathSegmentOf(ConsentDocument document) => switch (document) {
    ConsentDocument.termsOfService => 'terms',
    ConsentDocument.privacyPolicy => 'privacy',
  };

  /// 문서의 공개 URL을 만든다.
  Uri resolve(ConsentDocument document, {String language = 'en'}) {
    return baseUrl.resolve(
      '/legal/${pathSegmentOf(document)}?lang=$language',
    );
  }

  @override
  Future<bool> open(ConsentDocument document, {String language = 'en'}) async {
    final url = resolve(document, language: language);
    try {
      final launched = await launchUrl(
        url,
        // 앱 안에 겹쳐 띄우지 않고 브라우저로 넘긴다. 약관은 길어서 사용자가
        // 확대·검색·저장을 쓰게 되는데, 그 기능은 브라우저가 이미 갖고 있다.
        mode: LaunchMode.externalApplication,
      );
      if (!launched && kDebugMode) {
        // 실패 원인이 대부분 플랫폼 설정에 있는데 bool만으로는 구분할 수 없다.
        // 가장 흔한 원인을 개발 빌드에서만 함께 남긴다.
        debugPrint(
          'LegalDocumentLauncher: 브라우저를 열지 못했습니다. url=$url. '
          'Android는 AndroidManifest의 <queries>에 http/https VIEW intent가, '
          'iOS는 Info.plist의 LSApplicationQueriesSchemes가 필요합니다.',
        );
      }
      return launched;
    } catch (error) {
      // 열 수 있는 앱이 없는 기기가 있다. 앱을 멈추는 대신 실패를 알려
      // 호출자가 대체 안내를 보여주게 한다.
      if (kDebugMode) {
        debugPrint('LegalDocumentLauncher: url=$url 열기 실패 — $error');
      }
      return false;
    }
  }
}
