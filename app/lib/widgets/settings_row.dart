import 'package:flutter/material.dart';

import '../app/app_theme.dart';

class SettingsRow extends StatelessWidget {
  const SettingsRow({super.key, required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.border)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: Theme.of(context).textTheme.titleMedium),
          ),
          Text(value, style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(width: 6),
          const Icon(Icons.chevron_right, color: AppColors.brandStrong),
        ],
      ),
    );
  }
}
