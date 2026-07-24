// 파일 의도: lingko app 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'package:flutter/material.dart';

import '../api/evaluation_api.dart';
import '../api/practice_quota_api.dart';
import '../api/pronunciation_api.dart';
import '../api/sentence_api.dart';
import '../api/user_preferences_api.dart';
import '../models/auth_session.dart';
import '../models/practice_quota.dart';
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
/// Ling Ko App 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class LingKoApp extends StatelessWidget {
  const LingKoApp({
    super.key,
    this.pronunciationApi,
    this.sentenceApi,
    this.evaluationApi,
    this.userPreferencesApi,
    this.practiceQuotaApi,
    this.authService,
    this.audioRecorderService,
  });

  final PronunciationApi? pronunciationApi;
  final SentenceApi? sentenceApi;
  final EvaluationApi? evaluationApi;
  final UserPreferencesApi? userPreferencesApi;
  final PracticeQuotaApi? practiceQuotaApi;
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
        practiceQuotaApi: practiceQuotaApi ?? DartIoPracticeQuotaApi(),
        authService: authService ?? DefaultAppAuthService(),
        audioRecorderService:
            audioRecorderService ?? RecordAudioRecorderService(),
      ),
    );
  }
}

/// Ling Ko Shell 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class LingKoShell extends StatefulWidget {
  const LingKoShell({
    super.key,
    required this.pronunciationApi,
    required this.sentenceApi,
    required this.evaluationApi,
    required this.userPreferencesApi,
    required this.practiceQuotaApi,
    required this.authService,
    required this.audioRecorderService,
  });

  final PronunciationApi pronunciationApi;
  final SentenceApi sentenceApi;
  final EvaluationApi evaluationApi;
  final UserPreferencesApi userPreferencesApi;
  final PracticeQuotaApi practiceQuotaApi;
  final AppAuthService authService;
  final AudioRecorderService audioRecorderService;

  @override
  State<LingKoShell> createState() => _LingKoShellState();
}

/// Ling Ko Shell State Widget의 변경 가능한 화면 상태와 비동기 생명주기를 관리한다.
/// 불변 Widget 설정과 실행 시점 상태를 분리하기 위해 전용 State 객체를 사용한다.
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
  PracticeQuota? practiceQuota;
  bool isLoadingPracticeQuota = false;
  String? practiceQuotaError;

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
    // 보안 저장소 확인이 끝날 때까지 시작 화면을 유지해 로그인 화면과 홈 화면의 순간 전환을 막는다.
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
        // 문장과 할당량는 서로 의존하지 않으므로 동시에 조회해 인증 후 대기 시간을 줄인다.
        await Future.wait([
          loadRecommendedSentences(),
          loadPracticeQuota(restoredSession),
        ]);
      }
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        session = null;
        authErrorText = 'Unable to restore your session. Please sign in again.';
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
      await Future.wait([
        loadRecommendedSentences(),
        loadPracticeQuota(nextSession),
      ]);
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
      // 다른 사용자의 문장·평가·할당량가 다음 로그인에 노출되지 않도록 사용자 종속 상태를 함께 비운다.
      setState(() {
        session = null;
        selectedTab = 0;
        selectedSentence = null;
        latestResult = null;
        hasResult = false;
        authErrorText = null;
        practiceQuota = null;
        practiceQuotaError = null;
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

    await Future.wait([
      loadRecommendedSentences(),
      loadPracticeQuota(nextSession),
    ]);
  }

  /// 갱신을 인식하는 인증 경계를 통해 할당량을 조회한다.
  ///
  /// 복구할 수 없는 세션 실패는 전체 shell을 로그인 gate 상태로 되돌린다.
  Future<void> loadPracticeQuota([AuthSession? authSession]) async {
    final currentSession = authSession ?? session;
    if (currentSession == null) {
      setState(() {
        practiceQuota = null;
        isLoadingPracticeQuota = false;
        practiceQuotaError = null;
      });
      return;
    }

    setState(() {
      isLoadingPracticeQuota = true;
      practiceQuotaError = null;
    });

    try {
      final nextQuota = await widget.authService.runAuthenticated(
        (accessToken) =>
            widget.practiceQuotaApi.fetchTodayQuota(accessToken: accessToken),
      );

      if (!mounted) {
        return;
      }

      setState(() {
        practiceQuota = nextQuota;
      });
    } on AuthSessionExpiredException {
      if (mounted) {
        await handleSessionChanged(null);
      }
    } catch (_) {
      if (!mounted) {
        return;
      }

      setState(() {
        practiceQuota = null;
        practiceQuotaError = 'Unable to load practice quota. Please try again.';
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoadingPracticeQuota = false;
        });
      }
    }
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
    // 추천 문장은 서버 ID로, 사용자 입력 문장은 원문으로 보내 두 입력을 동시에 허용하지 않는 API 계약을 지킨다.
    final result = await widget.evaluationApi.evaluate(
      audioPath: audioPath,
      sentenceId: sentence.source == 'RECOMMENDED' ? sentence.sentenceId : null,
      text: sentence.source == 'RECOMMENDED' ? null : sentence.text,
    );

    setState(() {
      latestResult = result;
      hasResult = true;
    });
    await loadPracticeQuota();
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
        quota: practiceQuota,
        isLoadingQuota: isLoadingPracticeQuota,
        quotaErrorText: practiceQuotaError,
        onRetry: loadRecommendedSentences,
        onRetryQuota: loadPracticeQuota,
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
            remainingPractices: practiceQuota?.remainingPractices,
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
