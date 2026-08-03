// 파일 의도: lingko app 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'dart:math';

import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/evaluation_api.dart';
import '../api/practice_quota_api.dart';
import '../api/pronunciation_api.dart';
import '../api/sentence_api.dart';
import '../api/user_preferences_api.dart';
import '../models/auth_session.dart';
import '../models/evaluation_job.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_quota.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../screens/auth_gate_screen.dart';
import '../screens/home_screen.dart';
import '../screens/practice_screen.dart';
import '../screens/profile_screen.dart';
import '../screens/review_screen.dart';
import '../screens/result_screen.dart';
import '../services/audio_recorder_service.dart';
import '../services/app_auth_service.dart';
import '../services/sentence_speech_service.dart';
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
    this.sentenceSpeechService,
  });

  final PronunciationApi? pronunciationApi;
  final SentenceApi? sentenceApi;
  final EvaluationApi? evaluationApi;
  final UserPreferencesApi? userPreferencesApi;
  final PracticeQuotaApi? practiceQuotaApi;
  final AppAuthService? authService;
  final AudioRecorderService? audioRecorderService;
  final SentenceSpeechService? sentenceSpeechService;

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
        sentenceSpeechService:
            sentenceSpeechService ?? FlutterTtsSentenceSpeechService(),
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
    required this.sentenceSpeechService,
  });

  final PronunciationApi pronunciationApi;
  final SentenceApi sentenceApi;
  final EvaluationApi evaluationApi;
  final UserPreferencesApi userPreferencesApi;
  final PracticeQuotaApi practiceQuotaApi;
  final AppAuthService authService;
  final AudioRecorderService audioRecorderService;
  final SentenceSpeechService sentenceSpeechService;

  @override
  State<LingKoShell> createState() => _LingKoShellState();
}

/// Ling Ko Shell State Widget의 변경 가능한 화면 상태와 비동기 생명주기를 관리한다.
/// 불변 Widget 설정과 실행 시점 상태를 분리하기 위해 전용 State 객체를 사용한다.
class _LingKoShellState extends State<LingKoShell> {
  static const int evaluationPollingAttempts = 600;

  // 하단 탭 index입니다. 0: Home, 1: Practice, 2: Review, 3: Profile.
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
  EvaluationProgress evaluationProgress = const EvaluationProgress();

  // Practice 탭 안에서 연습 화면을 보여줄지, 결과 화면을 보여줄지 결정합니다.
  bool hasResult = false;
  bool isPracticeImmersive = false;

  @override
  void initState() {
    super.initState();
    restoreSession();
  }

  @override
  void dispose() {
    widget.audioRecorderService.dispose();
    widget.sentenceSpeechService.dispose();
    super.dispose();
  }

  Future<void> loadRecommendedSentences() async {
    setState(() {
      isLoadingRecommendedSentences = true;
      recommendedSentenceError = null;
    });

    try {
      final sentences = await widget.sentenceApi.fetchRecommendedSentences(
        limit: 50,
      );

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
        isPracticeImmersive = false;
        authErrorText = null;
        practiceQuota = null;
        practiceQuotaError = null;
        evaluationProgress = const EvaluationProgress();
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
    final normalizedSentence = sentence.normalizedForPractice();
    setState(() {
      selectedSentence = normalizedSentence;
      selectedTab = 1;
      hasResult = false;
      latestResult = null;
      evaluationProgress = const EvaluationProgress();
      isPracticeImmersive = false;
    });
  }

  /// Home의 직접 입력 진입점은 이전 추천 문장을 재사용하지 않고 빈 draft로 시작한다.
  void openCustomPractice() {
    setState(() {
      selectedSentence = null;
      selectedTab = 1;
      hasResult = false;
      latestResult = null;
      evaluationProgress = const EvaluationProgress();
      isPracticeImmersive = false;
    });
  }

  /// 기록의 과거 발음 snapshot을 재사용하지 않고 현재 백엔드 변환 규칙으로 다시 준비한다.
  Future<void> retryPractice(PracticeSentence sentence) async {
    try {
      final preparedSentence =
          sentence.sentenceId == null
              ? await widget.pronunciationApi.prepareCustomSentence(
                sentence.text,
              )
              : await widget.sentenceApi.fetchSentence(sentence.sentenceId!);

      if (mounted) {
        openPractice(preparedSentence);
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('현재 발음 규칙으로 문장을 준비하지 못했습니다. 다시 시도해 주세요.'),
          ),
        );
      }
    }
  }

  Future<void> evaluateRecording(
    PracticeSentence sentence,
    String audioPath,
  ) async {
    setState(() {
      selectedSentence = sentence;
      latestResult = null;
      hasResult = false;
      evaluationProgress = const EvaluationProgress(
        stage: EvaluationProgressStage.uploading,
        message: 'Uploading your recording.',
      );
    });

    try {
      final upload = await widget.authService.runAuthenticated(
        (accessToken) => widget.evaluationApi.prepareUpload(
          accessToken: accessToken,
          audioPath: audioPath,
        ),
      );
      await widget.evaluationApi.uploadAudio(
        upload: upload,
        audioPath: audioPath,
      );
      if (mounted) {
        setState(() {
          evaluationProgress = const EvaluationProgress(
            stage: EvaluationProgressStage.creatingJob,
            message: 'Creating your evaluation job.',
          );
        });
      }

      final idempotencyKey = _newEvaluationIdempotencyKey();
      EvaluationJob job = await widget.authService.runAuthenticated(
        (accessToken) => widget.evaluationApi.createJob(
          accessToken: accessToken,
          idempotencyKey: idempotencyKey,
          objectKey: upload.objectKey,
          sentenceId:
              sentence.source == 'RECOMMENDED' ? sentence.sentenceId : null,
          text: sentence.source == 'RECOMMENDED' ? null : sentence.text,
        ),
      );

      if (mounted) {
        setState(() {
          evaluationProgress = EvaluationProgress(
            stage: EvaluationProgressStage.analyzing,
            jobId: job.jobId,
            message:
                'Pronunciation analysis is running. You can use another tab.',
          );
        });
      }

      // 최초 취약 음절 영상 생성이 포함된 작업도 background polling으로 완료까지 기다린다.
      for (var attempt = 0; attempt < evaluationPollingAttempts; attempt++) {
        if (job.status == EvaluationJobStatus.succeeded && job.result != null) {
          if (mounted) {
            setState(() {
              evaluationProgress = EvaluationProgress(
                stage: EvaluationProgressStage.preparingFeedback,
                jobId: job.jobId,
                message: 'Preparing your feedback.',
              );
              latestResult = job.result;
              hasResult = true;
              isPracticeImmersive = false;
              evaluationProgress = EvaluationProgress(
                stage: EvaluationProgressStage.completed,
                jobId: job.jobId,
                message: 'Your pronunciation result is ready.',
              );
            });
          }
          await loadPracticeQuota();
          return;
        }
        if (job.status == EvaluationJobStatus.failed) {
          throw const ApiException('Pronunciation evaluation failed');
        }

        await Future<void>.delayed(const Duration(seconds: 1));
        job = await widget.authService.runAuthenticated(
          (accessToken) => widget.evaluationApi.fetchJob(
            accessToken: accessToken,
            jobId: job.jobId,
          ),
        );
      }

      throw const ApiException('Pronunciation evaluation timed out');
    } on AuthSessionExpiredException {
      if (mounted) {
        await handleSessionChanged(null);
      }
      rethrow;
    } catch (_) {
      if (mounted) {
        final failedAt = evaluationProgress.stage;
        setState(() {
          evaluationProgress = EvaluationProgress(
            stage: EvaluationProgressStage.failed,
            jobId: evaluationProgress.jobId,
            failedAt: failedAt,
            message:
                'The evaluation did not finish. Retry with the saved recording when available.',
          );
        });
      }
      rethrow;
    }
  }

  String _newEvaluationIdempotencyKey() {
    final random = Random.secure().nextInt(1 << 32);
    return 'evaluation-${DateTime.now().microsecondsSinceEpoch}-$random';
  }

  // Practice의 통합 입력에서 표준 발음 준비가 끝난 최신 문장으로 연습 대상을 교체합니다.
  void useCustomSentence(PracticeSentence sentence) {
    final normalizedSentence = sentence.normalizedForPractice();
    setState(() {
      selectedSentence = normalizedSentence;
      hasResult = false;
      latestResult = null;
      evaluationProgress = const EvaluationProgress();
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
        evaluationProgress: evaluationProgress,
        onRetry: loadRecommendedSentences,
        onRetryQuota: loadPracticeQuota,
        onSelect: openPractice,
        onOpenPractice: () => setState(() => selectedTab = 1),
        onOpenCustomPractice: openCustomPractice,
        onOpenReview: () => setState(() => selectedTab = 2),
        displayName: session?.user.name,
      ),
      hasResult && selectedSentence != null
          ? ResultScreen(
            sentence: selectedSentence!,
            result: latestResult,
            sentenceSpeechService: widget.sentenceSpeechService,
            onTryAgain: () {
              setState(() {
                hasResult = false;
                latestResult = null;
                evaluationProgress = const EvaluationProgress();
                selectedTab = 1;
              });
            },
            onOpenReview: () => setState(() => selectedTab = 2),
          )
          : PracticeScreen(
            sentence: selectedSentence,
            audioRecorderService: widget.audioRecorderService,
            sentenceSpeechService: widget.sentenceSpeechService,
            onEvaluateRecording: evaluateRecording,
            onCustomSentence: useCustomSentence,
            onPrepareCustomSentence:
                widget.pronunciationApi.prepareCustomSentence,
            remainingPractices: practiceQuota?.remainingPractices,
            evaluationProgress: evaluationProgress,
            onImmersiveModeChanged:
                (next) => setState(() => isPracticeImmersive = next),
            onContinueInBackground:
                () => setState(() {
                  isPracticeImmersive = false;
                  selectedTab = 0;
                }),
          ),
      ReviewScreen(
        evaluationApi: widget.evaluationApi,
        authService: widget.authService,
        session: session!,
        onRetryPractice: retryPractice,
        onSessionExpired: () => handleSessionChanged(null),
      ),
      ProfileScreen(
        userPreferencesApi: widget.userPreferencesApi,
        authService: widget.authService,
        session: session!,
        onSessionChanged: handleSessionChanged,
        onOpenReview: () => setState(() => selectedTab = 2),
      ),
    ];

    // Scaffold는 일반적인 앱 화면 뼈대입니다.
    // body에는 현재 화면, bottomNavigationBar에는 하단 탭을 둡니다.
    return Scaffold(
      body: SafeArea(child: pages[selectedTab]),
      bottomNavigationBar:
          selectedTab == 1 && isPracticeImmersive
              ? null
              : Container(
                decoration: const BoxDecoration(
                  border: Border(top: BorderSide(color: AppColors.border)),
                ),
                child: NavigationBar(
                  selectedIndex: selectedTab,
                  onDestinationSelected: (index) {
                    setState(() {
                      selectedTab = index;
                    });
                  },
                  destinations: const [
                    NavigationDestination(
                      icon: Icon(Icons.home_outlined),
                      selectedIcon: Icon(
                        Icons.home_rounded,
                        color: AppColors.primaryDark,
                      ),
                      label: 'Home',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.mic_none_rounded),
                      selectedIcon: Icon(
                        Icons.mic_rounded,
                        color: AppColors.primaryDark,
                      ),
                      label: 'Practice',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.rate_review_outlined),
                      selectedIcon: Icon(
                        Icons.rate_review_rounded,
                        color: AppColors.primaryDark,
                      ),
                      label: 'Review',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.person_outline_rounded),
                      selectedIcon: Icon(
                        Icons.person_rounded,
                        color: AppColors.primaryDark,
                      ),
                      label: 'Profile',
                    ),
                  ],
                ),
              ),
    );
  }
}
