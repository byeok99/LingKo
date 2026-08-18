// 파일 의도: auth gate screen 사용자 workflow와 화면 상태를 구성한다.
// 선택 이유: 화면은 상호작용과 표시 상태를 소유하고 네트워크·플랫폼 작업은 주입된 서비스에 위임한다.

import 'package:flutter/material.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart' as apple;

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
    required this.onSignInWithGoogle,
    required this.onSignInWithApple,
    required this.showAppleSignIn,
  });

  final bool isLoading;
  final String? errorText;
  final VoidCallback onSignInWithGoogle;
  final VoidCallback onSignInWithApple;

  /// true일 때만 Apple 버튼을 그린다. 현재 native credential 계약이 있는 iOS에서만 사용한다.
  final bool showAppleSignIn;

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
            onPressed: isLoading ? null : onSignInWithGoogle,
          ),
          if (showAppleSignIn) ...[
            const SizedBox(height: 10),
            _AppleSignInButton(onPressed: isLoading ? null : onSignInWithApple),
          ],
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

/// Apple 공식 logo painter를 공통 provider 버튼 배치에 연결한다.
class _AppleSignInButton extends StatelessWidget {
  const _AppleSignInButton({required this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return _ProviderSignInButton(
      buttonKey: const Key('apple-sign-in-button'),
      iconSlotKey: const Key('apple-sign-in-icon-slot'),
      labelKey: const Key('apple-sign-in-label'),
      label: 'Continue with Apple',
      onPressed: onPressed,
      backgroundColor: Colors.black,
      foregroundColor: Colors.white,
      disabledForegroundColor: Colors.white70,
      borderColor: Colors.black,
      textStyle: const TextStyle(
        fontFamily: '.SF Pro Text',
        fontSize: 15,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.2,
      ),
      icon: const SizedBox(
        key: Key('apple-sign-in-logo'),
        width: 20,
        height: 24,
        child: CustomPaint(
          painter: apple.AppleLogoPainter(color: Colors.white),
        ),
      ),
    );
  }
}

/// 공급자마다 다른 logo 비율을 보존하면서 icon 열과 중앙 label 기준선을 통일한다.
class _ProviderSignInButton extends StatelessWidget {
  const _ProviderSignInButton({
    required this.buttonKey,
    required this.iconSlotKey,
    required this.labelKey,
    required this.label,
    required this.onPressed,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.disabledForegroundColor,
    required this.borderColor,
    required this.textStyle,
    required this.icon,
  });

  final Key buttonKey;
  final Key iconSlotKey;
  final Key labelKey;
  final String label;
  final VoidCallback? onPressed;
  final Color backgroundColor;
  final Color foregroundColor;
  final Color disabledForegroundColor;
  final Color borderColor;
  final TextStyle textStyle;
  final Widget icon;

  @override
  Widget build(BuildContext context) {
    return FilledButton(
      key: buttonKey,
      onPressed: onPressed,
      style: FilledButton.styleFrom(
        backgroundColor: backgroundColor,
        disabledBackgroundColor: backgroundColor,
        foregroundColor: foregroundColor,
        disabledForegroundColor: disabledForegroundColor,
        minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
        padding: EdgeInsets.zero,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          side: BorderSide(color: borderColor),
        ),
        textStyle: textStyle,
      ),
      child: SizedBox(
        width: double.infinity,
        height: AppSizes.buttonHeight,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Positioned(
              left: 20,
              top: 14,
              child: SizedBox.square(
                key: iconSlotKey,
                dimension: 24,
                child: Center(child: icon),
              ),
            ),
            Text(label, key: labelKey, style: textStyle),
          ],
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
    return _ProviderSignInButton(
      buttonKey: const Key('google-sign-in-button'),
      iconSlotKey: const Key('google-sign-in-icon-slot'),
      labelKey: const Key('google-sign-in-label'),
      label: isLoading ? 'Signing in' : 'Continue with Google',
      onPressed: onPressed,
      backgroundColor: Colors.white,
      foregroundColor: AppColors.providerButtonForeground,
      disabledForegroundColor: AppColors.providerButtonDisabled,
      borderColor: AppColors.providerButtonBorder,
      textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
      icon:
          isLoading
              ? const SizedBox.square(
                dimension: 18,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
              : ClipRect(
                child: Transform.scale(
                  // 원본 PNG의 투명 여백이 약 2/3라 실제 G만 24px slot 안에서 확대한다.
                  scale: 2.65,
                  child: Image.asset(
                    'assets/images/google_g_logo.png',
                    key: const Key('google-sign-in-logo'),
                    width: 24,
                    height: 24,
                    fit: BoxFit.contain,
                    filterQuality: FilterQuality.high,
                  ),
                ),
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
