// 파일 의도: 회원가입 전에 필수·선택 동의를 받는 화면과 그 상호작용 경계를 구성한다.
// 선택 이유: 동의 결과는 상위가 저장·전송해야 하는 값이라, 화면은 선택 상태만 소유하고
// 문서 열기와 가입 진행은 주입된 callback에 위임한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';
import '../app/app_theme.dart';
import '../models/consent_selection.dart';
import '../widgets/shared_widgets.dart';

/// 회원가입 또는 문서 개정 후 재동의를 받는 화면이다.
///
/// 로그인 수단을 고르기 **전에** 보여준다. 계정이 만들어진 뒤에 동의를 받으면
/// 동의하지 않은 사용자의 개인정보가 이미 서버에 생성된 상태가 되어, 거부 시
/// 곧바로 삭제해야 하는 경로가 따로 필요해진다. 계정 생성 전에 받으면 그 경로가 없어도 된다.
class ConsentScreen extends StatefulWidget {
  const ConsentScreen({
    super.key,
    required this.onAgree,
    required this.onOpenDocument,
    required this.onCancel,
    this.documentVersion = consentDocumentVersion,
    this.errorText,
    this.isLoading = false,
  });

  /// 필수 항목을 모두 만족한 상태에서 계속하기를 눌렀을 때 호출한다.
  /// 상위는 이 값을 보관했다가 로그인 성공 후 서버에 전달한다.
  final void Function(ConsentSelection selection) onAgree;

  /// 약관·처리방침 전문을 여는 요청이다. 실제로 무엇을 여는지는 상위가 정한다.
  final void Function(ConsentDocument document) onOpenDocument;

  /// 동의하지 않고 이전 화면으로 돌아가는 요청이다.
  final VoidCallback onCancel;

  /// 서버가 현재로 판정한 문서 버전이다. 신규 로그인 전에는 앱 내 현재 버전을 쓴다.
  final String documentVersion;

  /// 서버 상태 확인 또는 기록에 실패했을 때 gate를 유지하면서 보여주는 안전한 안내다.
  final String? errorText;

  /// 중복 로그인·기록 요청을 막는 진행 상태다.
  final bool isLoading;

  @override
  State<ConsentScreen> createState() => _ConsentScreenState();
}

/// Consent Screen State 동의 선택 상태를 관리한다.
/// 불변 Widget 설정과 사용자가 바꾸는 선택 상태를 분리하기 위해 전용 State를 사용한다.
class _ConsentScreenState extends State<ConsentScreen> {
  bool termsAgreed = false;
  bool privacyAcknowledged = false;
  bool marketingOptIn = false;

  /// 필수 두 항목을 모두 만족해야 가입을 진행할 수 있다.
  /// 선택 항목인 마케팅 수신은 거부해도 이용에 제한이 없어야 하므로 판단에 넣지 않는다.
  bool get canProceed => termsAgreed && privacyAcknowledged;

  /// 전체 동의는 선택 항목까지 포함한 상태에서만 켜진 것으로 본다.
  /// 필수만 켠 상태를 "전체 동의"로 표시하면 사용자가 마케팅에도 동의한 것으로 오해한다.
  bool get isAllSelected =>
      termsAgreed && privacyAcknowledged && marketingOptIn;

  void toggleAll(bool next) {
    setState(() {
      termsAgreed = next;
      privacyAcknowledged = next;
      marketingOptIn = next;
    });
  }

  void submit() {
    if (!canProceed) {
      return;
    }
    widget.onAgree(
      ConsentSelection(
        termsAgreed: termsAgreed,
        privacyAcknowledged: privacyAcknowledged,
        marketingOptIn: marketingOptIn,
        documentVersion: widget.documentVersion,
        agreedAt: DateTime.now(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final palette = context.palette;
    return Scaffold(
      backgroundColor: palette.scaffold,
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(18, 8, 18, 26),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TopBar(
                    title: 'Before you start',
                    leading: IconButton(
                      key: const Key('consent-back'),
                      onPressed: widget.onCancel,
                      icon: const Icon(Icons.arrow_back_rounded),
                      tooltip: 'Back',
                    ),
                  ),
                  Expanded(
                    // 큰 글자 설정에서 항목이 길어지면 여기서만 스크롤한다.
                    // 하단 버튼은 항상 같은 자리에 남아 진행 지점을 잃지 않게 한다.
                    child: SingleChildScrollView(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const SizedBox(height: AppSpacing.sm),
                          if (widget.errorText != null) ...[
                            Text(
                              widget.errorText!,
                              key: const Key('consent-error'),
                              style: Theme.of(
                                context,
                              ).textTheme.bodyMedium?.copyWith(
                                color: palette.error,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(height: AppSpacing.md),
                          ],
                          Text(
                            'Review and agree to continue. You can change the '
                            'optional item at any time in Settings.',
                            style: Theme.of(
                              context,
                            ).textTheme.bodyLarge?.copyWith(
                              fontSize: 14,
                              height: 1.7,
                              color: palette.textSecondary,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.xl),
                          _AgreeAllTile(
                            isSelected: isAllSelected,
                            onChanged: toggleAll,
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          _ConsentTile(
                            id: 'terms',
                            label: 'Terms of Service',
                            required: true,
                            isSelected: termsAgreed,
                            onChanged:
                                (next) => setState(() => termsAgreed = next),
                            onOpenDocument:
                                () => widget.onOpenDocument(
                                  ConsentDocument.termsOfService,
                                ),
                          ),
                          _ConsentTile(
                            id: 'privacy',
                            // 처리방침은 동의가 아니라 확인 대상이라 문구를 구분한다.
                            // 자세한 근거는 ConsentDocument.privacyPolicy 주석에 있다.
                            label: 'How we handle your data',
                            required: true,
                            isSelected: privacyAcknowledged,
                            onChanged:
                                (next) =>
                                    setState(() => privacyAcknowledged = next),
                            onOpenDocument:
                                () => widget.onOpenDocument(
                                  ConsentDocument.privacyPolicy,
                                ),
                          ),
                          _ConsentTile(
                            id: 'marketing',
                            label: 'News about events and new features',
                            required: false,
                            isSelected: marketingOptIn,
                            onChanged:
                                (next) => setState(() => marketingOptIn = next),
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          // 선택 항목을 거부해도 불이익이 없다는 점을 화면에서 직접 말한다.
                          // 목록의 [Optional] 표시만으로는 거부해도 되는지 판단하기 어렵다.
                          Text(
                            'Declining the optional item does not limit your '
                            'use of LingKo.',
                            key: const Key('consent-optional-notice'),
                            style: Theme.of(
                              context,
                            ).textTheme.bodyMedium?.copyWith(height: 1.6),
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          _AgeNotice(),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xl),
                  PrimaryButton(
                    key: const Key('consent-continue'),
                    label: 'Agree and continue',
                    // 필수 항목이 채워지기 전에는 비활성으로 둔다. 눌리게 두고 경고를
                    // 띄우면 무엇이 부족한지 목록에서 다시 찾아야 한다.
                    onPressed: canProceed ? submit : null,
                    isLoading: widget.isLoading,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// 전체 동의 행이다. 필수와 선택을 한 번에 켜고 끈다.
class _AgreeAllTile extends StatelessWidget {
  const _AgreeAllTile({required this.isSelected, required this.onChanged});

  final bool isSelected;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final palette = context.palette;
    return InkWell(
      key: const Key('consent-agree-all'),
      borderRadius: BorderRadius.circular(AppSizes.radius),
      onTap: () => onChanged(!isSelected),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.lg,
        ),
        decoration: BoxDecoration(
          color: isSelected ? palette.softBlue : palette.card,
          border: Border.all(
            color: isSelected ? palette.primaryDark : palette.border,
          ),
          borderRadius: BorderRadius.circular(AppSizes.radius),
        ),
        child: Row(
          children: [
            _CheckMark(isSelected: isSelected),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Text(
                'Agree to all',
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 개별 동의 항목 행이다.
///
/// [onOpenDocument]가 null이면 전문 보기 버튼을 그리지 않는다. 열 문서가 없는 항목에
/// 눌리는 버튼을 두면 눌러도 아무 일이 없어 고장으로 보인다.
class _ConsentTile extends StatelessWidget {
  const _ConsentTile({
    required this.id,
    required this.label,
    required this.required,
    required this.isSelected,
    required this.onChanged,
    this.onOpenDocument,
  });

  /// 행과 전문 보기 버튼의 Key를 만드는 식별자다. `consent-<id>`, `consent-<id>-view`가 된다.
  final String id;
  final String label;
  final bool required;
  final bool isSelected;
  final ValueChanged<bool> onChanged;
  final VoidCallback? onOpenDocument;

  @override
  Widget build(BuildContext context) {
    final palette = context.palette;
    final openDocument = onOpenDocument;
    return Container(
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: palette.border)),
      ),
      child: Row(
        children: [
          Expanded(
            child: InkWell(
              key: Key('consent-$id'),
              onTap: () => onChanged(!isSelected),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  vertical: AppSpacing.lg,
                  horizontal: AppSpacing.xs,
                ),
                child: Row(
                  children: [
                    _CheckMark(isSelected: isSelected),
                    const SizedBox(width: AppSpacing.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            required ? '[Required]' : '[Optional]',
                            style: Theme.of(
                              context,
                            ).textTheme.bodyMedium?.copyWith(
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                              color:
                                  required
                                      ? palette.primaryDark
                                      : palette.textSecondary,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            label,
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (openDocument != null)
            TextButton(
              key: Key('consent-$id-view'),
              onPressed: openDocument,
              child: const Text('View'),
            ),
        ],
      ),
    );
  }
}

/// 선택 여부를 보여주는 표시다.
///
/// Checkbox 위젯 대신 직접 그린다. Material Checkbox는 자체 tap 영역과 색 규칙을 가져
/// 행 전체를 눌러 선택하는 이 화면의 동작과 두 겹으로 겹친다.
class _CheckMark extends StatelessWidget {
  const _CheckMark({required this.isSelected});

  final bool isSelected;

  @override
  Widget build(BuildContext context) {
    final palette = context.palette;
    return Container(
      width: 24,
      height: 24,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: isSelected ? palette.primaryDark : Colors.transparent,
        border: Border.all(
          color: isSelected ? palette.primaryDark : palette.border,
          width: 1.5,
        ),
        shape: BoxShape.circle,
      ),
      child: Icon(
        Icons.check_rounded,
        size: 16,
        color: isSelected ? palette.card : palette.border,
      ),
    );
  }
}

/// 이용 가능 연령을 알리는 안내다.
///
/// 앱에 실제 연령 확인 수단이 없으므로 이 문구는 고지일 뿐 차단 장치가 아니다.
/// 연령 확인 절차가 생기면 이 안내 대신 그 절차를 연결한다.
class _AgeNotice extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final palette = context.palette;
    return Container(
      key: const Key('consent-age-notice'),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: palette.neutralFill,
        borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
      ),
      child: Text(
        'LingKo is for users aged 16 and over.',
        style: Theme.of(
          context,
        ).textTheme.bodyMedium?.copyWith(color: palette.textSecondary),
      ),
    );
  }
}
