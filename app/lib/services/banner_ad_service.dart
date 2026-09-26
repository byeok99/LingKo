// 파일 의도: Review·Profile 인라인 배너의 설정과 플랫폼 광고 객체를 정규화한다.
// 선택 이유: 화면이 AdMob SDK와 Ad Unit ID를 직접 다루지 않고 lifecycle만 소비하게 한다.

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

import 'mobile_ads_privacy_service.dart';

// Ad Unit ID는 광고 요청에 포함되는 공개 식별자다. 현재 AdMob에서 만든 IOS_BANNER를
// 두 화면의 release 기본값으로 사용하고, 화면별 성과 분리가 필요하면 dart-define으로 덮어쓴다.
const _productionIosBannerAdUnitId = 'ca-app-pub-5081228614816629/5340118206';

/// 수익과 정책 상태를 화면별로 분리하기 위한 배너 placement다.
enum AppBannerPlacement { review, profile }

/// Mobile Ads가 지원하는 앱 플랫폼만 명시적으로 구분한다.
enum BannerAdPlatform { android, ios, unsupported }

/// 플랫폼·화면별 Ad Unit ID를 빌드 설정에서 선택한다.
class BannerAdConfiguration {
  const BannerAdConfiguration({
    required this.androidReviewAdUnitId,
    required this.androidProfileAdUnitId,
    required this.iosReviewAdUnitId,
    required this.iosProfileAdUnitId,
  });

  const BannerAdConfiguration.fromEnvironment()
    : androidReviewAdUnitId = const String.fromEnvironment(
        'ADMOB_ANDROID_REVIEW_BANNER_AD_UNIT_ID',
      ),
      androidProfileAdUnitId = const String.fromEnvironment(
        'ADMOB_ANDROID_PROFILE_BANNER_AD_UNIT_ID',
      ),
      iosReviewAdUnitId = const String.fromEnvironment(
        'ADMOB_IOS_REVIEW_BANNER_AD_UNIT_ID',
        defaultValue: _productionIosBannerAdUnitId,
      ),
      iosProfileAdUnitId = const String.fromEnvironment(
        'ADMOB_IOS_PROFILE_BANNER_AD_UNIT_ID',
        defaultValue: _productionIosBannerAdUnitId,
      );

  static const empty = BannerAdConfiguration(
    androidReviewAdUnitId: '',
    androidProfileAdUnitId: '',
    iosReviewAdUnitId: '',
    iosProfileAdUnitId: '',
  );

  final String androidReviewAdUnitId;
  final String androidProfileAdUnitId;
  final String iosReviewAdUnitId;
  final String iosProfileAdUnitId;

  String? adUnitIdFor(BannerAdPlatform platform, AppBannerPlacement placement) {
    final raw = switch ((platform, placement)) {
      (BannerAdPlatform.android, AppBannerPlacement.review) =>
        androidReviewAdUnitId,
      (BannerAdPlatform.android, AppBannerPlacement.profile) =>
        androidProfileAdUnitId,
      (BannerAdPlatform.ios, AppBannerPlacement.review) => iosReviewAdUnitId,
      (BannerAdPlatform.ios, AppBannerPlacement.profile) => iosProfileAdUnitId,
      (BannerAdPlatform.unsupported, _) => '',
    };
    final normalized = raw.trim();
    return normalized.isEmpty ? null : normalized;
  }

  bool isConfiguredFor(
    BannerAdPlatform platform,
    AppBannerPlacement placement,
  ) {
    return adUnitIdFor(platform, placement) != null;
  }
}

/// 화면에 mount할 광고와 실제 크기, 네이티브 자원 정리 계약이다.
abstract interface class AppBannerAdPresentation {
  double get width;

  double get height;

  Widget buildWidget();

  void dispose();
}

/// Google SDK load callback을 test 가능한 배너 요청 경계로 격리한다.
abstract interface class BannerAdGateway {
  Future<AppBannerAdPresentation> load({
    required String adUnitId,
    required int width,
    required int maxHeight,
  });
}

/// Review·Profile 화면이 의존하는 배너 광고 계약이다.
abstract interface class AppBannerAdService {
  bool get isConfigured;

  bool isConfiguredFor(AppBannerPlacement placement);

  Future<AppBannerAdPresentation?> load({
    required AppBannerPlacement placement,
    required int width,
    required int maxHeight,
  });
}

/// 개인정보 gate 뒤에 inline adaptive banner를 요청한다.
class GoogleAppBannerAdService implements AppBannerAdService {
  GoogleAppBannerAdService({
    this.configuration = const BannerAdConfiguration.fromEnvironment(),
    BannerAdPlatform? platform,
    required this.privacyService,
    BannerAdGateway? gateway,
    this.useTestAdUnits = kDebugMode,
  }) : platform = platform ?? _currentPlatform(),
       gateway = gateway ?? GoogleBannerAdGateway();

  static const _androidTestAdUnitId = 'ca-app-pub-3940256099942544/9214589741';
  static const _iosTestAdUnitId = 'ca-app-pub-3940256099942544/2934735716';

  final BannerAdConfiguration configuration;
  final BannerAdPlatform platform;
  final AdvertisingPrivacyService privacyService;
  final BannerAdGateway gateway;

  /// debug build만 Google 공식 test unit을 사용한다. release에서는 운영 ID가 없으면 요청하지 않는다.
  final bool useTestAdUnits;

  @override
  bool get isConfigured {
    return isConfiguredFor(AppBannerPlacement.review) ||
        isConfiguredFor(AppBannerPlacement.profile);
  }

  @override
  bool isConfiguredFor(AppBannerPlacement placement) {
    return _adUnitIdFor(placement) != null;
  }

  @override
  Future<AppBannerAdPresentation?> load({
    required AppBannerPlacement placement,
    required int width,
    required int maxHeight,
  }) async {
    final adUnitId = _adUnitIdFor(placement);
    if (adUnitId == null || width <= 0 || maxHeight <= 0) {
      return null;
    }

    // 탭 전환은 기존 BannerAd가 dispose된 뒤 새 Widget이 생성되는 정상 navigation이다.
    // 같은 화면·폭이라도 즉시 다시 요청해야 돌아온 화면에 빈 광고 공간이 남지 않는다.
    // 한 Widget 안의 중복 요청은 InlineBannerAd의 요청 폭·generation guard가 막는다.
    await privacyService.initialize();
    return gateway.load(adUnitId: adUnitId, width: width, maxHeight: maxHeight);
  }

  String? _adUnitIdFor(AppBannerPlacement placement) {
    // debug에서는 운영 ID가 설정돼 있어도 공식 test unit을 먼저 선택해 개발 중
    // 실광고 노출·클릭에 따른 invalid traffic 위험을 차단한다.
    if (useTestAdUnits) {
      return switch (platform) {
        BannerAdPlatform.android => _androidTestAdUnitId,
        BannerAdPlatform.ios => _iosTestAdUnitId,
        BannerAdPlatform.unsupported => null,
      };
    }
    return configuration.adUnitIdFor(platform, placement);
  }

  static BannerAdPlatform _currentPlatform() {
    if (Platform.isAndroid) {
      return BannerAdPlatform.android;
    }
    if (Platform.isIOS) {
      return BannerAdPlatform.ios;
    }
    return BannerAdPlatform.unsupported;
  }
}

/// inline adaptive size로 BannerAd를 load하고 실제 플랫폼 크기를 presentation에 보존한다.
class GoogleBannerAdGateway implements BannerAdGateway {
  @override
  Future<AppBannerAdPresentation> load({
    required String adUnitId,
    required int width,
    required int maxHeight,
  }) async {
    final loaded = Completer<AppBannerAdPresentation>();
    final requestedSize = AdSize.getInlineAdaptiveBannerAdSize(
      width,
      maxHeight,
    );
    late final BannerAd banner;
    banner = BannerAd(
      size: requestedSize,
      adUnitId: adUnitId,
      request: const AdRequest(),
      listener: BannerAdListener(
        onAdLoaded: (ad) async {
          final loadedBanner = ad as BannerAd;
          try {
            final platformSize =
                await loadedBanner.getPlatformAdSize() ?? requestedSize;
            if (!loaded.isCompleted) {
              loaded.complete(
                _GoogleBannerAdPresentation(loadedBanner, platformSize),
              );
            } else {
              loadedBanner.dispose();
            }
          } catch (error, stackTrace) {
            loadedBanner.dispose();
            if (!loaded.isCompleted) {
              loaded.completeError(error, stackTrace);
            }
          }
        },
        onAdFailedToLoad: (ad, error) {
          ad.dispose();
          if (!loaded.isCompleted) {
            loaded.completeError(
              StateError('Unable to load banner ad: ${error.code}'),
            );
          }
        },
      ),
    );

    try {
      await banner.load();
    } catch (_) {
      banner.dispose();
      rethrow;
    }
    // onAdFailedToLoad가 이미 광고를 폐기하므로 callback Future의 오류를 여기서 다시
    // catch해 중복 dispose하지 않는다.
    return loaded.future;
  }
}

class _GoogleBannerAdPresentation implements AppBannerAdPresentation {
  _GoogleBannerAdPresentation(this.banner, this.size);

  final BannerAd banner;
  final AdSize size;

  @override
  double get width => size.width.toDouble();

  @override
  double get height => size.height.toDouble();

  @override
  Widget buildWidget() => AdWidget(ad: banner);

  @override
  void dispose() => banner.dispose();
}
