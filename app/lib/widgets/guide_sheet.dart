// 파일 의도: guide sheet 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'guide_painter.dart';
import 'shared_widgets.dart';

/// Guide Sheet 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class GuideSheet extends StatelessWidget {
  const GuideSheet({super.key, required this.result});

  final CharacterResult result;

  @override
  Widget build(BuildContext context) {
    // 서버가 제공한 실제 입·혀 미디어를 우선하고, URL이 없을 때만 로컬 도식으로 대체한다.
    final guideAssets = _guideAssets(result);
    final hasVideo = guideAssets.any(
      (asset) => _guideMediaType(asset.url) == _GuideMediaType.video,
    );
    final guideMediaMessage =
        hasVideo
            ? 'Guide videos play automatically without sound. Use the control to pause or replay the movement.'
            : guideAssets.isEmpty
            ? 'A built-in static guide is shown because this evaluation did not provide guide media.'
            : 'A static guide image is shown because this evaluation did not provide a video URL.';

    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        12,
        20,
        MediaQuery.of(context).padding.bottom + 20,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                ),
              ),
            ),
            const SizedBox(height: 22),
            Row(
              children: [
                CharacterBadge(text: result.character, large: true),
                const SizedBox(width: 14),
                Expanded(
                  child: Text(
                    _guideTitle(result),
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            AspectRatio(
              aspectRatio: 1.5,
              child: Container(
                decoration: BoxDecoration(
                  color: AppColors.scaffold,
                  borderRadius: BorderRadius.circular(AppSizes.radius),
                  border: Border.all(color: AppColors.border),
                ),
                child:
                    guideAssets.isEmpty
                        ? _FallbackGuide(result: result)
                        : _GuideAssets(assets: guideAssets),
              ),
            ),
            const SizedBox(height: 16),
            Text(result.note, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 18),
            AppCard(
              color: AppColors.softBlue,
              child: Row(
                children: [
                  const Icon(Icons.info_outline, color: AppColors.primary),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(child: Text(guideMediaMessage)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

List<_GuideAsset> _guideAssets(CharacterResult result) {
  final assets = <_GuideAsset>[];

  if (_hasGuideUrl(result.mouthGuideUrl)) {
    assets.add(_GuideAsset(label: 'Mouth guide', url: result.mouthGuideUrl!));
  }

  if (_hasGuideUrl(result.tongueGuideUrl)) {
    assets.add(_GuideAsset(label: 'Tongue guide', url: result.tongueGuideUrl!));
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

/// Static Guide Assets 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _GuideAssets extends StatelessWidget {
  const _GuideAssets({required this.assets});

  final List<_GuideAsset> assets;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          for (final asset in assets) ...[
            Expanded(child: _GuideAssetView(asset: asset)),
            if (asset != assets.last) const SizedBox(width: 12),
          ],
        ],
      ),
    );
  }
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(asset.label, style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 8),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
            child:
                _guideMediaType(asset.url) == _GuideMediaType.video
                    ? _VideoGuideAsset(
                      key: ValueKey(
                        'guide-video-${_guideAssetKey(asset.label)}',
                      ),
                      url: asset.url,
                    )
                    : Image.network(
                      asset.url,
                      fit: BoxFit.contain,
                      errorBuilder:
                          // 외부 이미지 한 건의 실패가 전체 가이드 시트를 깨뜨리지 않도록 해당 영역만 대체한다.
                          (context, error, stackTrace) =>
                              const _UnavailableGuideAsset(),
                    ),
          ),
        ),
      ],
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
  late final VideoPlayerController controller;
  bool isReady = false;
  bool hasError = false;

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
          color: AppColors.card,
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
          child: IconButton.filled(
            tooltip: controller.value.isPlaying ? 'Pause guide' : 'Play guide',
            onPressed: _togglePlayback,
            icon: Icon(
              controller.value.isPlaying ? Icons.pause : Icons.play_arrow,
            ),
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
      color: AppColors.card,
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
      painter: GuidePainter(result.kind),
      child: const SizedBox.expand(),
    );
  }
}
