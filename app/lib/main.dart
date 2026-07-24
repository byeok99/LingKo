// 파일 의도: LingKo Flutter 앱의 main 진입 책임을 정의한다.
// 선택 이유: bootstrap 코드는 작게 유지하고 기능 동작은 각 소유 계층에 위임한다.

import 'package:flutter/material.dart';

import 'app/lingko_app.dart';

// Flutter 앱의 시작점입니다. runApp에 넘긴 위젯이 화면 전체의 루트가 됩니다.
void main() {
  runApp(const LingKoApp());
}
