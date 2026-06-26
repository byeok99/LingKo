import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'guide_painter.dart';
import 'shared_widgets.dart';

class GuideSheet extends StatelessWidget {
  const GuideSheet({super.key, required this.result});

  final CharacterResult result;

  @override
  Widget build(BuildContext context) {
    // showModalBottomSheet로 열린 하단 패널입니다.
    // 실제 가이드 이미지/영상이 붙기 전까지는 CustomPainter로 임시 그림을 그립니다.
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
                child: CustomPaint(
                  painter: GuidePainter(result.kind),
                  child: const SizedBox.expand(),
                ),
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
