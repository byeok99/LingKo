// 파일 의도: guide sheet 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_sentence.dart';
import 'guide_painter.dart';

/// 조음 가이드를 화면 하단에 붙는 bottom sheet로 연다.
///
/// 높이를 비율로 고정하지 않고 내용 높이만큼만 차지하게 둔다. 0.9처럼 비율을 강제하면
/// 도해가 시트 위쪽에 붙고 아래에 빈 공간이 남아, 손이 닿는 자리가 아무것도 없는 여백이
/// 된다. 내용에 맞추면 시트가 화면 바닥에서 올라온 만큼만 열린다.
///
/// 상한만 화면의 90%로 둔다. 작은 화면이나 큰 글자 설정에서 도해 두 장이 넘칠 때
/// 잘라내는 대신 시트 안에서 스크롤한다. 두 단면 중 하나라도 못 보면 가이드가 성립하지 않는다.
/// 호출부마다 같은 설정을 반복하지 않도록 진입점을 하나로 모았다.
Future<void> showGuideSheet(BuildContext context, CharacterResult result) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: context.palette.card,
    constraints: BoxConstraints(
      maxHeight: MediaQuery.of(context).size.height * 0.9,
    ),
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(
        top: Radius.circular(AppSizes.radiusLarge),
      ),
    ),
    builder: (_) => SingleChildScrollView(child: GuideSheet(result: result)),
  );
}

/// 음절 하나의 입·혀 도해를 세로로 쌓아 함께 보여주는 시트다.
///
/// 탭으로 나누지 않는 이유는 입 모양과 혀 위치가 같은 소리를 설명하는 두 단면이기
/// 때문이다. 번갈아 보면 둘의 관계를 머릿속에서 맞춰야 하지만, 위아래로 쌓으면
/// 눈만 옮기면 된다. 가로 2열은 각각이 너무 작아 따라 할 수 없다.
class GuideSheet extends StatelessWidget {
  const GuideSheet({super.key, required this.result});

  final CharacterResult result;

  /// 도해 한 장의 높이다. 세로로 쌓아도 두 장이 한 화면에 들어오는 크기로 잡았다.
  static const _mediaHeight = 214.0;

  @override
  Widget build(BuildContext context) {
    // 서버가 제공한 실제 입·혀 미디어를 우선하고, URL이 없을 때만 로컬 도식으로 대체한다.
    // 어떤 형식이 왔는지는 시스템 사정이므로 사용자에게 설명하지 않는다.
    final guideAssets = _guideAssets(result);

    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        10,
        20,
        MediaQuery.of(context).padding.bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: context.palette.borderStrong,
                borderRadius: BorderRadius.circular(AppSizes.pillRadius),
              ),
            ),
          ),
          const SizedBox(height: 16),
          // 음절과 로마자를 나란히 두고 아래 선으로 도해와 끊는다.
          // 자모 분해는 표시하지 않는다. 읽는 법이 아니라 내는 법을 보러 온 화면이다.
          Container(
            padding: const EdgeInsets.only(bottom: 16),
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: context.palette.line)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  result.character,
                  style: TextStyle(
                    color: context.palette.textPrimary,
                    fontSize: 44,
                    height: 1,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -1.3,
                  ),
                ),
                const SizedBox(width: 14),
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Text(
                    result.romanization,
                    style: TextStyle(
                      color: context.palette.textSecondary,
                      fontSize: 24,
                      height: 1,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 2.4,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          if (guideAssets.isEmpty)
            SizedBox(
              height: _mediaHeight,
              child: _GuideMediaFrame(child: _FallbackGuide(result: result)),
            )
          else
            for (var index = 0; index < guideAssets.length; index++) ...[
              if (index > 0) const SizedBox(height: 10),
              _LabeledGuide(
                key: ValueKey(
                  'guide-asset-${_guideAssetKey(guideAssets[index].label)}',
                ),
                asset: guideAssets[index],
                height: _mediaHeight,
              ),
            ],
          // note는 서버가 실제 조음 힌트를 담아 보낼 때만 의미가 있다.
          // 가이드 종류를 되풀이하는 자동 생성 문구는 표시하지 않는다.
          if (_hasMeaningfulNote(result.note)) ...[
            const SizedBox(height: 14),
            Text(result.note, style: Theme.of(context).textTheme.bodyLarge),
          ],
        ],
      ),
    );
  }
}

/// 도해 한 장과 그 단면 이름이다.
class _LabeledGuide extends StatelessWidget {
  const _LabeledGuide({
    super.key,
    required this.asset,
    required this.height,
  });

  final _GuideAsset asset;
  final double height;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: _GuideMediaFrame(
        child: Stack(
          fit: StackFit.expand,
          children: [
            _GuideAssetView(asset: asset),
            // 어느 단면인지 도해 위에 얹는다. 위아래로 쌓여 있어 라벨이 없으면
            // 어느 쪽이 입이고 혀인지 그림만으로 판단해야 한다.
            Positioned(
              left: 10,
              top: 10,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: context.palette.card.withValues(alpha: 0.88),
                  borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
                ),
                child: Text(
                  asset.label,
                  style: TextStyle(
                    color: context.palette.textSecondary,
                    fontSize: 10,
                    fontWeight: FontWeight.w500,
                    letterSpacing: 1.2,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 도해를 담는 공통 틀이다. 반경과 배경을 한곳에서 정한다.
class _GuideMediaFrame extends StatelessWidget {
  const _GuideMediaFrame({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: context.palette.neutralFill,
        borderRadius: BorderRadius.circular(AppSizes.radiusControl),
      ),
      clipBehavior: Clip.antiAlias,
      child: child,
    );
  }
}

/// 서버가 보낸 note가 가이드 종류를 되풀이하는 자동 생성 문구인지 판별한다.
///
/// `Focus on mouth placement`처럼 guideType만 갈아끼운 문장은 이미 제목과 탭이 전달하는
/// 정보라 화면 공간만 차지한다. 실제 조음 힌트가 들어오기 시작하면 그대로 표시된다.
bool _hasMeaningfulNote(String note) {
  final trimmed = note.trim();
  if (trimmed.isEmpty) {
    return false;
  }
  return !RegExp(
    r'^focus on \w+ placement\.?$',
    caseSensitive: false,
  ).hasMatch(trimmed);
}

List<_GuideAsset> _guideAssets(CharacterResult result) {
  final assets = <_GuideAsset>[];

  if (_hasGuideUrl(result.mouthGuideUrl)) {
    assets.add(_GuideAsset(label: 'LIPS · FRONT VIEW', url: result.mouthGuideUrl!));
  }

  if (_hasGuideUrl(result.tongueGuideUrl)) {
    assets.add(_GuideAsset(label: 'TONGUE · SIDE VIEW', url: result.tongueGuideUrl!));
  }

  return assets;
}

bool _hasGuideUrl(String? value) {
  return value != null && value.trim().isNotEmpty;
}

/// Guide Asset 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _GuideAsset {
  const _GuideAsset({required this.label, required this.url});

  final String label;
  final String url;
}

enum _GuideMediaType { image, video }

_GuideMediaType _guideMediaType(String url) {
  final path = Uri.tryParse(url)?.path.toLowerCase() ?? url.toLowerCase();
  const videoExtensions = ['.mp4', '.m4v', '.mov', '.webm'];
  return videoExtensions.any(path.endsWith)
      ? _GuideMediaType.video
      : _GuideMediaType.image;
}

String _guideAssetKey(String label) {
  return label.toLowerCase().replaceAll(RegExp(r'[^a-z0-9]+'), '-');
}

/// Guide Asset URL의 미디어 형식에 맞는 렌더러를 선택한다.
class _GuideAssetView extends StatelessWidget {
  const _GuideAssetView({required this.asset});

  final _GuideAsset asset;

  @override
  Widget build(BuildContext context) {
    if (_guideMediaType(asset.url) == _GuideMediaType.video) {
      return _VideoGuideAsset(
        key: ValueKey('guide-video-${_guideAssetKey(asset.label)}'),
        url: asset.url,
      );
    }

    // 정지 이미지는 확대해야 조음 위치를 확인할 수 있는 경우가 많아 핀치 줌을 허용한다.
    return InteractiveViewer(
      maxScale: 4,
      child: Image.network(
        asset.url,
        fit: BoxFit.contain,
        width: double.infinity,
        height: double.infinity,
        errorBuilder:
            // 외부 이미지 한 건의 실패가 전체 가이드 시트를 깨뜨리지 않도록 해당 영역만 대체한다.
            (context, error, stackTrace) => const _UnavailableGuideAsset(),
      ),
    );
  }
}

/// 서버가 제공한 사전 생성 가이드 영상을 무음·반복 재생한다.
class _VideoGuideAsset extends StatefulWidget {
  const _VideoGuideAsset({super.key, required this.url});

  final String url;

  @override
  State<_VideoGuideAsset> createState() => _VideoGuideAssetState();
}

class _VideoGuideAssetState extends State<_VideoGuideAsset> {
  /// 조음 동작은 실시간 속도로는 따라가기 어려워 느린 배속을 선택지로 제공한다.
  static const playbackSpeeds = [1.0, 0.5, 0.25];

  late final VideoPlayerController controller;
  bool isReady = false;
  bool hasError = false;
  int speedIndex = 0;

  @override
  void initState() {
    super.initState();
    controller = VideoPlayerController.networkUrl(
      Uri.parse(widget.url),
      videoPlayerOptions: VideoPlayerOptions(mixWithOthers: true),
    );
    unawaited(_initialize());
  }

  Future<void> _initialize() async {
    try {
      await controller.initialize();
      await controller.setLooping(true);
      await controller.setVolume(0);
      await controller.play();
      if (mounted) {
        setState(() => isReady = true);
      }
    } catch (_) {
      if (mounted) {
        setState(() => hasError = true);
      }
    }
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  Future<void> _togglePlayback() async {
    if (!isReady) {
      return;
    }
    if (controller.value.isPlaying) {
      await controller.pause();
    } else {
      await controller.play();
    }
    if (mounted) {
      setState(() {});
    }
  }

  /// 1x → 0.5x → 0.25x 순으로 순환해 버튼 하나로 배속을 오갈 수 있게 한다.
  Future<void> _cycleSpeed() async {
    if (!isReady) {
      return;
    }
    final nextIndex = (speedIndex + 1) % playbackSpeeds.length;
    await controller.setPlaybackSpeed(playbackSpeeds[nextIndex]);
    if (mounted) {
      setState(() => speedIndex = nextIndex);
    }
  }

  String _speedLabel() {
    final speed = playbackSpeeds[speedIndex];
    return speed == 1.0 ? '1x' : '${speed}x';
  }

  @override
  Widget build(BuildContext context) {
    if (hasError) {
      return const _UnavailableGuideAsset();
    }
    if (!isReady) {
      return const Center(child: CircularProgressIndicator(strokeWidth: 2));
    }

    final aspectRatio =
        controller.value.aspectRatio > 0 ? controller.value.aspectRatio : 1.0;
    return Stack(
      fit: StackFit.expand,
      children: [
        ColoredBox(
          color: context.palette.card,
          child: Center(
            child: AspectRatio(
              aspectRatio: aspectRatio,
              child: VideoPlayer(controller),
            ),
          ),
        ),
        Positioned(
          right: 8,
          bottom: 8,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Semantics(
                button: true,
                label: 'Playback speed ${_speedLabel()}',
                child: Material(
                  color: context.palette.primary,
                  shape: const StadiumBorder(),
                  child: InkWell(
                    key: const ValueKey('guide-video-speed'),
                    customBorder: const StadiumBorder(),
                    onTap: _cycleSpeed,
                    child: Container(
                      constraints: const BoxConstraints(
                        minWidth: AppSizes.minimumTouchTarget,
                        minHeight: AppSizes.minimumTouchTarget,
                      ),
                      alignment: Alignment.center,
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      child: Text(
                        _speedLabel(),
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              IconButton.filled(
                tooltip:
                    controller.value.isPlaying ? 'Pause guide' : 'Play guide',
                onPressed: _togglePlayback,
                icon: Icon(
                  controller.value.isPlaying ? Icons.pause : Icons.play_arrow,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Unavailable Guide Asset 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _UnavailableGuideAsset extends StatelessWidget {
  const _UnavailableGuideAsset();

  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: Alignment.center,
      color: context.palette.card,
      child: Text(
        'Guide unavailable',
        style: Theme.of(context).textTheme.bodyMedium,
      ),
    );
  }
}

/// 대체 Guide 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _FallbackGuide extends StatelessWidget {
  const _FallbackGuide({required this.result});

  final CharacterResult result;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: GuidePainter(result.kind, context.palette),
      child: const SizedBox.expand(),
    );
  }
}
