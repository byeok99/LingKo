// 파일 의도: 계정 정보와 학습 환경 설정만 관리하고 기록은 Review로 분리한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/auth_session.dart';
import '../services/app_auth_service.dart';
import '../widgets/shared_widgets.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.authService,
    required this.session,
    required this.onSessionChanged,
    required this.onOpenReview,
    this.onOpenSavedSentences,
  });

  final AppAuthService authService;
  final AuthSession session;
  final ValueChanged<AuthSession?> onSessionChanged;
  final VoidCallback onOpenReview;
  final VoidCallback? onOpenSavedSentences;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  bool isDeletingAccount = false;
  String? errorText;

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

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 22),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const TopBar(title: 'Profile'),
          const SizedBox(height: 18),
          _AccountBlock(session: widget.session),
          const SizedBox(height: 20),
          const SectionHeader(title: 'Your content'),
          const SizedBox(height: 10),
          AppCard(
            padding: EdgeInsets.zero,
            child: _SettingsLinkRow(
              key: const ValueKey('profile-saved-sentences'),
              icon: Icons.bookmark_border,
              label: 'Saved sentences',
              onTap: widget.onOpenSavedSentences,
              showDivider: false,
            ),
          ),
          const SizedBox(height: 20),
          const SectionHeader(title: 'About'),
          const SizedBox(height: 10),
          AppCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                _SettingsLinkRow(
                  icon: Icons.mic_none_rounded,
                  label: 'Audio & privacy',
                  onTap: null,
                ),
                _SettingsLinkRow(
                  icon: Icons.info_outline_rounded,
                  label: 'About LingKo 1.0.0',
                  onTap: null,
                  showDivider: false,
                ),
              ],
            ),
          ),
          const SizedBox(height: 28),
          // 로그아웃은 되돌릴 수 있어 선으로만 두고, 탈퇴는 채우지 않는다.
          // 파괴적 동작을 채우면 실수로 누르기 쉬운 무게를 갖게 된다.
          SecondaryButton(
            label: 'Sign out',
            onPressed: isDeletingAccount ? null : signOut,
          ),
          const SizedBox(height: AppSpacing.sm),
          SizedBox(
            height: AppSizes.buttonHeight,
            child: OutlinedButton(
              style: OutlinedButton.styleFrom(
                foregroundColor: context.palette.error,
                side: BorderSide(color: context.palette.errorBorder),
              ),
              onPressed: isDeletingAccount ? null : deleteAccount,
              child: Text(
                isDeletingAccount ? 'Deleting account' : 'Delete account',
                style: TextStyle(color: context.palette.error),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 계정 정보 블록이다. 설정 목록과 같은 카드 재질로 묶어 프로필의 시작점을 만든다.
class _AccountBlock extends StatelessWidget {
  const _AccountBlock({required this.session});

  final AuthSession session;

  @override
  Widget build(BuildContext context) {
    final user = session.user;
    final initial =
        user.name.trim().isEmpty ? '?' : user.name.trim().characters.first;
    return AppCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            width: 58,
            height: 58,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: context.palette.softBlue,
              shape: BoxShape.circle,
            ),
            child: Text(
              initial.toUpperCase(),
              style: TextStyle(
                color: context.palette.primaryDark,
                fontSize: 24,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  user.name,
                  style: TextStyle(
                    color: context.palette.textPrimary,
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                    letterSpacing: -0.36,
                  ),
                ),
                const SizedBox(height: 4),
                Text(user.email, style: Theme.of(context).textTheme.bodyMedium),
                const SizedBox(height: 7),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 9,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: context.palette.softBlue,
                    borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                  ),
                  child: Text(
                    'Google',
                    style: TextStyle(
                      color: context.palette.primaryDark,
                      fontSize: 10.5,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 설정 목록의 한 행이다. 아직 연결되지 않은 항목은 눌리지 않게 흐리게 둔다.
class _SettingsLinkRow extends StatelessWidget {
  const _SettingsLinkRow({
    super.key,
    required this.icon,
    required this.label,
    required this.onTap,
    this.showDivider = true,
  });

  final IconData icon;
  final String label;
  final VoidCallback? onTap;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    final enabled = onTap != null;
    final color =
        enabled ? context.palette.textPrimary : context.palette.textMuted;
    return InkWell(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minHeight: 52),
        decoration: BoxDecoration(
          border:
              showDivider
                  ? Border(
                    bottom: BorderSide(color: context.palette.lineSubtle),
                  )
                  : null,
        ),
        child: Row(
          children: [
            Icon(icon, size: 20, color: color),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  color: color,
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            Icon(
              Icons.chevron_right_rounded,
              size: 18,
              color: context.palette.textMuted,
            ),
          ],
        ),
      ),
    );
  }
}
