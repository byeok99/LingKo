// 파일 의도: profile screen 사용자 workflow와 화면 상태를 구성한다.
// 선택 이유: 화면은 상호작용과 표시 상태를 소유하고 네트워크·플랫폼 작업은 주입된 서비스에 위임한다.

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

/// Profile Screen 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.evaluationApi,
    required this.userPreferencesApi,
    required this.authService,
    required this.onRetryPractice,
    required this.onSessionChanged,
  });

  final EvaluationApi evaluationApi;
  final UserPreferencesApi userPreferencesApi;
  final AppAuthService authService;
  final ValueChanged<PracticeSentence> onRetryPractice;
  final ValueChanged<AuthSession?> onSessionChanged;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

/// Profile Screen State Widget의 변경 가능한 화면 상태와 비동기 생명주기를 관리한다.
/// 불변 Widget 설정과 실행 시점 상태를 분리하기 위해 전용 State 객체를 사용한다.
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
      widget.onSessionChanged(restoredSession);
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
      widget.onSessionChanged(nextSession);
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

  /// 오프라인으로 서버 폐기 요청을 완료하지 못해도
  /// profile의 인증 상태는 항상 제거한다.
  Future<void> signOut() async {
    try {
      await widget.authService.signOut();
    } catch (_) {
      // 서버 폐기가 불가능해도 로컬 인증 정보은 이미 삭제됐으므로 화면 상태를 정리한다.
    }

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
    widget.onSessionChanged(null);
  }

  /// 자동 토큰 갱신와 1회 재시도를 통해 연습 기록을 조회한다.
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
      final nextHistory = await widget.authService.runAuthenticated(
        (accessToken) =>
            widget.evaluationApi.fetchHistory(accessToken: accessToken),
      );

      if (!mounted) {
        return;
      }

      setState(() {
        history = nextHistory;
      });
    } on AuthSessionExpiredException {
      _expireSession();
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

  /// 자동 토큰 갱신와 1회 재시도를 통해 사용자 설정을 조회한다.
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
      final nextPreferences = await widget.authService.runAuthenticated(
        (accessToken) => widget.userPreferencesApi.fetchPreferences(
          accessToken: accessToken,
        ),
      );

      if (!mounted) {
        return;
      }

      setState(() {
        preferences = nextPreferences;
      });
    } on AuthSessionExpiredException {
      _expireSession();
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

  /// 동일한 갱신을 인식하는 요청 경계를 통해 사용자 설정을 저장한다.
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
      final savedPreferences = await widget.authService.runAuthenticated(
        (accessToken) => widget.userPreferencesApi.updatePreferences(
          accessToken: accessToken,
          preferences: nextPreferences,
        ),
      );

      if (!mounted) {
        return;
      }

      setState(() {
        preferences = savedPreferences;
      });
    } on AuthSessionExpiredException {
      _expireSession();
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

  /// root 인증 gate에 알리기 전에 profile 소유 데이터를 삭제한다.
  void _expireSession() {
    if (!mounted) {
      return;
    }

    setState(() {
      session = null;
      history = null;
      preferences = null;
      errorText = null;
      preferencesErrorText = null;
    });
    widget.onSessionChanged(null);
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

/// Account Panel 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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

/// Option Sheet 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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

/// History Summary Card 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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

/// History 메시지 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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

/// Practice History Tile 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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

/// Score Badge 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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
