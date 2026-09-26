// 파일 의도: 인라인 배너가 공간을 안정적으로 예약하고 광고 자원을 정리하는지 검증한다.
// 보장 대상: 설정 없음, load 성공·실패, 화면 제거와 orientation 폭 변경 lifecycle이다.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/services/banner_ad_service.dart';
import 'package:lingko_app/widgets/inline_banner_ad.dart';

void main() {
  testWidgets('설정된 배너는 로드 중 공간을 예약하고 성공한 광고를 표시한다', (tester) async {
    final service = FakeAppBannerAdService();

    await tester.pumpWidget(_host(service));

    expect(find.byKey(const ValueKey('banner-reserved-space')), findsOneWidget);
    expect(
      tester
          .getSize(find.byKey(const ValueKey('banner-reserved-space')))
          .height,
      70,
    );

    service.completeNext();
    await tester.pump();

    expect(find.byKey(const ValueKey('fake-banner-content')), findsOneWidget);
  });

  testWidgets('위젯이 제거되면 로드한 광고를 정확히 한 번 폐기한다', (tester) async {
    final service = FakeAppBannerAdService();
    await tester.pumpWidget(_host(service));
    service.completeNext();
    await tester.pump();

    await tester.pumpWidget(const MaterialApp(home: SizedBox()));

    expect(service.presentations.single.disposeCount, 1);
  });

  testWidgets('광고가 설정되지 않으면 공간과 SDK 요청을 만들지 않는다', (tester) async {
    final service = FakeAppBannerAdService(isConfigured: false);

    await tester.pumpWidget(_host(service));
    await tester.pump();

    expect(find.byKey(const ValueKey('banner-reserved-space')), findsNothing);
    expect(service.requests, isEmpty);
  });

  testWidgets('광고 로드 실패는 핵심 화면 오류 없이 예약 공간을 유지한다', (tester) async {
    final service = FakeAppBannerAdService(loadError: StateError('no fill'));

    await tester.pumpWidget(_host(service));
    await tester.pump();

    expect(find.byKey(const ValueKey('banner-reserved-space')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('사용 가능한 화면 폭이 바뀌면 새 adaptive 크기를 요청한다', (tester) async {
    final service = FakeAppBannerAdService();
    await tester.pumpWidget(_host(service));
    await tester.pump();
    service.completeNext();
    await tester.pump();

    await tester.pumpWidget(_host(service, size: const Size(800, 844)));
    await tester.pump();

    expect(service.requests.map((request) => request.width), [354, 764]);
  });
}

Widget _host(AppBannerAdService service, {Size size = const Size(390, 844)}) {
  return MaterialApp(
    home: Align(
      alignment: Alignment.topLeft,
      child: SizedBox(
        width: size.width,
        height: size.height,
        child: MediaQuery(
          data: MediaQueryData(size: size),
          child: Scaffold(
            body: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              child: InlineBannerAd(
                service: service,
                placement: AppBannerPlacement.review,
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

class FakeAppBannerAdService implements AppBannerAdService {
  FakeAppBannerAdService({this.isConfigured = true, this.loadError});

  @override
  final bool isConfigured;
  final Object? loadError;

  @override
  bool isConfiguredFor(AppBannerPlacement placement) => isConfigured;

  final List<({AppBannerPlacement placement, int width, int maxHeight})>
  requests = [];
  final List<FakeBannerPresentation> presentations = [];
  Completer<AppBannerAdPresentation?>? _pending;

  @override
  Future<AppBannerAdPresentation?> load({
    required AppBannerPlacement placement,
    required int width,
    required int maxHeight,
  }) {
    requests.add((placement: placement, width: width, maxHeight: maxHeight));
    final error = loadError;
    if (error != null) {
      return Future.error(error);
    }
    _pending = Completer<AppBannerAdPresentation?>();
    return _pending!.future;
  }

  void completeNext() {
    final presentation = FakeBannerPresentation();
    presentations.add(presentation);
    _pending!.complete(presentation);
  }
}

class FakeBannerPresentation implements AppBannerAdPresentation {
  int disposeCount = 0;

  @override
  double get height => 50;

  @override
  double get width => 320;

  @override
  Widget buildWidget() {
    return const SizedBox(
      key: ValueKey('fake-banner-content'),
      width: 320,
      height: 50,
    );
  }

  @override
  void dispose() {
    disposeCount++;
  }
}
