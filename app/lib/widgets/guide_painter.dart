// 파일 의도: guide painter 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';

/// Guide Painter 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class GuidePainter extends CustomPainter {
  const GuidePainter(this.kind);

  final String kind;

  @override
  void paint(Canvas canvas, Size size) {
    // 임시 조음 가이드 그림입니다.
    // 나중에는 S3 이미지/영상 URL을 받아 Image 또는 Video 위젯으로 교체합니다.
    final outline =
        Paint()
          ..color = AppColors.brandStrong
          ..style = PaintingStyle.stroke
          ..strokeWidth = 5
          ..strokeCap = StrokeCap.round;
    final fill =
        Paint()
          ..color = AppColors.brandSoft
          ..style = PaintingStyle.fill;
    final accent =
        Paint()
          ..color = AppColors.info
          ..style = PaintingStyle.stroke
          ..strokeWidth = 4
          ..strokeCap = StrokeCap.round;

    final mouth =
        Path()
          ..moveTo(size.width * .18, size.height * .5)
          ..quadraticBezierTo(
            size.width * .5,
            size.height * .28,
            size.width * .82,
            size.height * .5,
          )
          ..quadraticBezierTo(
            size.width * .5,
            size.height * .76,
            size.width * .18,
            size.height * .5,
          );

    canvas.drawPath(mouth, fill);
    canvas.drawPath(mouth, outline);

    if (kind.toUpperCase() == 'TONGUE') {
      final tongue =
          Path()
            ..moveTo(size.width * .31, size.height * .62)
            ..quadraticBezierTo(
              size.width * .5,
              size.height * .45,
              size.width * .7,
              size.height * .62,
            );
      canvas.drawPath(tongue, accent);
    } else {
      canvas.drawLine(
        Offset(size.width * .32, size.height * .5),
        Offset(size.width * .68, size.height * .5),
        accent,
      );
    }
  }

  @override
  bool shouldRepaint(covariant GuidePainter oldDelegate) {
    return oldDelegate.kind != kind;
  }
}
