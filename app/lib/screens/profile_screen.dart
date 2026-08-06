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
  });

  final AppAuthService authService;
  final AuthSession session;
  final ValueChanged<AuthSession?> onSessionChanged;
  final VoidCallback onOpenReview;

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
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 22),
      children: [
        TopBar(
          title: 'Profile',
          trailing: IconButton(
            tooltip: 'Open review',
            onPressed: widget.onOpenReview,
            icon: const Icon(Icons.history_rounded, size: 21),
          ),
        ),
        const SizedBox(height: 10),
        _AccountCard(session: widget.session),
        const SizedBox(height: 24),
        SizedBox(
          height: AppSizes.buttonHeight,
          child: OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              foregroundColor: context.palette.error,
              backgroundColor: context.palette.errorSoft,
              side: const BorderSide(color: Color(0xFFEFCACA)),
            ),
            onPressed: isDeletingAccount ? null : signOut,
            icon: Icon(Icons.logout, color: context.palette.error),
            label: Text(
              'Sign out',
              style: TextStyle(color: context.palette.error),
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        TextButton.icon(
          onPressed: isDeletingAccount ? null : deleteAccount,
          icon: Icon(Icons.delete_forever, color: context.palette.error),
          label: Text(
            isDeletingAccount ? 'Deleting account' : 'Delete account',
            style: TextStyle(color: context.palette.error),
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
      padding: const EdgeInsets.all(15),
      child: Row(
        children: [
          Container(
            width: 58,
            height: 58,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: context.palette.softBlue,
              borderRadius: BorderRadius.circular(19),
            ),
            child: Text(
              initial,
              style: TextStyle(
                color: context.palette.primaryDark,
                fontSize: 25,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(user.name, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 2),
                Text(user.email, style: Theme.of(context).textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

