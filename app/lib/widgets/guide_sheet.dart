// 파일 의도: guide sheet 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:video_player/video_player.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_sentence.dart';
import 'guide_painter.dart';
import 'shared_widgets.dart';

/// 조음 가이드를 화면 대부분을 차지하는 bottom sheet로 연다.
///
/// 기본 bottom sheet는 내용 높이에 맞춰 줄어들어 가이드가 작은 상자에 갇힌다. 가이드는
/// 세부를 봐야 하는 콘텐츠이므로 높이를 먼저 확보하고 그 안을 미디어가 채우게 한다.
/// 호출부마다 같은 설정을 반복하지 않도록 진입점을 하나로 모았다.
Future<void> showGuideSheet(BuildContext context, CharacterResult result) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: context.palette.card,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(
        top: Radius.circular(AppSizes.radiusLarge),
      ),
    ),
    builder:
        (_) => FractionallySizedBox(
          heightFactor: 0.88,
          child: GuideSheet(result: result),
        ),
  );
}

/// 한 번에 하나의 가이드만 화면 폭 전체로 보여주는 조음 가이드 시트다.
///
/// 입과 혀를 나란히 놓으면 각 가이드가 화면 폭의 절반도 쓰지 못해, 작은 기기에서는
/// 혀 위치 같은 세부를 분간할 수 없었다. 사용자는 보통 한 번에 하나만 보므로 탭으로
/// 전환하게 하고 남는 폭과 높이를 전부 미디어에 넘긴다.
class GuideSheet extends StatefulWidget {
  const GuideSheet({super.key, required this.result});

  final CharacterResult result;

  @override
  State<GuideSheet> createState() => _GuideSheetState();
}

class _GuideSheetState extends State<GuideSheet> {
  int selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    // 서버가 제공한 실제 입·혀 미디어를 우선하고, URL이 없을 때만 로컬 도식으로 대체한다.
    // 어떤 형식이 왔는지는 시스템 사정이므로 사용자에게 설명하지 않는다.
    final guideAssets = _guideAssets(context, widget.result);
    final safeIndex =
        selectedIndex < guideAssets.length ? selectedIndex : 0;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        12,
        20,
        MediaQuery.of(context).padding.bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 42,
              height: 4,
              decoration: BoxDecoration(
                color: context.palette.border,
                borderRadius: BorderRadius.circular(AppSizes.pillRadius),
              ),
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              CharacterBadge(text: widget.result.character, large: true),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  _guideTitle(widget.result),
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
              ),
            ],
          ),
          // 가이드가 둘 다 있을 때만 전환이 의미를 가진다.
          if (guideAssets.length > 1) ...[
            const SizedBox(height: 14),
            _GuideTabs(
              labels: [for (final asset in guideAssets) asset.label],
              selectedIndex: safeIndex,
              onChanged: (index) => setState(() => selectedIndex = index),
            ),
          ],
          const SizedBox(height: 14),
          // 남은 세로 공간을 미디어가 전부 차지하도록 Expanded로 넘긴다.
          Expanded(
            child: Container(
              width: double.infinity,
              decoration: BoxDecoration(
                color: context.palette.scaffold,
                borderRadius: BorderRadius.circular(AppSizes.radius),
                border: Border.all(color: context.palette.border),
              ),
              clipBehavior: Clip.antiAlias,
              child:
                  guideAssets.isEmpty
                      ? _FallbackGuide(result: widget.result)
                      : _GuideAssetView(
                        key: ValueKey(
                          'guide-asset-'
                          '${_guideAssetKey(guideAssets[safeIndex].label)}',
                        ),
                        asset: guideAssets[safeIndex],
                      ),
            ),
          ),
          // note는 서버가 실제 조음 힌트를 담아 보낼 때만 의미가 있다.
          // 가이드 종류를 되풀이하는 자동 생성 문구는 표시하지 않는다.
          if (_hasMeaningfulNote(widget.result.note)) ...[
            const SizedBox(height: 14),
            Text(
              widget.result.note,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
          ],
        ],
      ),
    );
  }
}

/// 입·혀 가이드를 오가는 세그먼트 전환이다.
class _GuideTabs extends StatelessWidget {
  const _GuideTabs({
    required this.labels,
    required this.selectedIndex,
    required this.onChanged,
  });

  final List<String> labels;
  final int selectedIndex;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: context.palette.softBlue,
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
      ),
      child: Row(
        children: [
          for (var index = 0; index < labels.length; index++)
            Expanded(
              child: Semantics(
                button: true,
                selected: index == selectedIndex,
                child: InkWell(
                  key: ValueKey('guide-tab-${_guideAssetKey(labels[index])}'),
                  borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                  onTap: () => onChanged(index),
                  child: Container(
                    constraints: const BoxConstraints(
                      minHeight: AppSizes.minimumTouchTarget - 8,
                    ),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color:
                          index == selectedIndex
                              ? context.palette.card
                              : Colors.transparent,
                      borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                    ),
                    child: Text(
                      labels[index],
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w800,
                        color:
                            index == selectedIndex
                                ? context.palette.primaryDark
                                : context.palette.textSecondary,
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
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

/// 탭 라벨은 화면에 보이므로 현재 로케일의 문자열이 필요하다.
List<_GuideAsset> _guideAssets(BuildContext context, CharacterResult result) {
  final assets = <_GuideAsset>[];

  if (_hasGuideUrl(result.mouthGuideUrl)) {
    assets.add(_GuideAsset(label: AppL10n.of(context).mouthGuide, url: result.mouthGuideUrl!));
  }

  if (_hasGuideUrl(result.tongueGuideUrl)) {
    assets.add(_GuideAsset(label: AppL10n.of(context).tongueGuide, url: result.tongueGuideUrl!));
  }

  return assets;
}

bool _hasGuideUrl(String? value) {
  return value != null && value.trim().isNotEmpty;
}

String _guideTitle(CharacterResult result) {
  final hasMouthGuide = _hasGuideUrl(result.mouthGuideUrl);
  final hasTongueGuide = _hasGuideUrl(result.tongueGuideUrl);

  if (hasMouthGuide && hasTongueGuide) {
    return 'Mouth & tongue guide';
  }
  if (hasMouthGuide) {
    return 'Mouth guide';
  }
  if (hasTongueGuide) {
    return 'Tongue guide';
  }

  final kind = result.kind.trim();
  return kind.isEmpty || kind.toUpperCase() == 'NONE'
      ? 'Pronunciation guide'
      : '$kind guide';
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
  const _GuideAssetView({super.key, required this.asset});

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
                label: AppL10n.of(context).playbackSpeedLabel(_speedLabel()),
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
        AppL10n.of(context).guideUnavailable,
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
