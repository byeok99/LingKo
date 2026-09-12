// AI 고지의 수신자 표시, 명시적 선택, 실패·구버전 차단과 HTTP 계약을 검증한다.
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/ai_processing_consent_api.dart';
import 'package:lingko_app/models/ai_processing_consent.dart';
import 'package:lingko_app/screens/ai_processing_consent_screen.dart';

void main() {
  test(
    'AI consent transport identifies the owner by token, not body',
    () async {
      final api = DartIoAiProcessingConsentApi(
        client: ApiClient(
          baseUrl: 'http://localhost:8080',
          getJsonTransport: (uri, timeout, headers) async {
            expect(uri.path, '/api/legal/ai-consent');
            expect(headers['Authorization'], 'Bearer test');
            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'granted': false,
                'noticeVersion': '2026-09-12',
              }),
            );
          },
          postJsonWithHeadersTransport: (uri, body, timeout, headers) async {
            expect(body, {'granted': true, 'noticeVersion': '2026-09-12'});
            expect(headers['Authorization'], 'Bearer test');
            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'granted': true,
                'noticeVersion': '2026-09-12',
              }),
            );
          },
        ),
      );
      expect((await api.fetchStatus(accessToken: 'test')).granted, isFalse);
      expect(
        (await api.record(accessToken: 'test', granted: true)).granted,
        isTrue,
      );
      expect(
        () => AiProcessingConsent.fromJson({'granted': 'true'}),
        throwsFormatException,
      );
    },
  );

  for (final action in [
    'decline',
    'allow',
    'failure',
    'withdraw',
    'outdated',
  ]) {
    testWidgets('AI disclosure supports $action without implicit permission', (
      tester,
    ) async {
      final choices = <bool>[];
      await tester.pumpWidget(
        MaterialApp(
          home: AiProcessingConsentScreen(
            status: AiProcessingConsent(
              granted: action == 'withdraw',
              noticeVersion: action == 'outdated' ? 'future' : '2026-09-12',
            ),
            onOpenPrivacy: () {},
            onSave: (granted) async {
              choices.add(granted);
              if (action == 'failure') throw Exception('offline');
              return AiProcessingConsent(
                granted: granted,
                noticeVersion: '2026-09-12',
              );
            },
          ),
        ),
      );
      expect(choices, isEmpty);
      expect(find.textContaining('Microsoft Azure AI Speech'), findsOneWidget);
      if (action == 'outdated') {
        expect(find.text('Allow sharing with Microsoft Azure'), findsNothing);
        expect(choices, isEmpty);
        return;
      }
      final label =
          action == 'decline'
              ? 'Not now — do not send'
              : action == 'withdraw'
              ? 'Withdraw permission'
              : 'Allow sharing with Microsoft Azure';
      await tester.scrollUntilVisible(find.text(label), 350);
      await tester.tap(find.text(label));
      await tester.pumpAndSettle();
      expect(choices, action == 'decline' ? [] : [action != 'withdraw']);
      if (action == 'failure') {
        expect(
          find.textContaining('Could not save your choice'),
          findsOneWidget,
        );
      }
    });
  }
}
