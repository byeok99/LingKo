import 'package:flutter/material.dart';

import '../api/evaluation_api.dart';
import '../api/user_preferences_api.dart';
import '../app/app_theme.dart';
import '../models/auth_session.dart';
import '../models/practice_history.dart';
import '../models/practice_sentence.dart';
import '../models/user_preferences.dart';
import '../services/app_auth_service.dart';
import '../widgets/settings_row.dart';
import '../widgets/shared_widgets.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.evaluationApi,
    required this.userPreferencesApi,
    required this.authService,
    required this.onRetryPractice,
  });

  final EvaluationApi evaluationApi;
  final UserPreferencesApi userPreferencesApi;
  final AppAuthService authService;
  final ValueChanged<PracticeSentence> onRetryPractice;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  PracticeHistory? history;
  UserPreferences? preferences;
  AuthSession? session;
  bool isLoading = true;
  bool isLoadingPreferences = false;
  bool isAuthenticating = true;
  String? errorText;
  String? preferencesErrorText;
  String? authErrorText;

  @override
  void initState() {
    super.initState();
    restoreSession();
  }

  Future<void> restoreSession() async {
    try {
      final restoredSession = await widget.authService.restoreSession();

      if (!mounted) {
        return;
      }

      setState(() {
        session = restoredSession;
      });
      await Future.wait([
        loadHistory(restoredSession),
        loadPreferences(restoredSession),
      ]);
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        authErrorText = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isAuthenticating = false;
        });
      }
    }
  }

  Future<void> signInWithGoogle() async {
    setState(() {
      isAuthenticating = true;
      authErrorText = null;
    });

    try {
      final nextSession = await widget.authService.signInWithGoogle();

      if (!mounted) {
        return;
      }

      setState(() {
        session = nextSession;
      });
      await Future.wait([
        loadHistory(nextSession),
        loadPreferences(nextSession),
      ]);
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        authErrorText = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isAuthenticating = false;
        });
      }
    }
  }

  Future<void> signOut() async {
    await widget.authService.signOut();

    if (!mounted) {
      return;
    }

    setState(() {
      session = null;
      history = null;
      preferences = null;
      isLoading = false;
      isLoadingPreferences = false;
      authErrorText = null;
      preferencesErrorText = null;
    });
  }

  Future<void> loadHistory([AuthSession? authSession]) async {
    final currentSession = authSession ?? session;
    if (currentSession == null) {
      setState(() {
        history = null;
        isLoading = false;
        errorText = null;
      });
      return;
    }

    setState(() {
      isLoading = true;
      errorText = null;
    });

    try {
      final nextHistory = await widget.evaluationApi.fetchHistory(
        accessToken: currentSession.accessToken,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        history = nextHistory;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        history = null;
        errorText = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
    }
  }

  Future<void> loadPreferences([AuthSession? authSession]) async {
    final currentSession = authSession ?? session;
    if (currentSession == null) {
      setState(() {
        preferences = null;
        isLoadingPreferences = false;
        preferencesErrorText = null;
      });
      return;
    }

    setState(() {
      isLoadingPreferences = true;
      preferencesErrorText = null;
    });

    try {
      final nextPreferences = await widget.userPreferencesApi.fetchPreferences(
        accessToken: currentSession.accessToken,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        preferences = nextPreferences;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        preferencesErrorText = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoadingPreferences = false;
        });
      }
    }
  }

  Future<void> updatePreferences(UserPreferences nextPreferences) async {
    final currentSession = session;
    if (currentSession == null) {
      return;
    }

    setState(() {
      isLoadingPreferences = true;
      preferencesErrorText = null;
    });

    try {
      final savedPreferences = await widget.userPreferencesApi
          .updatePreferences(
            accessToken: currentSession.accessToken,
            preferences: nextPreferences,
          );

      if (!mounted) {
        return;
      }

      setState(() {
        preferences = savedPreferences;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        preferencesErrorText = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoadingPreferences = false;
        });
      }
    }
  }

  Future<void> selectLanguage({required bool isDisplayLanguage}) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      builder: (context) {
        return _OptionSheet<String>(
          title: isDisplayLanguage ? 'Display language' : 'Native language',
          options: _languageOptions,
          labelFor: languageLabel,
        );
      },
    );

    if (selected == null) {
      return;
    }

    final currentPreferences = preferences ?? UserPreferences.defaults;
    await updatePreferences(
      isDisplayLanguage
          ? currentPreferences.copyWith(displayLanguage: selected)
          : currentPreferences.copyWith(nativeLanguage: selected),
    );
  }

  Future<void> selectTargetLevel() async {
    final selected = await showModalBottomSheet<LearningLevel>(
      context: context,
      builder: (context) {
        return _OptionSheet<LearningLevel>(
          title: 'Target level',
          options: LearningLevel.values,
          labelFor: (level) => level.label,
        );
      },
    );

    if (selected == null) {
      return;
    }

    await updatePreferences(
      (preferences ?? UserPreferences.defaults).copyWith(targetLevel: selected),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        const TopBar(title: 'Profile'),
        const SizedBox(height: 24),
        _AccountPanel(
          session: session,
          isLoading: isAuthenticating,
          errorText: authErrorText,
          onSignIn: signInWithGoogle,
          onSignOut: signOut,
        ),
        const SizedBox(height: 24),
        const SectionHeader(title: 'Practice history'),
        const SizedBox(height: 12),
        _HistorySummaryCard(history: history),
        const SizedBox(height: 12),
        if (isLoading)
          const _HistoryMessage(
            icon: Icons.history,
            label: 'Loading practice history',
          )
        else if (errorText != null)
          _HistoryMessage(
            icon: Icons.error_outline,
            label: errorText!,
            action: TextButton.icon(
              onPressed: () => loadHistory(),
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          )
        else if (history == null || history!.items.isEmpty)
          const _HistoryMessage(
            icon: Icons.history_toggle_off,
            label: 'No practice history yet.',
          )
        else
          for (final item in history!.items) ...[
            _PracticeHistoryTile(
              item: item,
              onRetry: () => widget.onRetryPractice(item.toPracticeSentence()),
            ),
            const SizedBox(height: 10),
          ],
        const SizedBox(height: 24),
        Text('Settings', style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 16),
        if (preferencesErrorText != null) ...[
          _HistoryMessage(
            icon: Icons.error_outline,
            label: preferencesErrorText!,
            action: TextButton.icon(
              onPressed: () => loadPreferences(),
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ),
          const SizedBox(height: 8),
        ],
        SettingsRow(
          label: 'Display language',
          value:
              isLoadingPreferences
                  ? 'Loading'
                  : languageLabel(
                    (preferences ?? UserPreferences.defaults).displayLanguage,
                  ),
          onTap:
              session == null
                  ? null
                  : () => selectLanguage(isDisplayLanguage: true),
        ),
        SettingsRow(
          label: 'Native language',
          value:
              isLoadingPreferences
                  ? 'Loading'
                  : languageLabel(
                    (preferences ?? UserPreferences.defaults).nativeLanguage,
                  ),
          onTap:
              session == null
                  ? null
                  : () => selectLanguage(isDisplayLanguage: false),
        ),
        SettingsRow(
          label: 'Target level',
          value:
              isLoadingPreferences
                  ? 'Loading'
                  : (preferences ?? UserPreferences.defaults).targetLevel.label,
          onTap: session == null ? null : selectTargetLevel,
        ),
      ],
    );
  }
}

const _languageOptions = ['en', 'ko', 'ja'];

String languageLabel(String value) {
  return switch (value) {
    'ko' => 'Korean',
    'ja' => 'Japanese',
    _ => 'English',
  };
}

class _AccountPanel extends StatelessWidget {
  const _AccountPanel({
    required this.session,
    required this.isLoading,
    required this.errorText,
    required this.onSignIn,
    required this.onSignOut,
  });

  final AuthSession? session;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onSignIn;
  final VoidCallback onSignOut;

  @override
  Widget build(BuildContext context) {
    final user = session?.user;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.account_circle_outlined, color: AppColors.info),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  user == null ? 'Account' : user.name,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
            ],
          ),
          if (user != null) ...[
            const SizedBox(height: 6),
            Text(user.email, style: Theme.of(context).textTheme.bodyMedium),
          ],
          if (errorText != null) ...[
            const SizedBox(height: 8),
            Text(
              errorText!,
              style: const TextStyle(
                color: Color(0xFFB3261E),
                fontSize: 13,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child:
                user == null
                    ? FilledButton.icon(
                      onPressed: isLoading ? null : onSignIn,
                      icon:
                          isLoading
                              ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                ),
                              )
                              : const Icon(Icons.login),
                      label: const Text('Sign in with Google'),
                    )
                    : OutlinedButton.icon(
                      onPressed: isLoading ? null : onSignOut,
                      icon: const Icon(Icons.logout),
                      label: const Text('Sign out'),
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
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
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
      ),
    );
  }
}

class _HistorySummaryCard extends StatelessWidget {
  const _HistorySummaryCard({required this.history});

  final PracticeHistory? history;

  @override
  Widget build(BuildContext context) {
    final bestScore = history?.bestScore;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          const Icon(Icons.emoji_events_outlined, color: AppColors.info),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Best score',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
          Text(
            bestScore == null ? '-' : '$bestScore',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
        ],
      ),
    );
  }
}

class _HistoryMessage extends StatelessWidget {
  const _HistoryMessage({required this.icon, required this.label, this.action});

  final IconData icon;
  final String label;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          Icon(icon, color: AppColors.textSecondary),
          const SizedBox(height: 8),
          Text(label, style: Theme.of(context).textTheme.bodyMedium),
          if (action != null) ...[const SizedBox(height: 8), action!],
        ],
      ),
    );
  }
}

class _PracticeHistoryTile extends StatelessWidget {
  const _PracticeHistoryTile({required this.item, required this.onRetry});

  final PracticeHistoryItem item;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  item.originalText,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              const SizedBox(width: 12),
              _ScoreBadge(score: item.overallScore),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            item.standardPronunciation,
            style: const TextStyle(
              color: AppColors.info,
              fontSize: 15,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (item.recognizedText.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              'Recognized: ${item.recognizedText}',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: MetaPill(label: item.gradeLabel)),
              TextButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.replay),
                label: const Text('Practice again'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ScoreBadge extends StatelessWidget {
  const _ScoreBadge({required this.score});

  final int score;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: AppColors.brandSoft,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Center(
        child: Text(
          '$score',
          style: const TextStyle(
            color: AppColors.textPrimary,
            fontSize: 18,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
    );
  }
}
