import 'package:flutter/material.dart';

import '../api/evaluation_api.dart';
import '../app/app_theme.dart';
import '../models/auth_session.dart';
import '../models/practice_history.dart';
import '../models/practice_sentence.dart';
import '../services/app_auth_service.dart';
import '../widgets/settings_row.dart';
import '../widgets/shared_widgets.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.evaluationApi,
    required this.authService,
    required this.onRetryPractice,
  });

  final EvaluationApi evaluationApi;
  final AppAuthService authService;
  final ValueChanged<PracticeSentence> onRetryPractice;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  PracticeHistory? history;
  AuthSession? session;
  bool isLoading = true;
  bool isAuthenticating = true;
  String? errorText;
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
      await loadHistory(restoredSession);
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
      await loadHistory(nextSession);
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
      isLoading = false;
      authErrorText = null;
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
        const SettingsRow(label: 'Display language', value: 'English'),
        const SettingsRow(label: 'Native language', value: 'English'),
        const SettingsRow(label: 'Target level', value: 'Beginner 2'),
      ],
    );
  }
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
