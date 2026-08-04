// 파일 의도: 추천 문장과 기록 문장을 동일한 정보 계층으로 표시한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';
import '../models/practice_sentence.dart';

class SentenceCard extends StatelessWidget {
  const SentenceCard({
    super.key,
    required this.sentence,
    required this.onTap,
    this.actionLabel = 'Practice',
    this.showDivider = false,
  });

  final PracticeSentence sentence;
  final VoidCallback onTap;
  final String actionLabel;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: context.palette.card,
      child: InkWell(
        onTap: onTap,
        child: Container(
          constraints: const BoxConstraints(minHeight: 65),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            border:
                showDivider
                    ? Border(bottom: BorderSide(color: context.palette.border))
                    : null,
          ),
          child: Row(
            children: [
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: context.palette.blue50,
                  shape: BoxShape.circle,
                  border: Border.all(color: context.palette.blue200),
                ),
                child: Icon(
                  Icons.play_arrow_rounded,
                  color: context.palette.primaryDark,
                  size: 20,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      sentence.text,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: context.palette.textPrimary,
                        fontSize: 14,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    if (sentence.translation.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        sentence.translation,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: context.palette.textSecondary,
                          fontSize: 11,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: context.palette.textSecondary,
                size: 21,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
