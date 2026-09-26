// 파일 의도: UMP 개인정보 선택과 Google Mobile Ads 초기화를 모든 광고 형식이 공유하게 한다.
// 선택 이유: 배너와 보상형 광고가 각자 동의 form과 SDK를 초기화하면 중복 화면·중복 요청이 발생한다.

import 'dart:async';

import 'package:google_mobile_ads/google_mobile_ads.dart';

/// 앱 화면이 광고 개인정보 상태와 설정 화면에 접근하는 최소 계약이다.
abstract interface class AdvertisingPrivacyService {
  /// 최신 UMP 상태가 광고 요청을 허용할 때만 Mobile Ads SDK를 초기화한다.
  Future<void> initialize();

  /// 현재 지역·메시지 설정에서 사용자가 다시 선택할 진입점이 필요한지 반환한다.
  Future<bool> isPrivacyOptionsRequired();

  /// 사용자가 광고 개인정보 선택을 변경할 수 있는 Google UMP form을 표시한다.
  Future<void> showPrivacyOptions();
}

/// 플랫폼 plugin 호출을 격리해 개인정보 초기화 순서와 실패 복구를 단위 test할 수 있게 한다.
abstract interface class MobileAdsPrivacyGateway {
  Future<void> requestConsentInfoUpdate();

  Future<void> loadAndShowConsentFormIfRequired();

  Future<bool> canRequestAdsNow();

  Future<void> configureTestDevice(String testDeviceId);

  Future<void> initializeMobileAds();

  Future<bool> isPrivacyOptionsRequired();

  Future<void> showPrivacyOptions();
}

/// UMP 선택을 갱신하고 허용된 경우에만 Mobile Ads를 한 번 초기화한다.
class GoogleMobileAdsPrivacyService implements AdvertisingPrivacyService {
  GoogleMobileAdsPrivacyService({
    MobileAdsPrivacyGateway? gateway,
    this.testDeviceId = const String.fromEnvironment('ADMOB_TEST_DEVICE_ID'),
  }) : gateway = gateway ?? GoogleMobileAdsPrivacyGateway();

  final MobileAdsPrivacyGateway gateway;

  /// test device ID는 운영 사용자 식별자가 아니라 로컬 기기의 광고 test mode 설정이다.
  final String testDeviceId;

  Future<void>? _initialization;

  @override
  Future<void> initialize() {
    final existing = _initialization;
    if (existing != null) {
      return existing;
    }

    final next = _initialize();
    _initialization = next;
    return next.catchError((Object error, StackTrace stackTrace) {
      // 네트워크 또는 UMP의 일시 실패를 영구 cache하지 않아 다음 광고 진입에서 복구한다.
      if (identical(_initialization, next)) {
        _initialization = null;
      }
      Error.throwWithStackTrace(error, stackTrace);
    });
  }

  Future<void> _initialize() async {
    var consentWasUpdated = false;
    try {
      await gateway.requestConsentInfoUpdate();
      consentWasUpdated = true;
    } catch (_) {
      // UMP 공식 지침에 따라 갱신 실패 시 이전 session의 유효한 선택이 있는지 확인한다.
      // 이전 선택도 없으면 광고 요청을 시작하지 않고 원래 오류를 다시 전달한다.
      if (!await gateway.canRequestAdsNow()) {
        rethrow;
      }
    }

    if (consentWasUpdated) {
      try {
        await gateway.loadAndShowConsentFormIfRequired();
      } catch (_) {
        // form load 실패도 이전 session의 유효한 선택이 있으면 광고를 완전히 중단하지 않는다.
        if (!await gateway.canRequestAdsNow()) {
          rethrow;
        }
        consentWasUpdated = false;
      }
      if (consentWasUpdated && !await gateway.canRequestAdsNow()) {
        throw StateError('Privacy consent does not allow ad requests');
      }
    }

    final normalizedTestDeviceId = testDeviceId.trim();
    if (normalizedTestDeviceId.isNotEmpty) {
      await gateway.configureTestDevice(normalizedTestDeviceId);
    }
    await gateway.initializeMobileAds();
  }

  @override
  Future<bool> isPrivacyOptionsRequired() {
    return gateway.isPrivacyOptionsRequired();
  }

  @override
  Future<void> showPrivacyOptions() {
    return gateway.showPrivacyOptions();
  }
}

/// Google Mobile Ads Flutter plugin의 callback API를 Future 기반 계약으로 변환한다.
class GoogleMobileAdsPrivacyGateway implements MobileAdsPrivacyGateway {
  @override
  Future<void> requestConsentInfoUpdate() {
    final result = Completer<void>();
    ConsentInformation.instance.requestConsentInfoUpdate(
      ConsentRequestParameters(),
      result.complete,
      (error) => result.completeError(
        StateError('Unable to update ad privacy consent: ${error.errorCode}'),
      ),
    );
    return result.future;
  }

  @override
  Future<void> loadAndShowConsentFormIfRequired() {
    final result = Completer<void>();
    ConsentForm.loadAndShowConsentFormIfRequired((error) {
      if (error == null) {
        result.complete();
      } else {
        result.completeError(
          StateError('Unable to show ad privacy form: ${error.errorCode}'),
        );
      }
    });
    return result.future;
  }

  @override
  Future<bool> canRequestAdsNow() {
    return ConsentInformation.instance.canRequestAds();
  }

  @override
  Future<void> configureTestDevice(String testDeviceId) {
    return MobileAds.instance.updateRequestConfiguration(
      RequestConfiguration(testDeviceIds: [testDeviceId]),
    );
  }

  @override
  Future<void> initializeMobileAds() async {
    await MobileAds.instance.initialize();
  }

  @override
  Future<bool> isPrivacyOptionsRequired() async {
    return await ConsentInformation.instance
            .getPrivacyOptionsRequirementStatus() ==
        PrivacyOptionsRequirementStatus.required;
  }

  @override
  Future<void> showPrivacyOptions() {
    final result = Completer<void>();
    ConsentForm.showPrivacyOptionsForm((error) {
      if (error == null) {
        result.complete();
      } else {
        result.completeError(
          StateError(
            'Unable to show advertising privacy options: ${error.errorCode}',
          ),
        );
      }
    });
    return result.future;
  }
}
