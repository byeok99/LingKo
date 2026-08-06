// 파일 의도: auth gate screen 사용자 workflow와 화면 상태를 구성한다.
// 선택 이유: 화면은 상호작용과 표시 상태를 소유하고 네트워크·플랫폼 작업은 주입된 서비스에 위임한다.

import 'package:flutter/material.dart';

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
      centered: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
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
      // 카드로 감싸지 않는다. 첫 화면은 제품이 무엇인지 말하는 자리라
      // 테두리로 내용을 가두는 대신 여백으로 위계를 만든다.
      //
      // 위아래로 나눠 워드마크와 설명은 상단에 붙이고 로그인 수단은 바닥에 둔다.
      // 전체를 세로 중앙에 모으면 화면 크기마다 시작 위치가 달라져 첫인상이 흔들린다.
      top: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Wordmark(),
          const SizedBox(height: 28),
          Text(
            'Fix your Korean pronunciation, one syllable at a time.',
            style: Theme.of(context).textTheme.headlineLarge?.copyWith(
              fontSize: 28,
              height: 1.3,
              letterSpacing: -0.9,
            ),
          ),
          const SizedBox(height: 18),
          Text(
            'Say a sentence. See which syllables drifted, and hear how they '
            'should sound.',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              fontSize: 14,
              height: 1.75,
              color: context.palette.textSecondary,
            ),
          ),
          const SizedBox(height: 34),
          // 한국어를 못 읽는 사용자에게 이 앱이 다루는 대상을 한 번에 보여준다.
          // 첫 화면의 제품 샘플은 파란 그라디언트 카드로 실제 학습 카드의 재질을 미리 보여준다.
          Container(
            key: const ValueKey('login-pronunciation-sample'),
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [context.palette.softBlue, context.palette.card],
              ),
              border: Border.all(color: context.palette.border),
              borderRadius: BorderRadius.circular(AppSizes.radius),
              boxShadow: [
                BoxShadow(
                  color: context.palette.shadow,
                  blurRadius: 20,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '안녕하세요',
                  style: TextStyle(
                    color: context.palette.textPrimary,
                    fontSize: 40,
                    height: 1.2,
                    fontWeight: FontWeight.w900,
                    letterSpacing: -1.2,
                  ),
                ),
                const SizedBox(height: 8),
                const RomanizationText('an-nyeong-ha-se-yo', fontSize: 12),
              ],
            ),
          ),
        ],
      ),
      bottom: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (errorText != null) ...[
            Text(
              errorText!,
              style: TextStyle(
                color: context.palette.error,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
          ],
          _GoogleSignInButton(
            isLoading: isLoading,
            onPressed: isLoading ? null : onSignIn,
          ),
          const SizedBox(height: 10),
          const _AppleSignInButton(),
          const SizedBox(height: 14),
          Text(
            'Recordings are deleted right after scoring.',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

/// 아직 연결되지 않은 Apple 로그인 자리다.
///
/// iOS에서 소셜 로그인을 제공하면 Apple 로그인도 함께 제공해야 심사를 통과한다.
/// 버튼만 두고 동작을 비워두면 사용자가 눌러도 아무 일이 없어 고장으로 보이므로,
/// 비활성 상태와 준비 중임을 함께 보여준다.
class _AppleSignInButton extends StatelessWidget {
  const _AppleSignInButton();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      enabled: false,
      label: 'Continue with Apple, coming soon',
      child: Container(
        height: AppSizes.buttonHeight,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: context.palette.neutralFill,
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
        ),
        child: Text(
          'Continue with Apple · coming soon',
          style: TextStyle(
            color: context.palette.disabled,
            fontSize: 15,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}

/// Auth Frame 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class _AuthFrame extends StatelessWidget {
  const _AuthFrame({this.top, this.bottom, this.centered});

  /// 화면 위쪽에 붙는 내용이다.
  final Widget? top;

  /// 화면 아래쪽에 붙는 내용이다. 로그인 수단처럼 손이 닿는 자리에 두는 것들이다.
  final Widget? bottom;

  /// 세로 중앙에 모을 내용이다. 시작 화면처럼 읽을 것이 없을 때만 쓴다.
  final Widget? centered;

  @override
  Widget build(BuildContext context) {
    final center = centered;
    return Scaffold(
      backgroundColor: context.palette.scaffold,
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 16, 18, 26),
              child:
                  center != null
                      ? Center(child: center)
                      // 상단 내용은 위에 붙이고 남는 공간을 흡수한다. 로그인 수단은
                      // 그 아래 바닥에 남는다. 전체를 감싸 중앙 정렬하면 화면 크기마다
                      // 시작 위치가 달라져 첫인상이 흔들린다.
                      : Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Expanded(
                            child: SingleChildScrollView(
                              // 큰 글자 설정에서 상단 내용이 길어지면 여기서만 스크롤한다.
                              // 하단 버튼은 항상 같은 자리에 남는다.
                              child: top ?? const SizedBox.shrink(),
                            ),
                          ),
                          if (bottom != null) ...[
                            const SizedBox(height: 24),
                            bottom!,
                          ],
                        ],
                      ),
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
        minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
        padding: const EdgeInsets.symmetric(horizontal: 16),
        elevation: 0,
        shape: RoundedRectangleBorder(
          // 브랜드 색은 예외로 두되 형태(반경 15·높이 52)는 앱 버튼 규칙을 따른다.
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          side: const BorderSide(color: AppColors.providerButtonBorder),
        ),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
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
          Text(isLoading ? 'Signing in' : 'Continue with Google'),
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
