// 파일 의도: sentence card 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'shared_widgets.dart';

/// Sentence Card 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
class SentenceCard extends StatelessWidget {
  const SentenceCard({super.key, required this.sentence, required this.onTap});

  final PracticeSentence sentence;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.background,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Ink(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppColors.border),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      sentence.text,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  const Icon(Icons.chevron_right, color: AppColors.brandStrong),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                sentence.pronunciation,
                style: const TextStyle(
                  color: AppColors.info,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  MetaPill(label: sentence.level),
                  MetaPill(label: sentence.category),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
