// 파일 의도: 스크롤 콘텐츠 안에서 배너 공간과 네이티브 광고 lifecycle을 안전하게 관리한다.
// 선택 이유: 광고 load 지연으로 주변 버튼이 이동하거나 화면마다 dispose 처리가 달라지는 것을 막는다.

import 'dart:async';

import 'package:flutter/material.dart';

import '../services/banner_ad_service.dart';

/// 페이지 좌우 여백을 제외한 폭으로 inline adaptive banner를 한 개 표시한다.
class InlineBannerAd extends StatefulWidget {
  const InlineBannerAd({
    super.key,
    required this.service,
    required this.placement,
    this.maxHeight = 70,
  });

  final AppBannerAdService service;
  final AppBannerPlacement placement;
  final int maxHeight;

  @override
  State<InlineBannerAd> createState() => _InlineBannerAdState();
}

class _InlineBannerAdState extends State<InlineBannerAd> {
  AppBannerAdPresentation? _presentation;
  int? _requestedWidth;
  int _loadGeneration = 0;

  Future<void> _load(int width) async {
    final generation = ++_loadGeneration;
    final previous = _presentation;
    _presentation = null;
    previous?.dispose();
    if (mounted) setState(() {});

    try {
      final loaded = await widget.service.load(
        placement: widget.placement,
        width: width,
        maxHeight: widget.maxHeight,
      );
      if (!mounted || generation != _loadGeneration) {
        loaded?.dispose();
        return;
      }
      setState(() {
        _presentation = loaded;
      });
    } catch (_) {
      // 광고 실패는 핵심 콘텐츠 실패가 아니다. 주변 버튼이 이동하지 않도록 예약 공간은 유지한다.
    }
  }

  @override
  void dispose() {
    _loadGeneration++;
    _presentation?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.service.isConfiguredFor(widget.placement)) {
      return const SizedBox.shrink();
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final availableWidth = constraints.maxWidth.floor();
        if (availableWidth > 0 && availableWidth != _requestedWidth) {
          _requestedWidth = availableWidth;
          // build 중 setState하지 않도록 현재 layout이 끝난 뒤 orientation 대응 load를 시작한다.
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted && _requestedWidth == availableWidth) {
              unawaited(_load(availableWidth));
            }
          });
        }
        final presentation = _presentation;
        return Semantics(
          label: 'Advertisement',
          container: true,
          child: SizedBox(
            key: const ValueKey('banner-reserved-space'),
            height: widget.maxHeight.toDouble(),
            width: double.infinity,
            child:
                presentation == null
                    ? null
                    : Center(
                      child: SizedBox(
                        width: presentation.width,
                        height: presentation.height,
                        child: presentation.buildWidget(),
                      ),
                    ),
          ),
        );
      },
    );
  }
}
