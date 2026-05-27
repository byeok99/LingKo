import 'package:flutter/material.dart';

import '../widgets/settings_row.dart';
import '../widgets/shared_widgets.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        const TopBar(title: 'Profile'),
        const SizedBox(height: 24),
        Text('Settings', style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 16),
        const SettingsRow(label: 'Display language', value: 'English'),
        const SettingsRow(label: 'Native language', value: 'English'),
        const SettingsRow(label: 'Target level', value: 'Beginner 2'),
      ],
    );
  }
}
