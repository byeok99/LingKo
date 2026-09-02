// 파일 의도: lingko app 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'app_palette.dart';

import '../api/api_client.dart';
import '../api/evaluation_api.dart';
import '../api/practice_content_api.dart';
import '../api/practice_quota_api.dart';
import '../api/pronunciation_api.dart';
import '../api/sentence_api.dart';
import '../models/auth_session.dart';
import '../models/consent_selection.dart';
import '../models/evaluation_job.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_quota.dart';
import '../models/ad_reward_session.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../models/weak_sound.dart';
import '../screens/auth_gate_screen.dart';
import '../screens/consent_screen.dart';
import '../screens/home_screen.dart';
import '../screens/practice_screen.dart';
import '../screens/profile_screen.dart';
import '../screens/review_screen.dart';
import '../screens/saved_sentences_screen.dart';
import '../screens/sound_detail_screen.dart';
import '../screens/result_screen.dart';
import '../services/audio_recorder_service.dart';
import '../services/legal_document_launcher.dart';
import '../services/app_auth_service.dart';
import '../services/sentence_speech_service.dart';
import '../services/rewarded_ad_service.dart';
import 'app_theme.dart';

/// 동의 gate가 끝날 때까지 사용자가 고른 native 인증 흐름을 보존한다.
enum _SignInProvider { google, apple }

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
    this.practiceQuotaApi,
    this.practiceContentApi,
    this.authService,
    this.audioRecorderService,
    this.sentenceSpeechService,
    this.legalDocumentLauncher,
    this.practiceRewardAdService,
    this.onRequestPracticeReward,
  });

  final PronunciationApi? pronunciationApi;
  final SentenceApi? sentenceApi;
  final EvaluationApi? evaluationApi;
  final PracticeQuotaApi? practiceQuotaApi;
  final PracticeContentApi? practiceContentApi;
  final AppAuthService? authService;
  final AudioRecorderService? audioRecorderService;
  final SentenceSpeechService? sentenceSpeechService;
  final LegalDocumentLauncher? legalDocumentLauncher;
  final PracticeRewardAdService? practiceRewardAdService;

  /// 테스트·호스트 앱이 광고 지급 흐름 전체를 대체할 때만 사용하는 override다.
  final Future<void> Function()? onRequestPracticeReward;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LingKo',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      // 기기 설정을 따른다. 앱 안에 별도 토글을 두면 시스템 다크 모드를 쓰는 사용자가
      // 두 곳을 관리해야 하므로, 앱 내 선택지가 실제로 필요해질 때까지는 시스템에 맡긴다.
      themeMode: ThemeMode.system,
      // LingKoShell은 하단 탭과 현재 선택된 화면 상태를 관리합니다.
      home: LingKoShell(
        pronunciationApi: pronunciationApi ?? DartIoPronunciationApi(),
        sentenceApi: sentenceApi ?? DartIoSentenceApi(),
        evaluationApi: evaluationApi ?? DartIoEvaluationApi(),
        practiceQuotaApi: practiceQuotaApi ?? DartIoPracticeQuotaApi(),
        practiceContentApi: practiceContentApi ?? DartIoPracticeContentApi(),
        authService: authService ?? DefaultAppAuthService(),
        audioRecorderService:
            audioRecorderService ?? RecordAudioRecorderService(),
        sentenceSpeechService:
            sentenceSpeechService ?? FlutterTtsSentenceSpeechService(),
        legalDocumentLauncher:
            legalDocumentLauncher ?? UrlLauncherLegalDocumentLauncher(),
        practiceRewardAdService:
            practiceRewardAdService ?? GooglePracticeRewardAdService(),
        onRequestPracticeReward: onRequestPracticeReward,
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
    required this.practiceQuotaApi,
    required this.practiceContentApi,
    required this.authService,
    required this.audioRecorderService,
    required this.sentenceSpeechService,
    required this.legalDocumentLauncher,
    required this.practiceRewardAdService,
    this.onRequestPracticeReward,
  });

  final PronunciationApi pronunciationApi;
  final SentenceApi sentenceApi;
  final EvaluationApi evaluationApi;
  final PracticeQuotaApi practiceQuotaApi;
  final PracticeContentApi practiceContentApi;
  final AppAuthService authService;
  final AudioRecorderService audioRecorderService;
  final SentenceSpeechService sentenceSpeechService;
  final LegalDocumentLauncher legalDocumentLauncher;
  final PracticeRewardAdService practiceRewardAdService;
  final Future<void> Function()? onRequestPracticeReward;

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

  /// 로그인 수단을 고르기 전에 동의 화면을 띄우고 있는지 여부다.
  bool isConsentOpen = false;

  /// 동의 상태 확인·로그인·서버 기록 중 중복 제출을 막는다.
  bool isSubmittingConsent = false;

  /// 동의 gate를 통과시키지 않은 채 사용자에게 재시도 이유만 알려준다.
  String? consentErrorText;

  /// 복원 세션은 서버가 알려준 최신 버전을 사용하고, 로그인 전에는 앱 내 버전을 사용한다.
  String consentGateDocumentVersion = consentDocumentVersion;

  /// 동의 화면에서 받은 선택이다. 로그인 성공 후 서버에 전달할 때까지 들고 있는다.
  ///
  /// null이면 아직 이번 가입 흐름에서 동의를 받지 않았다는 뜻이다. 로그인에 실패하면
  /// 값을 비워 다음 시도에서 동의를 다시 받는다. 동의만 남고 계정이 없는 상태를 만들지 않는다.
  ConsentSelection? pendingConsent;

  /// 계정 생성 전 동의 gate를 통과한 뒤 호출할 사용자의 실제 로그인 수단이다.
  _SignInProvider? pendingSignInProvider;
  PracticeQuota? practiceQuota;
  bool isLoadingPracticeQuota = false;
  String? practiceQuotaError;
  EvaluationProgress evaluationProgress = const EvaluationProgress();

  // Practice 탭 안에서 연습 화면을 보여줄지, 결과 화면을 보여줄지 결정합니다.
  bool hasResult = false;
  bool isPracticeImmersive = false;
  bool isRequestingPracticeReward = false;

  /// Home 위에 겹쳐 여는 화면이다. 탭이 아니라 갈래길이라 탭바 index와 분리한다.
  /// null이면 겹친 화면이 없다.
  String? openSoundDetail;
  bool isSavedSentencesOpen = false;

  /// Home 타일에 쓰는 취약 음절이다. 비어 있으면 타일 영역 자체를 그리지 않는다.
  List<WeakSound> weakSounds = const [];

  /// 저장한 문장의 식별자다. 여러 화면이 같은 저장 상태를 보여줘야 해서
  /// 화면마다 따로 조회하지 않고 shell이 하나만 들고 내려준다.
  Set<int> savedSentenceIds = const {};

  /// 서버 응답을 기다리는 동안 중복 요청을 막는다.
  final Set<int> pendingSavedToggles = {};

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

  Future<void> loadWeakSounds() async {
    final currentSession = session;
    if (currentSession == null) {
      return;
    }
    try {
      final sounds = await widget.authService.runAuthenticated(
        (accessToken) =>
            widget.practiceContentApi.fetchWeakSounds(accessToken: accessToken),
      );
      if (mounted) {
        setState(() => weakSounds = sounds);
      }
    } catch (_) {
      // 취약 음절은 보조 정보다. 실패해도 Home의 문장 목록은 그대로 쓸 수 있어야 하므로
      // 화면에 오류를 띄우지 않고 타일만 비운다.
      if (mounted) {
        setState(() => weakSounds = const []);
      }
    }
  }

  Future<void> loadSavedSentenceIds() async {
    if (session == null) {
      return;
    }
    try {
      final saved = await widget.authService.runAuthenticated(
        (accessToken) => widget.practiceContentApi.fetchSavedSentences(
          accessToken: accessToken,
        ),
      );
      if (mounted) {
        setState(() {
          savedSentenceIds = {
            for (final sentence in saved)
              if (sentence.sentenceId != null) sentence.sentenceId!,
          };
        });
      }
    } catch (_) {
      // 저장 상태는 보조 정보다. 실패하면 북마크가 꺼진 것처럼 보이지만
      // 문장 연습 자체는 막지 않는다.
    }
  }

  /// 저장 상태를 뒤집는다.
  ///
  /// 화면을 먼저 바꾸고 서버에 알린다. 왕복을 기다리면 북마크가 한 박자 늦게 반응해
  /// 눌리지 않은 것처럼 느껴진다. 실패하면 되돌린다.
  Future<void> toggleSavedSentence(int sentenceId) async {
    if (pendingSavedToggles.contains(sentenceId)) {
      return;
    }
    final wasSaved = savedSentenceIds.contains(sentenceId);
    setState(() {
      pendingSavedToggles.add(sentenceId);
      savedSentenceIds = {
        for (final id in savedSentenceIds)
          if (id != sentenceId) id,
        if (!wasSaved) sentenceId,
      };
    });

    try {
      final saved = await widget.authService.runAuthenticated(
        (accessToken) => widget.practiceContentApi.toggleSavedSentence(
          accessToken: accessToken,
          sentenceId: sentenceId,
        ),
      );
      if (!mounted) {
        return;
      }
      setState(() {
        pendingSavedToggles.remove(sentenceId);
        // 서버가 알려준 실제 상태로 맞춘다. 두 기기에서 동시에 눌렀을 때
        // 화면이 서버와 어긋나 있을 수 있다.
        savedSentenceIds = {
          for (final id in savedSentenceIds)
            if (id != sentenceId) id,
          if (saved) sentenceId,
        };
      });
    } on AuthSessionExpiredException {
      handleSessionChanged(null);
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        pendingSavedToggles.remove(sentenceId);
        savedSentenceIds = {
          for (final id in savedSentenceIds)
            if (id != sentenceId) id,
          if (wasSaved) sentenceId,
        };
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not update this sentence.')),
      );
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
        try {
          // 토큰 존재만으로 Home을 열면 이전 버전 사용자가 동의 gate를 우회한다.
          // 보호 API가 현재 사용자·현재 문서 버전을 판정한 뒤에만 앱 데이터를 노출한다.
          final consentStatus =
              await widget.authService.fetchLegalConsentStatus();
          if (!mounted) {
            return;
          }
          if (consentStatus.required) {
            setState(() {
              isConsentOpen = true;
              consentGateDocumentVersion = consentStatus.documentVersion;
              consentErrorText = null;
            });
            return;
          }
          await loadAuthenticatedData(restoredSession);
        } on AuthSessionExpiredException {
          rethrow;
        } catch (_) {
          if (!mounted) {
            return;
          }
          // 상태 조회 실패를 동의 완료로 간주하면 장애 순간에 gate가 열린다.
          // 서버가 복구되면 같은 화면에서 제출해 재확인할 수 있도록 fail-closed 한다.
          setState(() {
            isConsentOpen = true;
            consentGateDocumentVersion = consentDocumentVersion;
            consentErrorText =
                'Could not verify your agreement. Please try again.';
          });
        }
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

  /// 로그인 수단을 누르면 곧바로 인증하지 않고 동의 화면을 먼저 연다.
  ///
  /// 계정이 만들어진 뒤에 동의를 받으면, 거부한 사용자의 개인정보가 이미 서버에 생긴
  /// 상태가 되어 즉시 삭제하는 경로를 따로 만들어야 한다. 계정 생성 전에 받으면 그 경로가 없어도 된다.
  void openConsent(_SignInProvider provider) {
    setState(() {
      pendingSignInProvider = provider;
      isConsentOpen = true;
      consentGateDocumentVersion = consentDocumentVersion;
      consentErrorText = null;
      authErrorText = null;
    });
  }

  /// Review Notes의 코드를 검증해 기존 계정 세션을 복원하고 최신 동의 상태까지 확인한다.
  Future<void> signInForReview(String accessCode) async {
    if (isSigningIn) {
      return;
    }
    setState(() {
      isSigningIn = true;
      authErrorText = null;
    });

    late final AuthSession activeSession;
    try {
      activeSession = await widget.authService.signInForReview(accessCode);
      if (!mounted) {
        return;
      }
      setState(() {
        session = activeSession;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        authErrorText =
            error is ApiException && error.statusCode == 429
                ? 'Too many review access attempts. Please try again later.'
                : 'Unable to verify review access. Please try again.';
      });
      return;
    } finally {
      if (mounted && session == null) {
        setState(() {
          isSigningIn = false;
        });
      }
    }

    try {
      final consentStatus = await widget.authService.fetchLegalConsentStatus();
      if (!mounted) {
        return;
      }
      if (consentStatus.required) {
        setState(() {
          pendingSignInProvider = null;
          isConsentOpen = true;
          consentGateDocumentVersion = consentStatus.documentVersion;
          consentErrorText = null;
        });
        return;
      }
      await loadAuthenticatedData(activeSession);
    } on AuthSessionExpiredException {
      await handleSessionChanged(null);
      if (mounted) {
        setState(() {
          authErrorText = 'Your session expired. Please sign in again.';
        });
      }
    } catch (_) {
      if (!mounted) {
        return;
      }
      // 상태 확인 장애를 동의 완료로 간주하지 않고 review 계정도 같은 fail-closed gate에 둔다.
      setState(() {
        isConsentOpen = true;
        consentGateDocumentVersion = consentDocumentVersion;
        consentErrorText = 'Could not verify your agreement. Please try again.';
      });
    } finally {
      if (mounted) {
        setState(() {
          isSigningIn = false;
        });
      }
    }
  }

  /// 동의 화면에서 필수 항목을 채우고 계속하기를 누른 뒤의 흐름이다.
  Future<void> acceptConsentAndSignIn(ConsentSelection selection) async {
    if (isSubmittingConsent) {
      return;
    }
    setState(() {
      pendingConsent = selection;
      isSubmittingConsent = true;
      consentErrorText = null;
    });

    AuthSession? activeSession = session;
    if (activeSession == null) {
      setState(() {
        isSigningIn = true;
      });
      try {
        final provider = pendingSignInProvider;
        if (provider == null) {
          throw StateError('Sign-in provider was not selected');
        }
        activeSession = switch (provider) {
          _SignInProvider.google => await widget.authService.signInWithGoogle(),
          _SignInProvider.apple => await widget.authService.signInWithApple(),
        };
        if (!mounted) {
          return;
        }
        setState(() {
          session = activeSession;
        });
      } catch (_) {
        if (!mounted) {
          return;
        }
        final failedProvider = pendingSignInProvider;
        setState(() {
          pendingConsent = null;
          pendingSignInProvider = null;
          isConsentOpen = false;
          isSubmittingConsent = false;
          authErrorText = switch (failedProvider) {
            _SignInProvider.apple =>
              'Unable to sign in with Apple. Please try again.',
            _ => 'Unable to sign in with Google. Please try again.',
          };
        });
        return;
      } finally {
        if (mounted) {
          setState(() {
            isSigningIn = false;
          });
        }
      }
    }

    try {
      final status = await widget.authService.recordLegalConsent(selection);
      if (!mounted) {
        return;
      }
      if (status.required) {
        throw StateError('Server did not accept current consent');
      }
      setState(() {
        isConsentOpen = false;
        pendingConsent = null;
        pendingSignInProvider = null;
        consentErrorText = null;
      });
      await loadAuthenticatedData(activeSession);
    } on AuthSessionExpiredException {
      if (!mounted) {
        return;
      }
      await handleSessionChanged(null);
      if (mounted) {
        setState(() {
          authErrorText = 'Your session expired. Please sign in again.';
        });
      }
    } catch (_) {
      if (!mounted) {
        return;
      }
      // 앱보다 서버 문서 버전이 먼저 올라간 배포 순서에서도 다음 재시도가 같은
      // 구버전 body를 반복하지 않게 현재 상태를 다시 읽는다.
      String nextDocumentVersion = consentGateDocumentVersion;
      try {
        final latestStatus = await widget.authService.fetchLegalConsentStatus();
        if (latestStatus.required) {
          nextDocumentVersion = latestStatus.documentVersion;
        }
      } catch (_) {
        // 원래 저장 실패 안내가 우선이다. 보조 조회 실패로 원인을 덮지 않는다.
      }
      if (!mounted) {
        return;
      }
      // 인증은 성공했지만 동의 기록이 실패한 경우 Home으로 보내지 않는다.
      // 저장된 세션으로 같은 제출을 재시도할 수 있게 gate와 선택 화면을 유지한다.
      setState(() {
        isConsentOpen = true;
        consentGateDocumentVersion = nextDocumentVersion;
        consentErrorText = 'Could not save your agreement. Please try again.';
      });
    } finally {
      if (mounted) {
        setState(() {
          isSubmittingConsent = false;
        });
      }
    }
  }

  /// 동의 gate가 열린 복원 세션에서 뒤로 가면 서비스 대신 로그아웃 화면으로 돌아간다.
  Future<void> cancelConsent() async {
    if (session == null) {
      setState(() {
        isConsentOpen = false;
        pendingConsent = null;
        pendingSignInProvider = null;
        consentErrorText = null;
      });
      return;
    }
    try {
      await widget.authService.signOut();
    } finally {
      if (mounted) {
        await handleSessionChanged(null);
      }
    }
  }

  /// 동의가 확인된 사용자에게만 필요한 초기 데이터를 병렬로 불러온다.
  Future<void> loadAuthenticatedData(AuthSession authSession) async {
    await Future.wait([
      loadRecommendedSentences(),
      loadPracticeQuota(authSession),
    ]);
    await Future.wait([loadWeakSounds(), loadSavedSentenceIds()]);
  }

  /// 약관·처리방침 전문 열기 요청을 처리한다. 가입 동의 화면과 Profile 설정이 함께 쓴다.
  ///
  /// 백엔드가 서빙하는 공개 URL을 브라우저로 연다. 두 화면이 같은 경로를 쓰므로
  /// 문서 위치가 바뀌어도 여기만 고치면 된다.
  Future<void> openLegalDocument(ConsentDocument document) async {
    final opened = await widget.legalDocumentLauncher.open(document);
    if (opened || !mounted) {
      return;
    }
    // 브라우저를 열 수 없는 기기가 있다. 아무 반응이 없으면 버튼이 고장난 것으로 보이므로
    // 실패를 알리고 문의 경로가 생기면 그쪽으로 안내한다.
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Could not open the document.')),
    );
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
        isConsentOpen = false;
        isSubmittingConsent = false;
        consentErrorText = null;
        consentGateDocumentVersion = consentDocumentVersion;
        pendingConsent = null;
        pendingSignInProvider = null;
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

  /// 광고 SDK가 실제 reward callback을 전달한 경우에만 서버에 1회 지급을 요청한다.
  Future<void> requestPracticeReward() async {
    if (isRequestingPracticeReward) {
      return;
    }
    setState(() => isRequestingPracticeReward = true);
    try {
      final override = widget.onRequestPracticeReward;
      if (override != null) {
        await override();
        if (mounted) {
          await loadPracticeQuota();
        }
        return;
      }

      final session = await widget.authService.runAuthenticated(
        (accessToken) => widget.practiceQuotaApi.createAdRewardSession(
          accessToken: accessToken,
        ),
      );
      final result = await widget.practiceRewardAdService.show(
        customData: session.sessionToken,
      );
      if (result != RewardedAdResult.earned) {
        return;
      }
      for (var attempt = 0; attempt < 10; attempt++) {
        final status = await widget.authService.runAuthenticated(
          (accessToken) => widget.practiceQuotaApi.fetchAdRewardSessionStatus(
            accessToken: accessToken,
            sessionToken: session.sessionToken,
          ),
        );
        if (status.status == AdRewardStatus.completed) {
          if (mounted) {
            await loadPracticeQuota();
          }
          return;
        }
        if (status.status == AdRewardStatus.expired) {
          throw StateError('Ad reward session expired');
        }
        await Future<void>.delayed(const Duration(seconds: 1));
      }
      if (mounted) {
        await loadPracticeQuota();
      }
    } on AuthSessionExpiredException {
      if (mounted) {
        await handleSessionChanged(null);
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Unable to add a practice right now.')),
        );
      }
    } finally {
      if (mounted) {
        setState(() => isRequestingPracticeReward = false);
      }
    }
  }

  /// Profile에서 UMP의 광고 개인정보 선택 화면을 다시 연다.
  Future<void> openAdPrivacyOptions() async {
    try {
      await widget.practiceRewardAdService.showPrivacyOptions();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Unable to open ad privacy settings.')),
        );
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
        uploadFraction: 0,
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
        onProgress: (progress) {
          if (!mounted ||
              evaluationProgress.stage != EvaluationProgressStage.uploading) {
            return;
          }
          setState(() {
            evaluationProgress = EvaluationProgress(
              stage: EvaluationProgressStage.uploading,
              message: 'Uploading your recording.',
              uploadFraction: progress,
            );
          });
        },
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
          evaluationProgress = EvaluationProgress.fromJob(job);
        });
        // 서버가 평가 기회를 예약한 직후 Home의 남은 수량도 같은 값으로 맞춘다.
        await loadPracticeQuota();
      }

      // 최초 취약 음절 영상 생성이 포함된 작업도 background polling으로 완료까지 기다린다.
      for (var attempt = 0; attempt < evaluationPollingAttempts; attempt++) {
        if (job.status == EvaluationJobStatus.succeeded && job.result != null) {
          if (mounted) {
            setState(() {
              latestResult = job.result;
              hasResult = true;
              isPracticeImmersive = false;
              evaluationProgress = EvaluationProgress.fromJob(job);
            });
            // 평가는 수 분이 걸릴 수 있어 사용자가 다른 일을 하고 있을 가능성이 높다.
            // 결과 도착과 실패를 소리 없이도 알 수 있게 촉각으로 구분해 알린다.
            unawaited(HapticFeedback.lightImpact());
          }
          await loadPracticeQuota();
          return;
        }
        if (job.status == EvaluationJobStatus.failed) {
          throw const ApiException('Pronunciation evaluation failed');
        }

        if (mounted) {
          final nextProgress = EvaluationProgress.fromJob(job);
          if (nextProgress.stage != evaluationProgress.stage ||
              nextProgress.message != evaluationProgress.message) {
            setState(() {
              evaluationProgress = nextProgress;
            });
          }
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
        unawaited(HapticFeedback.heavyImpact());
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

    if (isConsentOpen) {
      return ConsentScreen(
        documentVersion: consentGateDocumentVersion,
        errorText: consentErrorText,
        isLoading: isSubmittingConsent,
        onAgree: (selection) => unawaited(acceptConsentAndSignIn(selection)),
        onOpenDocument: (document) => unawaited(openLegalDocument(document)),
        onCancel: () => unawaited(cancelConsent()),
      );
    }

    if (session == null) {
      return LoginScreen(
        isLoading: isSigningIn,
        errorText: authErrorText,
        onSignInWithGoogle: () => openConsent(_SignInProvider.google),
        onSignInWithApple: () => openConsent(_SignInProvider.apple),
        onSignInForReview: signInForReview,
        // Android는 Service ID·HTTPS redirect 계약이 별도라 native iOS 범위에서만 노출한다.
        showAppleSignIn: defaultTargetPlatform == TargetPlatform.iOS,
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
        weakSounds: weakSounds,
        savedSentenceIds: savedSentenceIds,
        onToggleSaved: toggleSavedSentence,
        onSelectWeakSound:
            (sound) => setState(() => openSoundDetail = sound.text),
        onOpenPractice: () => setState(() => selectedTab = 1),
        onOpenCustomPractice: openCustomPractice,
        onRequestPracticeReward:
            (widget.onRequestPracticeReward == null &&
                        !widget.practiceRewardAdService.isConfigured) ||
                    isRequestingPracticeReward
                ? null
                : requestPracticeReward,
        displayName: session?.user.name,
      ),
      hasResult && selectedSentence != null
          ? ResultScreen(
            sentence: selectedSentence!,
            result: latestResult,
            // 추천 문장만 저장할 수 있다. 직접 입력한 문장은 서버에 식별자가 없다.
            isSaved:
                selectedSentence!.sentenceId == null
                    ? null
                    : savedSentenceIds.contains(selectedSentence!.sentenceId),
            onToggleSaved: () {
              final id = selectedSentence!.sentenceId;
              if (id != null) {
                unawaited(toggleSavedSentence(id));
              }
            },
            onTryAgain: () {
              setState(() {
                hasResult = false;
                latestResult = null;
                evaluationProgress = const EvaluationProgress();
                selectedTab = 1;
              });
            },
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
        authService: widget.authService,
        session: session!,
        onSessionChanged: handleSessionChanged,
        onOpenReview: () => setState(() => selectedTab = 2),
        onOpenDocument: (document) => unawaited(openLegalDocument(document)),
        onOpenSavedSentences: () => setState(() => isSavedSentencesOpen = true),
        onOpenAdPrivacy:
            widget.practiceRewardAdService.isConfigured
                ? () => unawaited(openAdPrivacyOptions())
                : null,
        // 문의 창구는 아직 붙지 않아 연결 전까지 행을 비활성으로 둔다.
        onOpenContact: null,
      ),
    ];

    // Scaffold는 일반적인 앱 화면 뼈대입니다.
    // body에는 현재 화면, bottomNavigationBar에는 하단 탭을 둡니다.
    // 겹쳐 여는 화면은 탭 위에 얹는다. 탭바를 유지해 사용자가 어디에 있는지 잃지 않게 한다.
    final character = openSoundDetail;
    final Widget body;
    if (character != null) {
      body = SoundDetailScreen(
        character: character,
        practiceContentApi: widget.practiceContentApi,
        authService: widget.authService,
        onSessionExpired: () => handleSessionChanged(null),
        onSelectSentence: (sentence) {
          setState(() => openSoundDetail = null);
          openPractice(sentence);
        },
        onClose: () => setState(() => openSoundDetail = null),
      );
    } else if (isSavedSentencesOpen) {
      body = SavedSentencesScreen(
        practiceContentApi: widget.practiceContentApi,
        authService: widget.authService,
        onSessionExpired: () => handleSessionChanged(null),
        onSelect: (sentence) {
          setState(() => isSavedSentencesOpen = false);
          openPractice(sentence);
        },
        onClose: () {
          setState(() => isSavedSentencesOpen = false);
          // 저장 화면에서 해제한 결과를 Home 북마크에도 반영한다.
          unawaited(loadSavedSentenceIds());
        },
      );
    } else {
      body = pages[selectedTab];
    }

    return Scaffold(
      body: SafeArea(child: body),
      bottomNavigationBar:
          selectedTab == 1 && isPracticeImmersive
              ? null
              : Container(
                decoration: BoxDecoration(
                  border: Border(
                    top: BorderSide(color: context.palette.border),
                  ),
                ),
                child: NavigationBar(
                  selectedIndex: selectedTab,
                  onDestinationSelected: (index) {
                    setState(() {
                      // 탭을 누르면 겹쳐 있던 화면을 닫는다. 남겨두면 탭을 눌렀는데
                      // 화면이 그대로인 것처럼 보인다.
                      openSoundDetail = null;
                      isSavedSentencesOpen = false;
                      selectedTab = index;
                    });
                  },
                  destinations: [
                    NavigationDestination(
                      icon: Icon(Icons.home_outlined),
                      selectedIcon: Icon(
                        Icons.home_rounded,
                        color: context.palette.primaryDark,
                      ),
                      label: 'Home',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.mic_none_rounded),
                      selectedIcon: Icon(
                        Icons.mic_rounded,
                        color: context.palette.primaryDark,
                      ),
                      label: 'Practice',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.rate_review_outlined),
                      selectedIcon: Icon(
                        Icons.rate_review_rounded,
                        color: context.palette.primaryDark,
                      ),
                      label: 'Review',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.person_outline_rounded),
                      selectedIcon: Icon(
                        Icons.person_rounded,
                        color: context.palette.primaryDark,
                      ),
                      label: 'Profile',
                    ),
                  ],
                ),
              ),
    );
  }
}
