import 'package:flutter/material.dart';

import '../api/evaluation_api.dart';
import '../api/pronunciation_api.dart';
import '../api/sentence_api.dart';
import '../api/user_preferences_api.dart';
import '../models/auth_session.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../screens/auth_gate_screen.dart';
import '../screens/home_screen.dart';
import '../screens/practice_screen.dart';
import '../screens/profile_screen.dart';
import '../screens/result_screen.dart';
import '../services/audio_recorder_service.dart';
import '../services/app_auth_service.dart';
import 'app_theme.dart';

// 앱 전체 설정을 담당하는 최상위 위젯입니다.
// 여기서는 앱 이름, 테마 색상, 기본 글자 스타일, 첫 화면을 정합니다.
class LingKoApp extends StatelessWidget {
  const LingKoApp({
    super.key,
    this.pronunciationApi,
    this.sentenceApi,
    this.evaluationApi,
    this.userPreferencesApi,
    this.authService,
    this.audioRecorderService,
  });

  final PronunciationApi? pronunciationApi;
  final SentenceApi? sentenceApi;
  final EvaluationApi? evaluationApi;
  final UserPreferencesApi? userPreferencesApi;
  final AppAuthService? authService;
  final AudioRecorderService? audioRecorderService;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LingKo',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      // LingKoShell은 하단 탭과 현재 선택된 화면 상태를 관리합니다.
      home: LingKoShell(
        pronunciationApi: pronunciationApi ?? DartIoPronunciationApi(),
        sentenceApi: sentenceApi ?? DartIoSentenceApi(),
        evaluationApi: evaluationApi ?? DartIoEvaluationApi(),
        userPreferencesApi: userPreferencesApi ?? DartIoUserPreferencesApi(),
        authService: authService ?? DefaultAppAuthService(),
        audioRecorderService:
            audioRecorderService ?? RecordAudioRecorderService(),
      ),
    );
  }
}

class LingKoShell extends StatefulWidget {
  const LingKoShell({
    super.key,
    required this.pronunciationApi,
    required this.sentenceApi,
    required this.evaluationApi,
    required this.userPreferencesApi,
    required this.authService,
    required this.audioRecorderService,
  });

  final PronunciationApi pronunciationApi;
  final SentenceApi sentenceApi;
  final EvaluationApi evaluationApi;
  final UserPreferencesApi userPreferencesApi;
  final AppAuthService authService;
  final AudioRecorderService audioRecorderService;

  @override
  State<LingKoShell> createState() => _LingKoShellState();
}

class _LingKoShellState extends State<LingKoShell> {
  // 하단 탭 index입니다. 0: Home, 1: Practice, 2: Profile.
  int selectedTab = 0;

  // 사용자가 홈에서 고르거나 직접 입력한 현재 연습 문장입니다.
  // 아직 아무 문장도 선택하지 않은 Practice 탭 진입 상태는 null입니다.
  PracticeSentence? selectedSentence;
  List<PracticeSentence> recommendedSentences = const [];
  bool isLoadingRecommendedSentences = true;
  String? recommendedSentenceError;
  PracticeResult? latestResult;
  AuthSession? session;
  bool isRestoringSession = true;
  bool isSigningIn = false;
  String? authErrorText;
  // Practice 탭 안에서 연습 화면을 보여줄지, 결과 화면을 보여줄지 결정합니다.
  bool hasResult = false;

  @override
  void initState() {
    super.initState();
    restoreSession();
  }

  @override
  void dispose() {
    widget.audioRecorderService.dispose();
    super.dispose();
  }

  Future<void> loadRecommendedSentences() async {
    setState(() {
      isLoadingRecommendedSentences = true;
      recommendedSentenceError = null;
    });

    try {
      final sentences = await widget.sentenceApi.fetchRecommendedSentences();

      if (!mounted) {
        return;
      }

      setState(() {
        recommendedSentences = sentences;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        recommendedSentences = const [];
        recommendedSentenceError = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoadingRecommendedSentences = false;
        });
      }
    }
  }

  Future<void> restoreSession() async {
    try {
      final restoredSession = await widget.authService.restoreSession();

      if (!mounted) {
        return;
      }

      setState(() {
        session = restoredSession;
        authErrorText = null;
      });
      if (restoredSession != null) {
        await loadRecommendedSentences();
      }
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        session = null;
        authErrorText =
            'Unable to restore your session. Please sign in again.';
      });
    } finally {
      if (mounted) {
        setState(() {
          isRestoringSession = false;
        });
      }
    }
  }

  Future<void> signInWithGoogle() async {
    setState(() {
      isSigningIn = true;
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
      await loadRecommendedSentences();
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        authErrorText = 'Unable to sign in with Google. Please try again.';
      });
    } finally {
      if (mounted) {
        setState(() {
          isSigningIn = false;
        });
      }
    }
  }

  Future<void> handleSessionChanged(AuthSession? nextSession) async {
    if (nextSession == null) {
      setState(() {
        session = null;
        selectedTab = 0;
        selectedSentence = null;
        latestResult = null;
        hasResult = false;
        authErrorText = null;
        recommendedSentences = const [];
        recommendedSentenceError = null;
        isLoadingRecommendedSentences = false;
      });
      return;
    }

    setState(() {
      session = nextSession;
      authErrorText = null;
    });

    await loadRecommendedSentences();
  }

  // 홈에서 문장을 선택하면 Practice 탭으로 이동합니다.
  void openPractice(PracticeSentence sentence) {
    setState(() {
      selectedSentence = sentence;
      selectedTab = 1;
      hasResult = false;
    });
  }

  Future<void> evaluateRecording(
    PracticeSentence sentence,
    String audioPath,
  ) async {
    final result = await widget.evaluationApi.evaluate(
      audioPath: audioPath,
      sentenceId: sentence.source == 'RECOMMENDED' ? sentence.sentenceId : null,
      text: sentence.source == 'RECOMMENDED' ? null : sentence.text,
    );

    setState(() {
      latestResult = result;
      hasResult = true;
    });
  }

  // Practice 탭에서 사용자가 직접 입력한 문장으로 현재 연습 대상을 교체합니다.
  void useCustomSentence(PracticeSentence sentence) {
    setState(() {
      selectedSentence = sentence;
      hasResult = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (isRestoringSession) {
      return const SplashScreen();
    }

    if (session == null) {
      return LoginScreen(
        isLoading: isSigningIn,
        errorText: authErrorText,
        onSignIn: signInWithGoogle,
      );
    }

    // Flutter는 화면도 모두 Widget입니다. 조건에 따라 Practice 탭의 내용을
    // 연습 화면 또는 결과 화면으로 바꿔 끼웁니다.
    final pages = [
      HomeScreen(
        sentences: recommendedSentences,
        isLoading: isLoadingRecommendedSentences,
        errorText: recommendedSentenceError,
        onRetry: loadRecommendedSentences,
        onSelect: openPractice,
      ),
      hasResult && selectedSentence != null
          ? ResultScreen(
            sentence: selectedSentence!,
            result: latestResult,
            onTryAgain: () {
              setState(() {
                hasResult = false;
              });
            },
          )
          : PracticeScreen(
            sentence: selectedSentence,
            audioRecorderService: widget.audioRecorderService,
            onEvaluateRecording: evaluateRecording,
            onCustomSentence: useCustomSentence,
            onPrepareCustomSentence:
                widget.pronunciationApi.prepareCustomSentence,
          ),
      ProfileScreen(
        evaluationApi: widget.evaluationApi,
        userPreferencesApi: widget.userPreferencesApi,
        authService: widget.authService,
        onRetryPractice: openPractice,
        onSessionChanged: handleSessionChanged,
      ),
    ];

    // Scaffold는 일반적인 앱 화면 뼈대입니다.
    // body에는 현재 화면, bottomNavigationBar에는 하단 탭을 둡니다.
    return Scaffold(
      body: SafeArea(child: pages[selectedTab]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: selectedTab,
        backgroundColor: AppColors.background,
        indicatorColor: AppColors.brandSoft,
        onDestinationSelected: (index) {
          setState(() {
            selectedTab = index;
            // 결과 화면은 Practice 탭 안에서만 유지합니다.
            // Home/Profile로 이동했다가 돌아오면 다시 연습 화면부터 시작합니다.
            if (index != 1) {
              hasResult = false;
            }
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.mic_none),
            selectedIcon: Icon(Icons.mic),
            label: 'Practice',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}
