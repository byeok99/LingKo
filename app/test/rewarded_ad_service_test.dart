// 파일 의도: 보상형 광고가 설정·표시·자원 정리 계약을 지키는지 검증한다.
// 보장 대상: ID가 없으면 광고를 요청하지 않고, 플랫폼별 ID를 고르며, 성공·취소와 무관하게 광고를 폐기한다.

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/services/rewarded_ad_service.dart';

void main() {
  group('RewardedAdConfiguration', () {
    const configuration = RewardedAdConfiguration(
      androidAdUnitId: 'android-rewarded-id',
      iosAdUnitId: 'ios-rewarded-id',
    );

    test('별도 빌드 설정이 없어도 iOS 운영 reward 광고가 구성된다', () {
      const environmentConfiguration =
          RewardedAdConfiguration.fromEnvironment();

      expect(
        environmentConfiguration.isConfiguredFor(RewardedAdPlatform.ios),
        isTrue,
      );
    });

    test('플랫폼별 reward ad unit ID를 선택한다', () {
      expect(
        configuration.adUnitIdFor(RewardedAdPlatform.android),
        'android-rewarded-id',
      );
      expect(
        configuration.adUnitIdFor(RewardedAdPlatform.ios),
        'ios-rewarded-id',
      );
      expect(configuration.adUnitIdFor(RewardedAdPlatform.unsupported), isNull);
    });

    test('빈 ID는 설정되지 않은 것으로 처리한다', () {
      const missing = RewardedAdConfiguration(
        androidAdUnitId: '  ',
        iosAdUnitId: '',
      );

      expect(missing.isConfiguredFor(RewardedAdPlatform.android), isFalse);
      expect(missing.isConfiguredFor(RewardedAdPlatform.ios), isFalse);
    });
  });

  group('GooglePracticeRewardAdService', () {
    test('현재 플랫폼 ID로 광고를 불러오고 획득 결과를 반환한 뒤 폐기한다', () async {
      final presentation = FakeRewardedAdPresentation(
        result: RewardedAdResult.earned,
      );
      final gateway = FakeRewardedAdGateway(presentation);
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: 'android-rewarded-id',
          iosAdUnitId: 'ios-rewarded-id',
        ),
        platform: RewardedAdPlatform.ios,
        gateway: gateway,
      );

      final result = await service.show(customData: 'ssv-session-token');

      expect(result, RewardedAdResult.earned);
      expect(gateway.initializationCount, 1);
      expect(gateway.loadedAdUnitIds, ['ios-rewarded-id']);
      expect(gateway.loadedCustomData, ['ssv-session-token']);
      expect(presentation.showCount, 1);
      expect(presentation.disposeCount, 1);
    });

    test('빌드에 설정한 테스트 기기 ID를 SDK 초기화 경계에 전달한다', () async {
      final gateway = FakeRewardedAdGateway(
        FakeRewardedAdPresentation(result: RewardedAdResult.dismissed),
      );
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: 'android-rewarded-id',
          iosAdUnitId: 'ios-rewarded-id',
        ),
        platform: RewardedAdPlatform.ios,
        gateway: gateway,
        testDeviceId: '  ios-test-device-id  ',
      );

      await service.show(customData: 'session-token');

      expect(gateway.initializedTestDeviceIds, ['ios-test-device-id']);
    });

    test('테스트 기기 ID가 비어 있으면 운영 광고 요청 설정을 바꾸지 않는다', () async {
      final gateway = FakeRewardedAdGateway(
        FakeRewardedAdPresentation(result: RewardedAdResult.dismissed),
      );
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: 'android-rewarded-id',
          iosAdUnitId: 'ios-rewarded-id',
        ),
        platform: RewardedAdPlatform.ios,
        gateway: gateway,
        testDeviceId: '   ',
      );

      await service.show(customData: 'session-token');

      expect(gateway.initializedTestDeviceIds, [null]);
    });

    test('두 번째 표시는 SDK를 다시 초기화하지 않는다', () async {
      final gateway = FakeRewardedAdGateway(
        FakeRewardedAdPresentation(result: RewardedAdResult.dismissed),
      );
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: 'android-rewarded-id',
          iosAdUnitId: 'ios-rewarded-id',
        ),
        platform: RewardedAdPlatform.android,
        gateway: gateway,
      );

      await service.show(customData: 'session-1');
      await service.show(customData: 'session-2');

      expect(gateway.initializationCount, 1);
      expect(gateway.loadedAdUnitIds, [
        'android-rewarded-id',
        'android-rewarded-id',
      ]);
    });

    test('초기화가 일시적으로 실패하면 다음 광고 요청에서 다시 시도한다', () async {
      final gateway = FakeRewardedAdGateway(
        FakeRewardedAdPresentation(result: RewardedAdResult.earned),
        initializationFailuresRemaining: 1,
      );
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: 'android-rewarded-id',
          iosAdUnitId: 'ios-rewarded-id',
        ),
        platform: RewardedAdPlatform.android,
        gateway: gateway,
      );

      await expectLater(
        service.show(customData: 'session-1'),
        throwsStateError,
      );
      expect(
        await service.show(customData: 'session-2'),
        RewardedAdResult.earned,
      );
      expect(gateway.initializationCount, 2);
    });

    test('ID가 없으면 SDK를 호출하지 않고 명확한 설정 오류를 반환한다', () async {
      final gateway = FakeRewardedAdGateway(
        FakeRewardedAdPresentation(result: RewardedAdResult.earned),
      );
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: '',
          iosAdUnitId: '',
        ),
        platform: RewardedAdPlatform.ios,
        gateway: gateway,
      );

      expect(service.isConfigured, isFalse);
      await expectLater(
        service.show(customData: 'session-token'),
        throwsA(isA<RewardedAdNotConfigured>()),
      );
      expect(gateway.initializationCount, 0);
      expect(gateway.loadedAdUnitIds, isEmpty);
    });

    test('광고 표시가 실패해도 광고 자원을 폐기한다', () async {
      final presentation = FakeRewardedAdPresentation(
        error: StateError('show failed'),
      );
      final gateway = FakeRewardedAdGateway(presentation);
      final service = GooglePracticeRewardAdService(
        configuration: const RewardedAdConfiguration(
          androidAdUnitId: 'android-rewarded-id',
          iosAdUnitId: 'ios-rewarded-id',
        ),
        platform: RewardedAdPlatform.android,
        gateway: gateway,
      );

      await expectLater(
        service.show(customData: 'session-token'),
        throwsStateError,
      );
      expect(presentation.disposeCount, 1);
    });
  });
}

class FakeRewardedAdGateway implements RewardedAdGateway {
  FakeRewardedAdGateway(
    this.presentation, {
    this.initializationFailuresRemaining = 0,
  });

  final RewardedAdPresentation presentation;
  int initializationFailuresRemaining;
  int initializationCount = 0;
  final List<String?> initializedTestDeviceIds = [];
  final List<String> loadedAdUnitIds = [];
  final List<String> loadedCustomData = [];

  @override
  Future<void> initialize({String? testDeviceId}) async {
    initializationCount++;
    initializedTestDeviceIds.add(testDeviceId);
    if (initializationFailuresRemaining > 0) {
      initializationFailuresRemaining--;
      throw StateError('initialization failed');
    }
  }

  @override
  Future<RewardedAdPresentation> load({
    required String adUnitId,
    required String customData,
  }) async {
    loadedAdUnitIds.add(adUnitId);
    loadedCustomData.add(customData);
    return presentation;
  }

  @override
  Future<void> showPrivacyOptions() async {}
}

class FakeRewardedAdPresentation implements RewardedAdPresentation {
  FakeRewardedAdPresentation({this.result, this.error});

  final RewardedAdResult? result;
  final Object? error;
  int showCount = 0;
  int disposeCount = 0;

  @override
  Future<RewardedAdResult> show() async {
    showCount++;
    final failure = error;
    if (failure != null) {
      throw failure;
    }
    return result!;
  }

  @override
  void dispose() {
    disposeCount++;
  }
}
