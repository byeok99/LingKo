// 파일 의도: LingKo의 공통 정보 계층, 버튼, 카드, 상태 표현을 제공한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_sentence.dart';

/// 장식용 액션을 만들지 않고 제목과 실제 동작만 노출하는 공통 상단 바다.
class TopBar extends StatelessWidget {
  const TopBar({
    super.key,
    required this.title,
    this.leading,
    this.trailing,
    this.subtitle,
    this.centered = false,
  });

  final String title;
  final Widget? leading;
  final Widget? trailing;
  final String? subtitle;
  final bool centered;

  @override
  Widget build(BuildContext context) {
    final titleBlock = Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment:
          centered ? CrossAxisAlignment.center : CrossAxisAlignment.start,
      children: [
        Text(
          title,
          textAlign: centered ? TextAlign.center : TextAlign.start,
          style: Theme.of(context).textTheme.headlineMedium,
        ),
        if (subtitle != null) ...[
          const SizedBox(height: AppSpacing.xs),
          Text(
            subtitle!,
            textAlign: centered ? TextAlign.center : TextAlign.start,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ],
    );
    return ConstrainedBox(
      constraints: const BoxConstraints(minHeight: 47),
      child:
          centered
              ? Stack(
                alignment: Alignment.topCenter,
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 48),
                    child: titleBlock,
                  ),
                  if (leading != null)
                    Positioned(left: 0, top: 0, child: leading!),
                  if (trailing != null)
                    Positioned(right: 0, top: 0, child: trailing!),
                ],
              )
              : Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (leading != null) ...[
                    leading!,
                    SizedBox(width: AppSpacing.sm),
                  ],
                  Expanded(child: titleBlock),
                  if (trailing != null) trailing!,
                ],
              ),
    );
  }
}

class SectionHeader extends StatelessWidget {
  const SectionHeader({super.key, required this.title, this.trailing});

  final String title;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(title, style: Theme.of(context).textTheme.titleLarge),
        ),
        if (trailing != null) trailing!,
      ],
    );
  }
}

/// 기본 카드의 배경, 테두리와 여백을 한곳에서 유지한다.
class AppCard extends StatelessWidget {
  const AppCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(AppSpacing.lg),
    this.color,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  /// 지정하지 않으면 현재 테마의 카드 배경을 쓴다.
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: color ?? context.palette.card,
        borderRadius: BorderRadius.circular(AppSizes.radius),
        // 그림자를 쓰지 않는다. 면을 띄우는 대신 1px 선으로만 구분해
        // 화면에서 떠 있는 요소가 하나도 없게 한다.
        border: Border.all(color: context.palette.border),
      ),
      child: child,
    );
  }
}

class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.isLoading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    final child =
        isLoading
            ? SizedBox.square(
              dimension: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: context.palette.onPrimary,
              ),
            )
            : Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (icon != null) ...[
                  Icon(icon),
                  const SizedBox(width: AppSpacing.sm),
                ],
                Flexible(child: Text(label, textAlign: TextAlign.center)),
              ],
            );
    // 그라디언트와 그림자를 쓰지 않는다. 위계는 재질이 아니라 채움으로만 말한다.
    // 이전에는 primary만 입체였고 나머지는 평면이라 같은 무게의 버튼끼리 재질이 달랐다.
    return SizedBox(
      height: AppSizes.buttonHeight,
      child: FilledButton(
        onPressed: isLoading ? null : onPressed,
        child: Semantics(
          label: isLoading ? '$label in progress' : label,
          child: child,
        ),
      ),
    );
  }
}

class SecondaryButton extends StatelessWidget {
  const SecondaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final currentIcon = icon;
    return SizedBox(
      height: AppSizes.buttonHeight,
      child:
          currentIcon == null
              // 라벨만 있는 형태가 기본이다. 아이콘을 강제로 붙이면 동등한 선택지
              // 두 개를 나란히 뒀을 때 한쪽이 더 무거워 보인다.
              ? OutlinedButton(onPressed: onPressed, child: Text(label))
              : OutlinedButton.icon(
                onPressed: onPressed,
                icon: Icon(currentIcon, size: 19),
                label: Text(label),
              ),
    );
  }
}

enum StatusTone { info, success, warning, error, neutral }

class StatusBadge extends StatelessWidget {
  const StatusBadge({
    super.key,
    required this.label,
    this.tone = StatusTone.info,
  });

  final String label;
  final StatusTone tone;

  @override
  Widget build(BuildContext context) {
    final color = switch (tone) {
      StatusTone.success => context.palette.success,
      StatusTone.warning => context.palette.warning,
      StatusTone.error => context.palette.error,
      StatusTone.neutral => context.palette.textSecondary,
      StatusTone.info => context.palette.primary,
    };
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: 5,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 10,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }
}

class ScoreRing extends StatelessWidget {
  const ScoreRing({super.key, required this.score, this.size = 118});

  final int score;
  final double size;

  @override
  Widget build(BuildContext context) {
    final normalized = score.clamp(0, 100) / 100;
    return Semantics(
      label: 'Overall score $score out of 100',
      child: SizedBox.square(
        dimension: size,
        child: Stack(
          alignment: Alignment.center,
          children: [
            CircularProgressIndicator(
              value: normalized,
              strokeWidth: 10,
              backgroundColor: context.palette.border,
              color: context.palette.primary,
            ),
            Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '$score',
                      style: Theme.of(context).textTheme.headlineLarge,
                    ),
                    Text(
                      '/100',
                      style: TextStyle(
                        color: context.palette.textSecondary,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class StatePanel extends StatelessWidget {
  const StatePanel({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.actionLabel,
    this.onAction,
    this.isLoading = false,
  });

  final IconData icon;
  final String title;
  final String? message;
  final String? actionLabel;
  final VoidCallback? onAction;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        children: [
          if (isLoading)
            const CircularProgressIndicator()
          else
            Icon(icon, color: context.palette.primary, size: 30),
          const SizedBox(height: AppSpacing.md),
          Text(
            title,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          if (message != null) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(
              message!,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
          if (actionLabel != null && onAction != null) ...[
            const SizedBox(height: AppSpacing.md),
            TextButton(onPressed: onAction, child: Text(actionLabel!)),
          ],
        ],
      ),
    );
  }
}

class CharacterChip extends StatelessWidget {
  const CharacterChip({super.key, required this.result});

  final CharacterResult result;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 38),
      padding: const EdgeInsets.symmetric(
        horizontal: 11,
        vertical: AppSpacing.sm,
      ),
      decoration: BoxDecoration(
        color: context.palette.card,
        border: Border.all(color: context.palette.border),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            result.character,
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w900),
          ),
          if (result.kind.isNotEmpty && result.kind != 'NONE')
            Text(
              result.kind,
              style: TextStyle(
                color: context.palette.textSecondary,
                fontSize: 10,
              ),
            ),
        ],
      ),
    );
  }
}

class CharacterBadge extends StatelessWidget {
  const CharacterBadge({super.key, required this.text, this.large = false});

  final String text;
  final bool large;

  @override
  Widget build(BuildContext context) {
    final size = large ? 58.0 : 46.0;
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: context.palette.softBlue,
        borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: large ? 24 : 19,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }
}

/// 제품명 표기다. 화면마다 크기·자간이 갈리지 않도록 한곳에서 정의한다.
class Wordmark extends StatelessWidget {
  const Wordmark({super.key, this.fontSize = 24});

  final double fontSize;

  @override
  Widget build(BuildContext context) {
    return Text(
      'LingKo',
      style: TextStyle(
        color: context.palette.textPrimary,
        fontSize: fontSize,
        fontWeight: FontWeight.w700,
        letterSpacing: -fontSize * 0.045,
      ),
    );
  }
}

/// 한국어 옆에 붙는 로마자 표기다.
///
/// 대상 사용자가 한글을 아직 읽지 못하므로 모든 한국어에 병기한다. 자간을 넓게 두는 이유는
/// 하이픈으로 이어진 음절 경계를 눈으로 끊어 읽을 수 있게 하기 위해서다.
class RomanizationText extends StatelessWidget {
  const RomanizationText(
    this.text, {
    super.key,
    this.fontSize = 12,
    this.highlight,
  });

  final String text;
  final double fontSize;

  /// 이 부분만 강조한다. 취약 어절이 문장 어디에 있는지 짚어줄 때 쓴다.
  final String? highlight;

  @override
  Widget build(BuildContext context) {
    final base = TextStyle(
      color: context.palette.textSecondary,
      fontSize: fontSize,
      fontWeight: FontWeight.w500,
      letterSpacing: fontSize * 0.12,
      height: 1.4,
    );
    final target = highlight;
    if (target == null || target.isEmpty || !text.contains(target)) {
      return Text(text, style: base);
    }

    final index = text.indexOf(target);
    return Text.rich(
      TextSpan(
        style: base,
        children: [
          TextSpan(text: text.substring(0, index)),
          TextSpan(
            text: target,
            style: TextStyle(
              color: context.palette.error,
              fontWeight: FontWeight.w700,
            ),
          ),
          TextSpan(text: text.substring(index + target.length)),
        ],
      ),
    );
  }
}

/// 섹션을 여는 작은 라벨이다.
///
/// 큰 제목 대신 이 라벨을 쓰는 이유는, 화면에서 가장 크게 읽혀야 할 것이 한국어 문장과
/// 점수이지 섹션 이름이 아니기 때문이다. 자간을 넓히고 대문자로 눌러 구분만 하게 한다.
class EyebrowLabel extends StatelessWidget {
  const EyebrowLabel(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text.toUpperCase(),
      style: TextStyle(
        color: context.palette.textSecondary,
        fontSize: 10.5,
        fontWeight: FontWeight.w600,
        letterSpacing: 1.68,
      ),
    );
  }
}
