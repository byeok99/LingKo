// 파일 의도: AdMob 보상형 광고의 설정, 개인정보 동의, load/show callback을 앱 계약으로 정규화한다.
// 선택 이유: 화면과 quota 로직이 플랫폼 SDK 객체와 callback 수명주기에 직접 의존하지 않게 한다.

import 'dart:async';
import 'dart:io';

import 'package:google_mobile_ads/google_mobile_ads.dart';

/// Mobile Ads가 지원하는 앱 플랫폼만 명시적으로 구분한다.
enum RewardedAdPlatform { android, ios, unsupported }

/// 광고가 닫혔을 때 실제 reward callback이 있었는지를 나타낸다.
enum RewardedAdResult { earned, dismissed }

// Ad Unit ID는 광고 요청에 포함되는 공개 식별자다. Xcode가 `.env.local`을 읽지 않아
// 운영 iOS 빌드의 충전 버튼이 비활성화되지 않도록 앱 기본값으로 보존한다.
const _productionIosRewardedAdUnitId = 'ca-app-pub-5081228614816629/3927267131';

/// 광고 단위 ID가 설정되지 않은 빌드에서 SDK를 호출하려 할 때 발생한다.
class RewardedAdNotConfigured implements Exception {
  const RewardedAdNotConfigured();

  @override
  String toString() => 'Rewarded ad unit ID is not configured';
}

/// 플랫폼별 Rewarded Ad Unit ID를 빌드 설정에서 읽는다.
///
/// Ad Unit ID는 Secret이 아니며 환경 override로 운영 기본값을 test ID와 교체할 수 있다.
class RewardedAdConfiguration {
  const RewardedAdConfiguration({
    required this.androidAdUnitId,
    required this.iosAdUnitId,
  });

  const RewardedAdConfiguration.fromEnvironment()
    : androidAdUnitId = const String.fromEnvironment(
        'ADMOB_ANDROID_REWARDED_AD_UNIT_ID',
      ),
      iosAdUnitId = const String.fromEnvironment(
        'ADMOB_IOS_REWARDED_AD_UNIT_ID',
        defaultValue: _productionIosRewardedAdUnitId,
      );

  final String androidAdUnitId;
  final String iosAdUnitId;

  String? adUnitIdFor(RewardedAdPlatform platform) {
    final raw = switch (platform) {
      RewardedAdPlatform.android => androidAdUnitId,
      RewardedAdPlatform.ios => iosAdUnitId,
      RewardedAdPlatform.unsupported => '',
    };
    final trimmed = raw.trim();
    return trimmed.isEmpty ? null : trimmed;
  }

  bool isConfiguredFor(RewardedAdPlatform platform) {
    return adUnitIdFor(platform) != null;
  }
}

/// Home의 광고 진입점이 의존하는 최소 계약이다.
abstract interface class PracticeRewardAdService {
  bool get isConfigured;

  Future<RewardedAdResult> show({required String customData});
}

/// SDK 초기화와 광고 load를 테스트 가능한 경계로 감싼다.
abstract interface class RewardedAdGateway {
  /// testDeviceId가 있을 때만 해당 기기의 운영 Ad Unit 요청을 test mode로 보낸다.
  Future<void> initialize({String? testDeviceId});

  Future<RewardedAdPresentation> load({
    required String adUnitId,
    required String customData,
  });
}

/// 한 번만 표시할 수 있는 RewardedAd의 수명주기다.
abstract interface class RewardedAdPresentation {
  Future<RewardedAdResult> show();

  void dispose();
}

/// 설정된 플랫폼 ID로 광고 한 편을 load하고 reward 여부를 반환한다.
class GooglePracticeRewardAdService implements PracticeRewardAdService {
  GooglePracticeRewardAdService({
    this.configuration = const RewardedAdConfiguration.fromEnvironment(),
    RewardedAdPlatform? platform,
    RewardedAdGateway? gateway,
    this.testDeviceId = const String.fromEnvironment('ADMOB_TEST_DEVICE_ID'),
  }) : platform = platform ?? _currentPlatform(),
       gateway = gateway ?? GoogleMobileAdsRewardedAdGateway();

  final RewardedAdConfiguration configuration;
  final RewardedAdPlatform platform;
  final RewardedAdGateway gateway;
  // Google SDK가 출력한 test device ID는 보상 세션과 무관한 공개 요청 설정이다.
  // 소스에 기기 식별자를 남기지 않고 로컬 빌드에서만 dart-define으로 주입한다.
  final String testDeviceId;
  Future<void>? _initialization;

  @override
  bool get isConfigured => configuration.isConfiguredFor(platform);

  @override
  Future<RewardedAdResult> show({required String customData}) async {
    final adUnitId = configuration.adUnitIdFor(platform);
    if (adUnitId == null) {
      throw const RewardedAdNotConfigured();
    }

    await _ensureInitialized();
    final presentation = await gateway.load(
      adUnitId: adUnitId,
      customData: customData,
    );
    try {
      return await presentation.show();
    } finally {
      presentation.dispose();
    }
  }

  Future<void> _ensureInitialized() async {
    // 성공한 초기화는 process 동안 공유하되, 네트워크·UMP의 일시 오류는 다음 요청에서
    // 다시 시도할 수 있도록 실패한 Future만 cache에서 제거한다.
    final normalizedTestDeviceId = testDeviceId.trim();
    final initialization =
        _initialization ??= gateway.initialize(
          testDeviceId:
              normalizedTestDeviceId.isEmpty ? null : normalizedTestDeviceId,
        );
    try {
      await initialization;
    } catch (_) {
      if (identical(_initialization, initialization)) {
        _initialization = null;
      }
      rethrow;
    }
  }

  static RewardedAdPlatform _currentPlatform() {
    if (Platform.isAndroid) {
      return RewardedAdPlatform.android;
    }
    if (Platform.isIOS) {
      return RewardedAdPlatform.ios;
    }
    return RewardedAdPlatform.unsupported;
  }
}

/// UMP 개인정보 선택을 갱신한 뒤 Google Mobile Ads SDK를 초기화한다.
class GoogleMobileAdsRewardedAdGateway implements RewardedAdGateway {
  @override
  Future<void> initialize({String? testDeviceId}) async {
    await _updateConsent();
    if (!await ConsentInformation.instance.canRequestAds()) {
      throw StateError('Privacy consent does not allow ad requests');
    }
    // 운영 Ad Unit과 SSV 계약은 유지하고, 명시적으로 주입된 로컬 기기만
    // Google test mode로 요청해 invalid traffic과 실수로 인한 실제 광고 클릭을 막는다.
    if (testDeviceId != null) {
      await MobileAds.instance.updateRequestConfiguration(
        RequestConfiguration(testDeviceIds: [testDeviceId]),
      );
    }
    await MobileAds.instance.initialize();
  }

  Future<void> _updateConsent() async {
    final update = Completer<void>();
    ConsentInformation.instance.requestConsentInfoUpdate(
      ConsentRequestParameters(),
      () => update.complete(),
      (error) => update.completeError(
        StateError('Unable to update ad privacy consent: ${error.errorCode}'),
      ),
    );

    try {
      await update.future;
    } catch (_) {
      // 공식 UMP 지침처럼 update 실패 시에도 이전 session의 유효한 동의가 있으면
      // canRequestAds로 계속할 수 있게 한다. 실제 허용 여부는 initialize에서 확인한다.
      if (!await ConsentInformation.instance.canRequestAds()) {
        rethrow;
      }
      return;
    }

    final form = Completer<void>();
    ConsentForm.loadAndShowConsentFormIfRequired((error) {
      if (error == null) {
        form.complete();
      } else {
        form.completeError(
          StateError('Unable to show ad privacy form: ${error.errorCode}'),
        );
      }
    });
    await form.future;
  }

  @override
  Future<RewardedAdPresentation> load({
    required String adUnitId,
    required String customData,
  }) {
    final loaded = Completer<RewardedAdPresentation>();
    RewardedAd.load(
      adUnitId: adUnitId,
      request: const AdRequest(),
      rewardedAdLoadCallback: RewardedAdLoadCallback(
        onAdLoaded: (ad) {
          // Google SSV callback이 인증 사용자 세션을 찾을 수 있도록 show 전에 설정한다.
          ad.setServerSideOptions(
            ServerSideVerificationOptions(customData: customData),
          );
          loaded.complete(_GoogleRewardedAdPresentation(ad));
        },
        onAdFailedToLoad:
            (error) => loaded.completeError(
              StateError('Unable to load rewarded ad: ${error.code}'),
            ),
      ),
    );
    return loaded.future;
  }
}

class _GoogleRewardedAdPresentation implements RewardedAdPresentation {
  _GoogleRewardedAdPresentation(this.ad);

  final RewardedAd ad;
  bool _didShow = false;

  @override
  Future<RewardedAdResult> show() {
    if (_didShow) {
      throw StateError('Rewarded ad can only be shown once');
    }
    _didShow = true;

    final result = Completer<RewardedAdResult>();
    var earned = false;
    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (_) {
        if (!result.isCompleted) {
          result.complete(
            earned ? RewardedAdResult.earned : RewardedAdResult.dismissed,
          );
        }
      },
      onAdFailedToShowFullScreenContent: (_, error) {
        if (!result.isCompleted) {
          result.completeError(
            StateError('Unable to show rewarded ad: ${error.code}'),
          );
        }
      },
    );
    ad.show(onUserEarnedReward: (_, _) => earned = true);
    return result.future;
  }

  @override
  void dispose() => ad.dispose();
}
