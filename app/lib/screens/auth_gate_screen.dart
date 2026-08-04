// 파일 의도: auth gate screen 사용자 workflow와 화면 상태를 구성한다.
// 선택 이유: 화면은 상호작용과 표시 상태를 소유하고 네트워크·플랫폼 작업은 주입된 서비스에 위임한다.

import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../widgets/shared_widgets.dart';

/// Splash Screen 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const _AuthFrame(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          _LogoImage(key: Key('splash-logo'), size: 156),
          SizedBox(height: 24),
          CircularProgressIndicator(),
        ],
      ),
    );
  }
}

/// Login Screen 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class LoginScreen extends StatelessWidget {
  const LoginScreen({
    super.key,
    required this.isLoading,
    required this.errorText,
    required this.onSignIn,
  });

  final bool isLoading;
  final String? errorText;
  final VoidCallback onSignIn;

  @override
  Widget build(BuildContext context) {
    return _AuthFrame(
      child: AppCard(
        padding: const EdgeInsets.all(AppSpacing.xxl),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Center(child: _LogoImage(size: 132)),
            const SizedBox(height: AppSpacing.xl),
            Text(
              'LingKo',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineLarge,
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              AppL10n.of(context).buildClearKoreanPronunciationOneSentence,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            if (errorText != null) ...[
              const SizedBox(height: AppSpacing.lg),
              Text(
                errorText!,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Theme.of(context).colorScheme.error,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
            const SizedBox(height: AppSpacing.xxl),
            _GoogleSignInButton(
              isLoading: isLoading,
              onPressed: isLoading ? null : onSignIn,
            ),
          ],
        ),
      ),
    );
  }
}

/// Auth Frame 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class _AuthFrame extends StatelessWidget {
  const _AuthFrame({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: context.palette.softBlue,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(28),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: child,
            ),
          ),
        ),
      ),
    );
  }
}

/// Google Sign In Button 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class _GoogleSignInButton extends StatelessWidget {
  const _GoogleSignInButton({required this.isLoading, required this.onPressed});

  final bool isLoading;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return FilledButton(
      onPressed: onPressed,
      style: FilledButton.styleFrom(
        // Google 로그인 버튼의 중립 색상은 제공자 브랜드 가이드를 따르는 예외다.
        backgroundColor: Colors.white,
        disabledBackgroundColor: Colors.white,
        foregroundColor: AppColors.providerButtonForeground,
        disabledForegroundColor: AppColors.providerButtonDisabled,
        minimumSize: const Size.fromHeight(54),
        padding: const EdgeInsets.symmetric(horizontal: 16),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          side: const BorderSide(color: AppColors.providerButtonBorder),
        ),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          if (isLoading)
            const SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          else
            Image.asset(
              'assets/images/google_g_logo.png',
              key: const Key('google-sign-in-logo'),
              width: 20,
              height: 20,
              fit: BoxFit.contain,
            ),
          const SizedBox(width: 12),
          Text(isLoading ? 'Signing in' : 'Sign in with Google'),
        ],
      ),
    );
  }
}

/// Logo Image 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class _LogoImage extends StatelessWidget {
  const _LogoImage({super.key, required this.size});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/logo.png',
      width: size,
      height: size,
      fit: BoxFit.contain,
    );
  }
}
