// 파일 의도: LingKo의 공통 정보 계층, 버튼, 카드, 상태 표현을 제공한다.

import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

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
        border: Border.all(color: context.palette.border),
        boxShadow: [
          BoxShadow(
            color: context.palette.shadow,
            blurRadius: 20,
            offset: Offset(0, 6),
          ),
        ],
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
                color: context.palette.card,
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
    final enabled = onPressed != null && !isLoading;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: enabled ? null : context.palette.border,
        gradient:
            enabled
                ? const LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Color(0xFF4387CA), Color(0xFF286EAE)],
                )
                : null,
        borderRadius: BorderRadius.circular(AppSizes.radiusControl),
        boxShadow:
            enabled
                ? const [
                  BoxShadow(
                    color: Color(0x3B2F73B9),
                    blurRadius: 18,
                    offset: Offset(0, 8),
                  ),
                ]
                : null,
      ),
      child: FilledButton(
        style: FilledButton.styleFrom(
          backgroundColor: Colors.transparent,
          disabledBackgroundColor: Colors.transparent,
          shadowColor: Colors.transparent,
        ),
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
    return SizedBox(
      height: AppSizes.buttonHeight,
      child: OutlinedButton.icon(
        onPressed: onPressed,
        icon: Icon(icon ?? Icons.arrow_forward, size: 19),
        label: Text(label),
      ),
    );
  }
}

/// 활성 상태가 아니면 명확히 비활성화되는 보조 기능 버튼이다.
class ActionButton extends StatelessWidget {
  const ActionButton({
    super.key,
    required this.icon,
    required this.label,
    this.onPressed,
  });

  final IconData icon;
  final String label;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return SecondaryButton(label: label, onPressed: onPressed, icon: icon);
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
      label: AppL10n.of(context).overallScoreOutOfHundred(score),
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
