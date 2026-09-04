// 파일 의도: settings row 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';

/// Settings Row 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class SettingsRow extends StatelessWidget {
  const SettingsRow({
    super.key,
    required this.label,
    required this.value,
    this.onTap,
  });

  final String label;
  final String value;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minHeight: 55),
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 6),
        decoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: context.palette.border)),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                label,
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            Flexible(
              child: Text(
                value,
                textAlign: TextAlign.end,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
            const SizedBox(width: 6),
            Icon(
              Icons.chevron_right,
              color:
                  onTap == null
                      ? context.palette.textSecondary
                      : context.palette.textPrimary,
              size: 20,
            ),
          ],
        ),
      ),
    );
  }
}
