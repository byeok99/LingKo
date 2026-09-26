// 파일 의도: 광고 동의와 Mobile Ads 초기화가 중복 없이 개인정보 선택을 존중하는지 검증한다.
// 보장 대상: 최신 UMP 상태 확인, 광고 요청 허용 확인, 실패 후 재시도, 개인정보 설정 진입점이다.

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/services/mobile_ads_privacy_service.dart';

void main() {
  group('GoogleMobileAdsPrivacyService', () {
    test('동의 확인과 필수 form을 거친 뒤 SDK를 한 번만 초기화한다', () async {
      final gateway = FakeMobileAdsPrivacyGateway();
      final service = GoogleMobileAdsPrivacyService(
        gateway: gateway,
        testDeviceId: '  test-device-id  ',
      );

      await Future.wait([service.initialize(), service.initialize()]);

      expect(gateway.updateCount, 1);
      expect(gateway.formCount, 1);
      expect(gateway.canRequestAdsCount, 1);
      expect(gateway.configuredTestDeviceIds, ['test-device-id']);
      expect(gateway.initializeCount, 1);
    });

    test('동의 갱신 실패여도 이전 상태가 광고를 허용하면 계속 초기화한다', () async {
      final gateway = FakeMobileAdsPrivacyGateway(
        updateError: StateError('network unavailable'),
        canRequestAds: true,
      );
      final service = GoogleMobileAdsPrivacyService(gateway: gateway);

      await service.initialize();

      expect(gateway.formCount, 0);
      expect(gateway.canRequestAdsCount, 1);
      expect(gateway.initializeCount, 1);
    });

    test('동의 form 실패여도 이전 상태가 광고를 허용하면 계속 초기화한다', () async {
      final gateway = FakeMobileAdsPrivacyGateway(
        formError: StateError('form unavailable'),
        canRequestAds: true,
      );
      final service = GoogleMobileAdsPrivacyService(gateway: gateway);

      await service.initialize();

      expect(gateway.formCount, 1);
      expect(gateway.canRequestAdsCount, 1);
      expect(gateway.initializeCount, 1);
    });

    test('광고 요청이 허용되지 않으면 초기화하지 않고 다음 호출에서 재시도한다', () async {
      final gateway = FakeMobileAdsPrivacyGateway(canRequestAds: false);
      final service = GoogleMobileAdsPrivacyService(gateway: gateway);

      await expectLater(service.initialize(), throwsStateError);
      expect(gateway.initializeCount, 0);

      gateway.canRequestAds = true;
      await service.initialize();

      expect(gateway.updateCount, 2);
      expect(gateway.initializeCount, 1);
    });

    test('필요한 지역에서 개인정보 설정 진입점을 제공한다', () async {
      final gateway = FakeMobileAdsPrivacyGateway(privacyOptionsRequired: true);
      final service = GoogleMobileAdsPrivacyService(gateway: gateway);

      expect(await service.isPrivacyOptionsRequired(), isTrue);
      await service.showPrivacyOptions();

      expect(gateway.showPrivacyOptionsCount, 1);
    });
  });
}

class FakeMobileAdsPrivacyGateway implements MobileAdsPrivacyGateway {
  FakeMobileAdsPrivacyGateway({
    this.updateError,
    this.formError,
    this.canRequestAds = true,
    this.privacyOptionsRequired = false,
  });

  final Object? updateError;
  final Object? formError;
  bool canRequestAds;
  final bool privacyOptionsRequired;
  int updateCount = 0;
  int formCount = 0;
  int canRequestAdsCount = 0;
  int initializeCount = 0;
  int showPrivacyOptionsCount = 0;
  final List<String> configuredTestDeviceIds = [];

  @override
  Future<bool> canRequestAdsNow() async {
    canRequestAdsCount++;
    return canRequestAds;
  }

  @override
  Future<void> configureTestDevice(String testDeviceId) async {
    configuredTestDeviceIds.add(testDeviceId);
  }

  @override
  Future<void> initializeMobileAds() async {
    initializeCount++;
  }

  @override
  Future<bool> isPrivacyOptionsRequired() async => privacyOptionsRequired;

  @override
  Future<void> loadAndShowConsentFormIfRequired() async {
    formCount++;
    final error = formError;
    if (error != null) {
      throw error;
    }
  }

  @override
  Future<void> requestConsentInfoUpdate() async {
    updateCount++;
    final error = updateError;
    if (error != null) {
      throw error;
    }
  }

  @override
  Future<void> showPrivacyOptions() async {
    showPrivacyOptionsCount++;
  }
}
