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
            padding: EdgeInsets.only(left: 7, right: isMax ? 10 : 2),
            decoration: BoxDecoration(
              color: context.palette.card,
              borderRadius: BorderRadius.circular(AppSizes.pillRadius),
              border: Border.all(color: context.palette.borderStrong),
              boxShadow: [
                BoxShadow(
                  color: context.palette.shadow,
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 26,
                  height: 26,
                  decoration: BoxDecoration(
                    color: context.palette.softBlue,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.mic_none_rounded,
                    color: context.palette.primaryDark,
                    size: 15,
                  ),
                ),
                const SizedBox(width: 7),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '$safeRemaining/$safeLimit',
                      maxLines: 1,
                      style: TextStyle(
                        color: context.palette.textPrimary,
                        fontSize: 14,
                        height: 1,
                        fontWeight: FontWeight.w900,
                        fontFeatures: const [FontFeature.tabularFigures()],
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      isMax ? 'MAX' : _countdownLabel(),
                      style: TextStyle(
                        color: context.palette.primaryDark,
                        fontSize: 10.5,
                        height: 1,
                        fontWeight: FontWeight.w900,
                        fontFeatures: const [FontFeature.tabularFigures()],
                      ),
                    ),
                  ],
                ),
                if (!isMax)
                  Semantics(
                    button: true,
                    label: 'Add one practice',
                    child: InkWell(
                      key: const ValueKey('practice-energy-ad-button'),
                      onTap: widget.onRequestAdReward,
                      borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                      child: SizedBox.square(
                        // 보이는 원은 compact하게 유지하되 touch target은 44px로 보존한다.
                        dimension: 44,
                        child: Center(
                          child: Container(
                            width: 26,
                            height: 26,
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
