// 파일 의도: 문서 열기 서비스가 만드는 공개 URL을 회귀 테스트로 고정한다.
// 보장 대상: 백엔드 `LegalDocument`의 경로 구간과 일치할 것, 언어 파라미터가 붙을 것,
// API base URL을 그대로 따를 것. 세 가지 중 하나라도 어긋나면 사용자가 문서를 못 연다.

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/models/consent_selection.dart';
import 'package:lingko_app/services/legal_document_launcher.dart';
import 'package:url_launcher/url_launcher.dart';

void main() {
  test('문서 경로는 백엔드가 정의한 구간과 일치한다', () {
    // 백엔드 LegalDocument의 path와 같아야 한다. 어긋나면 404가 된다.
    expect(
      UrlLauncherLegalDocumentLauncher.pathSegmentOf(
        ConsentDocument.termsOfService,
      ),
      'terms',
    );
    expect(
      UrlLauncherLegalDocumentLauncher.pathSegmentOf(
        ConsentDocument.privacyPolicy,
      ),
      'privacy',
    );
  });

  test('공개 URL은 API base URL과 언어 파라미터를 따른다', () {
    final launcher = UrlLauncherLegalDocumentLauncher(
      baseUrl: 'https://api.lingko.example',
    );

    expect(
      launcher.resolve(ConsentDocument.termsOfService, language: 'ko'),
      Uri.parse('https://api.lingko.example/legal/terms?lang=ko'),
    );
    expect(
      launcher.resolve(ConsentDocument.privacyPolicy, language: 'en'),
      Uri.parse('https://api.lingko.example/legal/privacy?lang=en'),
    );
  });

  test('base URL에 경로가 붙어 있어도 문서는 루트 경로에서 연다', () {
    // 서버가 /legal을 루트에 두므로 base URL의 하위 경로를 이어붙이면 안 된다.
    final launcher = UrlLauncherLegalDocumentLauncher(
      baseUrl: 'https://api.lingko.example/api/v1',
    );

    expect(
      launcher.resolve(ConsentDocument.termsOfService).path,
      '/legal/terms',
    );
  });

  test('언어를 지정하지 않으면 영어로 연다', () {
    // 앱 UI가 영어 기준이라 문서도 같은 언어로 시작해야 문맥이 끊기지 않는다.
    final launcher = UrlLauncherLegalDocumentLauncher(
      baseUrl: 'https://api.lingko.example',
    );

    expect(launcher.resolve(ConsentDocument.privacyPolicy).query, 'lang=en');
  });

  test('법무 문서는 외부 앱이 아니라 제한된 인앱 WebView로 연다', () async {
    Uri? openedUrl;
    LaunchMode? openedMode;
    WebViewConfiguration? openedConfiguration;
    final launcher = UrlLauncherLegalDocumentLauncher(
      baseUrl: 'https://api.lingko.example',
      launch: (url, {required mode, required webViewConfiguration}) async {
        openedUrl = url;
        openedMode = mode;
        openedConfiguration = webViewConfiguration;
        return true;
      },
    );

    final opened = await launcher.open(ConsentDocument.termsOfService);

    expect(opened, isTrue);
    expect(
      openedUrl,
      Uri.parse('https://api.lingko.example/legal/terms?lang=en'),
    );
    expect(openedMode, LaunchMode.inAppWebView);
    // 정적 법무 문서는 script나 client storage가 필요하지 않다. 불필요한 WebView
    // 실행 권한을 켜지 않는 계약을 회귀 테스트로 고정한다.
    expect(openedConfiguration?.enableJavaScript, isFalse);
    expect(openedConfiguration?.enableDomStorage, isFalse);
  });
}
