// 파일 의도: 서버 기준 발음 평가 에너지를 가로형 capsule과 countdown으로 표시한다.

import 'dart:async';

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';

/// 현재·최대 평가 기회와 다음 자연 충전까지의 시간을 compact capsule로 제공한다.
class ProgressPanel extends StatefulWidget {
  const ProgressPanel({
    super.key,
    required this.remaining,
    required this.limit,
    required this.timeUntilNextRefill,
    required this.onRefillDue,
    this.onRequestAdReward,
  });

  final int remaining;
  final int limit;
  final Duration? timeUntilNextRefill;
  final VoidCallback onRefillDue;
  final Future<void> Function()? onRequestAdReward;

  @override
  State<ProgressPanel> createState() => _ProgressPanelState();
}

class _ProgressPanelState extends State<ProgressPanel> {
  Timer? _timer;
  Duration? _remainingTime;
  bool _reportedRefillDue = false;

  @override
  void initState() {
    super.initState();
    _synchronizeCountdown();
  }

  @override
  void didUpdateWidget(covariant ProgressPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.remaining != widget.remaining ||
        oldWidget.limit != widget.limit ||
        oldWidget.timeUntilNextRefill != widget.timeUntilNextRefill) {
      _synchronizeCountdown();
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _synchronizeCountdown() {
    _timer?.cancel();
    _reportedRefillDue = false;
    _remainingTime = widget.timeUntilNextRefill;
    if (widget.remaining >= widget.limit || _remainingTime == null) {
      return;
    }
    if (_remainingTime! <= Duration.zero) {
      scheduleMicrotask(_reportRefillDue);
      return;
    }
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      final current = _remainingTime;
      if (!mounted || current == null) {
        return;
      }
      final next = current - const Duration(seconds: 1);
      setState(() {
        _remainingTime = next.isNegative ? Duration.zero : next;
      });
      if (next <= Duration.zero) {
        _timer?.cancel();
        _reportRefillDue();
      }
    });
  }

  void _reportRefillDue() {
    if (!mounted || _reportedRefillDue) {
      return;
    }
    _reportedRefillDue = true;
    widget.onRefillDue();
  }

  @override
  Widget build(BuildContext context) {
    final safeLimit = widget.limit <= 0 ? 1 : widget.limit;
    final safeRemaining = widget.remaining.clamp(0, safeLimit);
    final isMax = safeRemaining >= safeLimit;

    return Align(
      key: const ValueKey('practice-energy-alignment-frame'),
      alignment: Alignment.centerRight,
      // 좁은 화면이나 큰 글자 설정에서 캡슐이 헤더를 넘치는 대신 통째로 줄어들게 한다.
      // 내용을 숨기면 남은 횟수나 충전 시각 중 하나를 잃지만, 축소는 둘 다 보존한다.
      child: FittedBox(
        fit: BoxFit.scaleDown,
        alignment: Alignment.centerRight,
        child: Semantics(
        label:
            isMax
                ? 'Practice energy $safeRemaining of $safeLimit, maximum'
                : 'Practice energy $safeRemaining of $safeLimit, ${_countdownLabel()} until refill',
        container: true,
        child: Container(
          key: const ValueKey('practice-energy-capsule'),
          constraints: const BoxConstraints(minHeight: 40),
          padding: const EdgeInsets.only(left: 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(AppSizes.pillRadius),
            // 카드처럼 채우지 않고 선으로만 묶는다. 헤더에서 워드마크와 무게가 같아야 한다.
            border: Border.all(color: context.palette.borderStrong),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.mic_none_rounded,
                color: context.palette.textPrimary,
                size: 16,
              ),
              const SizedBox(width: 7),
              Text(
                '$safeRemaining/$safeLimit',
                maxLines: 1,
                style: TextStyle(
                  color: context.palette.textPrimary,
                  fontSize: 14,
                  height: 1,
                  fontWeight: FontWeight.w700,
                ),
              ),
              if (!isMax) ...[
                // 남은 수량과 다음 충전 시각은 성격이 달라 세로선으로 끊는다.
                Container(
                  width: 1,
                  height: 14,
                  margin: const EdgeInsets.symmetric(horizontal: 9),
                  color: context.palette.borderStrong,
                ),
                Text(
                  _countdownLabel(),
                  style: TextStyle(
                    color: context.palette.textSecondary,
                    fontSize: 12.5,
                    height: 1,
                    fontWeight: FontWeight.w500,
                    fontFeatures: const [FontFeature.tabularFigures()],
                  ),
                ),
                Semantics(
                  button: true,
                  label: 'Add one practice',
                  child: InkWell(
                    key: const ValueKey('practice-energy-ad-button'),
                    onTap: widget.onRequestAdReward,
                    borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                    // 시각 요소는 28px이지만 히트 영역은 44px를 유지한다.
                    // 캡슐이 헤더에 들어가야 해서 48px 기본값 대신 44px를 쓴다.
                    child: SizedBox.square(
                      dimension: 44,
                      child: Center(
                        child: Container(
                          width: 28,
                          height: 28,
                          decoration: BoxDecoration(
                            color: context.palette.softBlue,
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            Icons.add_rounded,
                            color:
                                widget.onRequestAdReward == null
                                    ? context.palette.textMuted
                                    : context.palette.primaryDark,
                            size: 17,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ] else
                // 최대치에서는 오른쪽 여백을 왼쪽 패딩과 맞춰 캡슐이 한쪽으로 쏠리지 않게 한다.
                const SizedBox(width: 10),
            ],
          ),
          ),
        ),
      ),
    );
  }

  String _countdownLabel() {
    final remaining = _remainingTime;
    if (remaining == null) {
      return '--:--';
    }
    final totalSeconds = remaining.inSeconds.clamp(0, 359999);
    final minutes = totalSeconds ~/ 60;
    final seconds = totalSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }
}
