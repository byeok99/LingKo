// 파일 의도: guide sheet 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'package:flutter/material.dart';

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
    // 서버가 제공한 실제 입·혀 가이드를 우선하고, URL이 없을 때만 로컬 도식으로 대체한다.
    final guideAssets = _staticGuideAssets(result);

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
                  borderRadius: BorderRadius.circular(99),
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
                    '${result.kind} guide',
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
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppColors.border),
                ),
                child:
                    guideAssets.isEmpty
                        ? _FallbackGuide(result: result)
                        : _StaticGuideAssets(assets: guideAssets),
              ),
            ),
            const SizedBox(height: 16),
            Text(result.note, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: ActionButton(
                    icon: Icons.play_arrow,
                    label: 'Play',
                    onPressed: () {},
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ActionButton(
                    icon: Icons.mic,
                    label: 'Repeat',
                    onPressed: () {},
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

List<_GuideAsset> _staticGuideAssets(CharacterResult result) {
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

/// Guide Asset 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _GuideAsset {
  const _GuideAsset({required this.label, required this.url});

  final String label;
  final String url;
}

/// Static Guide Assets 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _StaticGuideAssets extends StatelessWidget {
  const _StaticGuideAssets({required this.assets});

  final List<_GuideAsset> assets;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          for (final asset in assets) ...[
            Expanded(child: _StaticGuideAsset(asset: asset)),
            if (asset != assets.last) const SizedBox(width: 12),
          ],
        ],
      ),
    );
  }
}

/// Static Guide Asset 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _StaticGuideAsset extends StatelessWidget {
  const _StaticGuideAsset({required this.asset});

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
            borderRadius: BorderRadius.circular(8),
            child: Image.network(
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

/// Unavailable Guide Asset 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class _UnavailableGuideAsset extends StatelessWidget {
  const _UnavailableGuideAsset();

  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: Alignment.center,
      color: AppColors.background,
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
