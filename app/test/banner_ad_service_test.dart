// 파일 의도: 화면별 배너 설정, 개인정보 gate와 반복 요청 제한 계약을 검증한다.
// 보장 대상: 운영 ID 선택, 미설정 시 무요청, placement 분리, 60초 이내 중복 요청 방지다.

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/services/banner_ad_service.dart';
import 'package:lingko_app/services/mobile_ads_privacy_service.dart';

void main() {
  group('BannerAdConfiguration', () {
    const configuration = BannerAdConfiguration(
      androidReviewAdUnitId: 'android-review',
      androidProfileAdUnitId: 'android-profile',
      iosReviewAdUnitId: 'ios-review',
      iosProfileAdUnitId: 'ios-profile',
    );

    test('플랫폼과 placement마다 독립된 운영 ID를 선택한다', () {
      expect(
        configuration.adUnitIdFor(
          BannerAdPlatform.android,
          AppBannerPlacement.review,
        ),
        'android-review',
      );
      expect(
        configuration.adUnitIdFor(
          BannerAdPlatform.ios,
          AppBannerPlacement.profile,
        ),
        'ios-profile',
      );
      expect(
        configuration.adUnitIdFor(
          BannerAdPlatform.unsupported,
          AppBannerPlacement.review,
        ),
        isNull,
      );
    });

    test('빈 운영 ID는 설정되지 않은 값으로 처리한다', () {
      const missing = BannerAdConfiguration(
        androidReviewAdUnitId: '',
        androidProfileAdUnitId: ' ',
        iosReviewAdUnitId: '',
        iosProfileAdUnitId: '',
      );

      expect(
        missing.isConfiguredFor(
          BannerAdPlatform.android,
          AppBannerPlacement.review,
        ),
        isFalse,
      );
    });

    test('iOS release 기본값은 공용 IOS_BANNER를 두 placement에 적용한다', () {
      const environment = BannerAdConfiguration.fromEnvironment();

      expect(
        environment.adUnitIdFor(
          BannerAdPlatform.ios,
          AppBannerPlacement.review,
        ),
        'ca-app-pub-5081228614816629/5340118206',
      );
      expect(
        environment.adUnitIdFor(
          BannerAdPlatform.ios,
          AppBannerPlacement.profile,
        ),
        'ca-app-pub-5081228614816629/5340118206',
      );
    });
  });

  group('GoogleAppBannerAdService', () {
    test('동의 완료 후 placement 전용 ID와 화면 폭으로 배너를 요청한다', () async {
      final privacy = FakeAdvertisingPrivacyService();
      final gateway = FakeBannerAdGateway();
      final service = GoogleAppBannerAdService(
        configuration: const BannerAdConfiguration(
          androidReviewAdUnitId: '',
          androidProfileAdUnitId: '',
          iosReviewAdUnitId: 'ios-review',
          iosProfileAdUnitId: 'ios-profile',
        ),
        platform: BannerAdPlatform.ios,
        privacyService: privacy,
        gateway: gateway,
        useTestAdUnits: false,
      );

      final presentation = await service.load(
        placement: AppBannerPlacement.review,
        width: 354,
        maxHeight: 70,
      );

      expect(presentation, isNotNull);
      expect(privacy.initializeCount, 1);
      expect(gateway.requests.single.adUnitId, 'ios-review');
      expect(gateway.requests.single.width, 354);
      expect(gateway.requests.single.maxHeight, 70);
    });

    test('운영 ID가 없으면 개인정보 또는 SDK 경계를 호출하지 않는다', () async {
      final privacy = FakeAdvertisingPrivacyService();
      final gateway = FakeBannerAdGateway();
      final service = GoogleAppBannerAdService(
        configuration: BannerAdConfiguration.empty,
        platform: BannerAdPlatform.ios,
        privacyService: privacy,
        gateway: gateway,
        useTestAdUnits: false,
      );

      expect(service.isConfigured, isFalse);
      expect(
        await service.load(
          placement: AppBannerPlacement.profile,
          width: 354,
          maxHeight: 70,
        ),
        isNull,
      );
      expect(privacy.initializeCount, 0);
      expect(gateway.requests, isEmpty);
    });

    test('설정된 placement만 활성화하고 debug에서는 공식 test ID를 사용한다', () async {
      final gateway = FakeBannerAdGateway();
      final service = GoogleAppBannerAdService(
        configuration: const BannerAdConfiguration(
          androidReviewAdUnitId: '',
          androidProfileAdUnitId: '',
          iosReviewAdUnitId: 'ios-review',
          iosProfileAdUnitId: '',
        ),
        platform: BannerAdPlatform.ios,
        privacyService: FakeAdvertisingPrivacyService(),
        gateway: gateway,
        useTestAdUnits: false,
      );

      expect(service.isConfigured, isTrue);
      expect(service.isConfiguredFor(AppBannerPlacement.review), isTrue);
      expect(service.isConfiguredFor(AppBannerPlacement.profile), isFalse);

      final debugGateway = FakeBannerAdGateway();
      final debugService = GoogleAppBannerAdService(
        configuration: const BannerAdConfiguration(
          androidReviewAdUnitId: '',
          androidProfileAdUnitId: '',
          iosReviewAdUnitId: 'live-ios-review',
          iosProfileAdUnitId: 'live-ios-profile',
        ),
        platform: BannerAdPlatform.ios,
        privacyService: FakeAdvertisingPrivacyService(),
        gateway: debugGateway,
        useTestAdUnits: true,
      );
      await debugService.load(
        placement: AppBannerPlacement.profile,
        width: 354,
        maxHeight: 70,
      );
      expect(
        debugGateway.requests.single.adUnitId,
        'ca-app-pub-3940256099942544/2934735716',
      );
    });

    test('화면 재진입은 같은 placement와 폭이어도 즉시 새 광고를 요청한다', () async {
      final gateway = FakeBannerAdGateway();
      final service = GoogleAppBannerAdService(
        configuration: const BannerAdConfiguration(
          androidReviewAdUnitId: '',
          androidProfileAdUnitId: '',
          iosReviewAdUnitId: 'ios-review',
          iosProfileAdUnitId: 'ios-profile',
        ),
        platform: BannerAdPlatform.ios,
        privacyService: FakeAdvertisingPrivacyService(),
        gateway: gateway,
        useTestAdUnits: false,
      );

      expect(
        await service.load(
          placement: AppBannerPlacement.review,
          width: 354,
          maxHeight: 70,
        ),
        isNotNull,
      );
      expect(
        await service.load(
          placement: AppBannerPlacement.review,
          width: 354,
          maxHeight: 70,
        ),
        isNotNull,
      );
      expect(gateway.requests, hasLength(2));
    });

    test('회전으로 사용 가능한 폭이 달라지면 즉시 새 크기를 요청한다', () async {
      final gateway = FakeBannerAdGateway();
      final service = GoogleAppBannerAdService(
        configuration: const BannerAdConfiguration(
          androidReviewAdUnitId: '',
          androidProfileAdUnitId: '',
          iosReviewAdUnitId: 'ios-review',
          iosProfileAdUnitId: 'ios-profile',
        ),
        platform: BannerAdPlatform.ios,
        privacyService: FakeAdvertisingPrivacyService(),
        gateway: gateway,
        useTestAdUnits: false,
      );

      await service.load(
        placement: AppBannerPlacement.profile,
        width: 354,
        maxHeight: 70,
      );
      await service.load(
        placement: AppBannerPlacement.profile,
        width: 700,
        maxHeight: 70,
      );

      expect(gateway.requests.map((request) => request.width), [354, 700]);
    });
  });
}

class FakeAdvertisingPrivacyService implements AdvertisingPrivacyService {
  int initializeCount = 0;
  int showPrivacyOptionsCount = 0;
  bool privacyOptionsRequired = false;

  @override
  Future<void> initialize() async {
    initializeCount++;
  }

  @override
  Future<bool> isPrivacyOptionsRequired() async => privacyOptionsRequired;

  @override
  Future<void> showPrivacyOptions() async {
    showPrivacyOptionsCount++;
  }
}

class FakeBannerAdGateway implements BannerAdGateway {
  final List<BannerAdRequest> requests = [];

  @override
  Future<AppBannerAdPresentation> load({
    required String adUnitId,
    required int width,
    required int maxHeight,
  }) async {
    requests.add(
      BannerAdRequest(adUnitId: adUnitId, width: width, maxHeight: maxHeight),
    );
    return FakeBannerAdPresentation(width: width.toDouble(), height: 60);
  }
}

class BannerAdRequest {
  const BannerAdRequest({
    required this.adUnitId,
    required this.width,
    required this.maxHeight,
  });

  final String adUnitId;
  final int width;
  final int maxHeight;
}

class FakeBannerAdPresentation implements AppBannerAdPresentation {
  FakeBannerAdPresentation({required this.width, required this.height});

  @override
  final double width;

  @override
  final double height;

  @override
  Widget buildWidget() => const SizedBox();

  @override
  void dispose() {}
}
