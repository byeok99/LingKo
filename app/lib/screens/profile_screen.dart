// 파일 의도: 계정 정보와 학습 환경 설정만 관리하고 기록은 Review로 분리한다.

import 'package:flutter/material.dart';

import '../api/user_preferences_api.dart';
import '../app/app_theme.dart';
import '../models/auth_session.dart';
import '../models/user_preferences.dart';
import '../services/app_auth_service.dart';
import '../widgets/settings_row.dart';
import '../widgets/shared_widgets.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.userPreferencesApi,
    required this.authService,
    required this.session,
    required this.onSessionChanged,
    required this.onOpenReview,
  });

  final UserPreferencesApi userPreferencesApi;
  final AppAuthService authService;
  final AuthSession session;
  final ValueChanged<AuthSession?> onSessionChanged;
  final VoidCallback onOpenReview;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  UserPreferences? preferences;
  bool isLoading = true;
  bool isSaving = false;
  bool isDeletingAccount = false;
  String? errorText;
  String? savedMessage;

  @override
  void initState() {
    super.initState();
    loadPreferences();
  }

  Future<void> loadPreferences() async {
    setState(() {
      isLoading = true;
      errorText = null;
    });
    try {
      final next = await widget.authService.runAuthenticated(
        (accessToken) => widget.userPreferencesApi.fetchPreferences(
          accessToken: accessToken,
        ),
      );
      if (mounted) {
        setState(() => preferences = next);
      }
    } on AuthSessionExpiredException {
      widget.onSessionChanged(null);
    } catch (_) {
      if (mounted) {
        setState(() {
          errorText =
              'Settings could not be loaded. Check your connection and retry.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isLoading = false);
      }
    }
  }

  Future<void> updatePreferences(UserPreferences next) async {
    setState(() {
      isSaving = true;
      errorText = null;
      savedMessage = null;
    });
    try {
      final saved = await widget.authService.runAuthenticated(
        (accessToken) => widget.userPreferencesApi.updatePreferences(
          accessToken: accessToken,
          preferences: next,
        ),
      );
      if (mounted) {
        setState(() {
          preferences = saved;
          savedMessage = 'Settings saved.';
        });
      }
    } on AuthSessionExpiredException {
      widget.onSessionChanged(null);
    } catch (_) {
      if (mounted) {
        setState(() {
          errorText = 'Settings could not be saved. Please try again.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isSaving = false);
      }
    }
  }

  Future<void> signOut() async {
    try {
      await widget.authService.signOut();
    } finally {
      widget.onSessionChanged(null);
    }
  }

  Future<void> deleteAccount() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('Delete your account?'),
            content: const Text(
              'Your profile, sessions, practice history, quota, and uploaded audio will be deleted.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Delete account'),
              ),
            ],
          ),
    );
    if (confirmed != true || !mounted) {
      return;
    }

    setState(() {
      isDeletingAccount = true;
      errorText = null;
    });
    try {
      await widget.authService.deleteAccount();
      widget.onSessionChanged(null);
    } catch (_) {
      if (mounted) {
        setState(() {
          errorText =
              'Account could not be deleted. Check your connection and try again.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isDeletingAccount = false);
      }
    }
  }

  Future<void> selectLanguage({required bool display}) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      builder:
          (context) => _OptionSheet<String>(
            title: display ? 'Display language' : 'Native language',
            options: const ['en', 'ko', 'ja'],
            labelFor: languageLabel,
          ),
    );
    if (selected == null) {
      return;
    }
    final current = preferences ?? UserPreferences.defaults;
    await updatePreferences(
      display
          ? current.copyWith(displayLanguage: selected)
          : current.copyWith(nativeLanguage: selected),
    );
  }

  Future<void> selectTargetLevel() async {
    final selected = await showModalBottomSheet<LearningLevel>(
      context: context,
      builder:
          (context) => _OptionSheet<LearningLevel>(
            title: 'Target level',
            options: LearningLevel.values,
            labelFor: (level) => level.label,
          ),
    );
    if (selected != null) {
      await updatePreferences(
        (preferences ?? UserPreferences.defaults).copyWith(
          targetLevel: selected,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final current = preferences ?? UserPreferences.defaults;
    return ListView(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.xl,
        AppSpacing.lg,
        AppSpacing.xl,
        AppSpacing.section,
      ),
      children: [
        const TopBar(
          title: 'Profile',
          subtitle: 'Account and learning preferences.',
        ),
        const SizedBox(height: AppSpacing.xxl),
        _AccountCard(session: widget.session),
        const SizedBox(height: AppSpacing.lg),
        SecondaryButton(
          label: 'Open practice review',
          icon: Icons.history,
          onPressed: widget.onOpenReview,
        ),
        const SizedBox(height: AppSpacing.section),
        const SectionHeader(title: 'Learning preferences'),
        const SizedBox(height: AppSpacing.md),
        if (isLoading)
          const StatePanel(
            icon: Icons.settings_outlined,
            title: 'Loading settings',
            isLoading: true,
          )
        else ...[
          if (errorText != null) ...[
            StatePanel(
              icon: Icons.error_outline,
              title: errorText!,
              actionLabel: preferences == null ? 'Retry' : null,
              onAction: preferences == null ? loadPreferences : null,
            ),
            const SizedBox(height: AppSpacing.md),
          ],
          if (savedMessage != null) ...[
            const StatusBadge(
              label: 'Settings saved',
              tone: StatusTone.success,
            ),
            const SizedBox(height: AppSpacing.md),
          ],
          AppCard(
            child: Column(
              children: [
                SettingsRow(
                  label: 'Display language',
                  value: languageLabel(current.displayLanguage),
                  onTap: isSaving ? null : () => selectLanguage(display: true),
                ),
                SettingsRow(
                  label: 'Native language',
                  value: languageLabel(current.nativeLanguage),
                  onTap: isSaving ? null : () => selectLanguage(display: false),
                ),
                SettingsRow(
                  label: 'Target level',
                  value: current.targetLevel.label,
                  onTap: isSaving ? null : selectTargetLevel,
                ),
              ],
            ),
          ),
          if (isSaving) ...[
            const SizedBox(height: AppSpacing.md),
            const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                SizedBox.square(
                  dimension: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
                SizedBox(width: AppSpacing.sm),
                Text('Saving settings'),
              ],
            ),
          ],
        ],
        const SizedBox(height: AppSpacing.section),
        OutlinedButton.icon(
          onPressed: isDeletingAccount ? null : signOut,
          icon: const Icon(Icons.logout, color: AppColors.error),
          label: const Text(
            'Sign out',
            style: TextStyle(color: AppColors.error),
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        TextButton.icon(
          onPressed: isDeletingAccount ? null : deleteAccount,
          icon: const Icon(Icons.delete_forever, color: AppColors.error),
          label: Text(
            isDeletingAccount ? 'Deleting account' : 'Delete account',
            style: const TextStyle(color: AppColors.error),
          ),
        ),
      ],
    );
  }
}

class _AccountCard extends StatelessWidget {
  const _AccountCard({required this.session});

  final AuthSession session;

  @override
  Widget build(BuildContext context) {
    final user = session.user;
    final initial =
        user.name.trim().isEmpty ? '?' : user.name.trim().characters.first;
    return AppCard(
      child: Row(
        children: [
          CircleAvatar(
            radius: 30,
            backgroundColor: AppColors.softBlue,
            child: Text(
              initial,
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ),
          const SizedBox(width: AppSpacing.lg),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(user.name, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: AppSpacing.xs),
                Text(user.email, style: Theme.of(context).textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _OptionSheet<T> extends StatelessWidget {
  const _OptionSheet({
    required this.title,
    required this.options,
    required this.labelFor,
  });

  final String title;
  final List<T> options;
  final String Function(T option) labelFor;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.6,
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.xl),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: AppSpacing.md),
              Flexible(
                child: ListView(
                  shrinkWrap: true,
                  children: [
                    for (final option in options)
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        title: Text(labelFor(option)),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => Navigator.of(context).pop(option),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

String languageLabel(String value) {
  return switch (value) {
    'ko' => 'Korean',
    'ja' => 'Japanese',
    _ => 'English',
  };
}
