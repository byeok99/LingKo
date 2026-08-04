// 파일 의도: 평가 점수를 사용자에게 노출해도 되는지를 나타내는 서버 상태값을 앱 타입으로 고정한다.
// 선택 이유: 문자열 비교가 화면마다 흩어지면 오타 하나가 조용히 "신뢰할 수 없는 점수 노출"로 이어진다.

/// 서버 `scoreStatus` 계약의 앱 표현이다.
///
/// 점수 필드가 null인 것만으로는 "0점"과 "측정하지 못함"을 구분할 수 없어 서버가 상태값을 함께 보낸다.
/// [unavailable]이면 점수는 항상 null이며, 화면은 숫자를 감추되 발음 가이드는 그대로 제공해야 한다.
enum ScoreStatus {
  available,
  unavailable;

  /// 서버 JSON 값을 앱 타입으로 변환하며, 아는 값이 아니면 [unavailable]로 닫는다.
  ///
  /// 앱은 서버와 동시에 배포되지 않으므로 구버전 앱이 새 상태값을 만나는 상황이 정상 경로다.
  /// 이때 모르는 값을 낙관적으로 해석하면 신뢰할 수 없는 점수를 사용자에게 보여주게 되므로,
  /// 판단이 서지 않는 모든 입력을 안전한 쪽으로 보내는 fail-closed 규칙을 택했다.
  static ScoreStatus fromWire(Object? raw) {
    return raw == 'AVAILABLE' ? ScoreStatus.available : ScoreStatus.unavailable;
  }

  /// 점수 유무만 아는 응답에서 상태를 유도한다. 과거 기록 API처럼 상태값이 없는 경우에 쓴다.
  static ScoreStatus ofNullableScore(Object? score) {
    return score == null ? ScoreStatus.unavailable : ScoreStatus.available;
  }

  bool get isAvailable => this == ScoreStatus.available;
}
