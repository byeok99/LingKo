// 파일 의도: result tile 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'guide_sheet.dart';
import 'shared_widgets.dart';

/// Result Tile 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class ResultTile extends StatelessWidget {
  const ResultTile({super.key, required this.result});

  final CharacterResult result;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.background,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        // 글자별 결과를 누르면 입/혀 가이드 바텀시트를 엽니다.
        onTap:
            () => showModalBottomSheet<void>(
              context: context,
              isScrollControlled: true,
              backgroundColor: AppColors.background,
              shape: const RoundedRectangleBorder(
                borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
              ),
              builder: (_) => GuideSheet(result: result),
            ),
        child: Ink(
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.border),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Row(
            children: [
              CharacterBadge(text: result.character),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      result.note,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '${result.kind} guide',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              Text(
                result.scoreStatus == 'AVAILABLE' ? '${result.score}' : '—',
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
