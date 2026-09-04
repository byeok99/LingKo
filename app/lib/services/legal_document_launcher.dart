// 파일 의도: 약관·처리방침 공개 URL을 앱 내부 WebView로 여는 경계를 제공한다.
// 선택 이유: 플랫폼 화면 실행은 UI에서 직접 부르지 않고 서비스로 감싸, 두 법무 진입점이
// 같은 표시 방식과 보안 옵션을 사용하고 테스트에서 실제 WebView를 띄우지 않게 한다.

import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../models/consent_selection.dart';

/// 플랫폼 URL launcher를 테스트 가능한 경계로 감싼 함수 계약이다.
///
/// 정적 법무 문서는 JavaScript와 DOM 저장소가 필요하지 않으므로 호출자가 전달한 제한
/// 설정까지 테스트에서 확인할 수 있게 별도 typedef로 둔다.
typedef LegalDocumentUrlLaunch =
    Future<bool> Function(
      Uri url, {
      required LaunchMode mode,
      required WebViewConfiguration webViewConfiguration,
    });

/// 문서 열기 요청을 기기 동작으로 옮기는 경계다.
abstract class LegalDocumentLauncher {
  /// 문서를 연다.
  ///
  /// @return 실제로 열렸으면 true. 플랫폼 WebView를 열 수 없거나 실패하면 false를
  /// 돌려주며, 호출자는 이 경우 사용자에게 대체 안내를 보여줘야 한다. 예외를 던지지
  /// 않는 이유는 WebView 초기화 실패가 앱 전체를 중단할 오류는 아니기 때문이다.
  Future<bool> open(ConsentDocument document, {String language});
}

/// 백엔드가 서빙하는 공개 URL을 앱 내부 WebView로 여는 기본 구현이다.
///
/// 서버 페이지를 그대로 여는 이유는 약관이 개정되었을 때 앱 업데이트를 기다리지 않고
/// 최신본이 보여야 하기 때문이다. 같은 URL은 스토어 심사에 제출하는 공개 주소로도 쓴다.
class UrlLauncherLegalDocumentLauncher implements LegalDocumentLauncher {
  UrlLauncherLegalDocumentLauncher({
    String? baseUrl,
    LegalDocumentUrlLaunch? launch,
  }) : baseUrl = Uri.parse(baseUrl ?? resolveLingKoApiBaseUrl()),
       _launch = launch ?? _launchUrl;

  /// 문서를 서빙하는 서버 주소다. API와 같은 호스트를 쓴다.
  final Uri baseUrl;

  final LegalDocumentUrlLaunch _launch;

  static Future<bool> _launchUrl(
    Uri url, {
    required LaunchMode mode,
    required WebViewConfiguration webViewConfiguration,
  }) => launchUrl(url, mode: mode, webViewConfiguration: webViewConfiguration);

  /// 문서 종류를 서버 경로 구간으로 옮긴다. 백엔드 `LegalDocument`의 path와 같아야 한다.
  static String pathSegmentOf(ConsentDocument document) => switch (document) {
    ConsentDocument.termsOfService => 'terms',
    ConsentDocument.privacyPolicy => 'privacy',
  };

  /// 문서의 공개 URL을 만든다.
  Uri resolve(ConsentDocument document, {String language = 'en'}) {
    return baseUrl.resolve('/legal/${pathSegmentOf(document)}?lang=$language');
  }

  @override
  Future<bool> open(ConsentDocument document, {String language = 'en'}) async {
    final url = resolve(document, language: language);
    try {
      final launched = await _launch(
        url,
        // iOS는 닫기 동작을 제공하는 SFSafariViewController를 앱 위에 표시하고,
        // Android는 앱 내부 WebView를 사용한다. 정적 HTML만 필요하므로 script와
        // client storage를 끄며, 서버도 CSP로 외부 자원 실행을 차단한다.
        mode: LaunchMode.inAppWebView,
        webViewConfiguration: const WebViewConfiguration(
          enableJavaScript: false,
          enableDomStorage: false,
        ),
      );
      if (!launched && kDebugMode) {
        // 실패 원인은 플랫폼이 bool만 반환해 구분할 수 없으므로 개발 빌드에서만
        // 대상 URL을 남긴다. 문서 내용이나 사용자 데이터는 포함하지 않는다.
        debugPrint('LegalDocumentLauncher: 인앱 WebView를 열지 못했습니다. url=$url');
      }
      return launched;
    } catch (error) {
      // WebView를 열 수 없는 기기 상태에서도 앱을 멈추지 않고 실패를 알려
      // 호출자가 대체 안내를 보여주게 한다.
      if (kDebugMode) {
        debugPrint('LegalDocumentLauncher: url=$url 열기 실패 — $error');
      }
      return false;
    }
  }
}
