// 파일 의도: api client 백엔드 통신 경계를 정의한다.
// 선택 이유: HTTP 전송과 JSON 매핑을 UI에서 분리해 API 변경 영향을 한곳에서 관리한다.

import 'dart:async';
import 'dart:convert';
import 'dart:io';

/// API JSON object를 transport 경계에서 일관된 nullable 값 map으로 표현한다.
typedef JsonMap = Map<String, Object?>;

/// GET 요청 구현을 테스트 대역이나 Dart IO transport로 교체하는 계약이다.
typedef GetJsonTransport =
    Future<ApiResponse> Function(
      Uri uri,
      Duration timeout,
      Map<String, String> headers,
    );

/// 인증 header가 필요 없는 JSON POST 요청의 교체 가능한 transport 계약이다.
typedef PostJsonTransport =
    Future<ApiResponse> Function(Uri uri, JsonMap body, Duration timeout);

/// 명시적 header를 포함하는 JSON POST 요청의 교체 가능한 transport 계약이다.
typedef PostJsonWithHeadersTransport =
    Future<ApiResponse> Function(
      Uri uri,
      JsonMap body,
      Duration timeout,
      Map<String, String> headers,
    );

/// 인증 header와 JSON body를 함께 보내는 DELETE transport 계약이다.
typedef DeleteJsonWithHeadersTransport =
    Future<ApiResponse> Function(
      Uri uri,
      JsonMap body,
      Duration timeout,
      Map<String, String> headers,
    );

/// presigned URL에 로컬 파일을 직접 올리는 PUT transport 계약이다.
typedef PutFileTransport =
    Future<ApiResponse> Function(
      Uri uri,
      String filePath,
      String contentType,
      Duration timeout,
      UploadProgressCallback? onProgress,
    );

/// 파일 내용이나 presigned URL을 노출하지 않고 전송된 byte 수만 상위 계층에 알린다.
typedef UploadProgressCallback = void Function(int sentBytes, int totalBytes);

/// 인증 header를 포함하는 부분 수정 요청의 transport 계약이다.
typedef PatchJsonTransport =
    Future<ApiResponse> Function(
      Uri uri,
      JsonMap body,
      Duration timeout,
      Map<String, String> headers,
    );

/// 파일과 field를 함께 보내는 multipart 요청의 교체 가능한 transport 계약이다.
typedef MultipartTransport =
    Future<ApiResponse> Function(
      Uri uri,
      MultipartUpload upload,
      Duration timeout,
    );

/// Api Client 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class ApiClient {
  ApiClient({
    String? baseUrl,
    this.timeout = const Duration(seconds: 8),
    this.uploadTimeout = const Duration(seconds: 60),
    GetJsonTransport? getJsonTransport,
    PostJsonTransport? postJsonTransport,
    PostJsonWithHeadersTransport? postJsonWithHeadersTransport,
    DeleteJsonWithHeadersTransport? deleteJsonWithHeadersTransport,
    PutFileTransport? putFileTransport,
    PatchJsonTransport? patchJsonTransport,
    MultipartTransport? multipartTransport,
  }) : baseUrl = Uri.parse(baseUrl ?? resolveLingKoApiBaseUrl()),
       _getJsonTransport = getJsonTransport ?? _getJsonWithDartIo,
       _postJsonTransport = postJsonTransport ?? _postJsonWithDartIo,
       _postJsonWithHeadersTransport =
           postJsonWithHeadersTransport ?? _postJsonWithHeadersWithDartIo,
       _deleteJsonWithHeadersTransport =
           deleteJsonWithHeadersTransport ?? _deleteJsonWithHeadersWithDartIo,
       _putFileTransport = putFileTransport ?? _putFileWithDartIo,
       _patchJsonTransport = patchJsonTransport ?? _patchJsonWithDartIo,
       _multipartTransport = multipartTransport ?? _postMultipartWithDartIo;

  final Uri baseUrl;
  final Duration timeout;
  final Duration uploadTimeout;
  final GetJsonTransport _getJsonTransport;
  final PostJsonTransport _postJsonTransport;
  final PostJsonWithHeadersTransport _postJsonWithHeadersTransport;
  final DeleteJsonWithHeadersTransport _deleteJsonWithHeadersTransport;
  final PutFileTransport _putFileTransport;
  final PatchJsonTransport _patchJsonTransport;
  final MultipartTransport _multipartTransport;

  Future<JsonMap> getJson(
    String path, [
    Map<String, Object?> query = const {},
    Map<String, String> headers = const {},
  ]) async {
    final uri = _buildUri(path, query);
    final response = await _getJsonTransport(uri, timeout, headers);

    return _decodeResponse(response);
  }

  Future<JsonMap> postJson(String path, JsonMap body) async {
    final response = await _postJsonTransport(
      baseUrl.resolve(path),
      body,
      timeout,
    );
    return _decodeResponse(response);
  }

  Future<JsonMap> postJsonWithHeaders(
    String path,
    JsonMap body,
    Map<String, String> headers,
  ) async {
    final response = await _postJsonWithHeadersTransport(
      baseUrl.resolve(path),
      body,
      timeout,
      headers,
    );
    return _decodeResponse(response);
  }

  /// Presigned PUT을 수행하며 URL이나 파일 내용을 노출하지 않고 전송 byte만 callback으로 알린다.
  Future<void> putFile({
    required String url,
    required String filePath,
    required String contentType,
    UploadProgressCallback? onProgress,
  }) async {
    final response = await _putFileTransport(
      Uri.parse(url),
      filePath,
      contentType,
      uploadTimeout,
      onProgress,
    );
    _throwIfError(response);
  }

  /// 성공 응답에 본문이 없는 계약을 위한 JSON POST를 전송한다.
  ///
  /// 로그아웃의 `204 No Content`가 잘못된 JSON으로 처리되지 않게 하면서
  /// 공통 API 오류 매핑은 그대로 유지한다.
  Future<void> postJsonWithoutResponse(String path, JsonMap body) async {
    final response = await _postJsonTransport(
      baseUrl.resolve(path),
      body,
      timeout,
    );
    _throwIfError(response);
  }

  /// 인증된 DELETE 요청의 JSON 확인 정보를 전송하고 본문 없는 성공 응답을 처리한다.
  Future<void> deleteJsonWithoutResponse(
    String path,
    JsonMap body,
    Map<String, String> headers,
  ) async {
    final response = await _deleteJsonWithHeadersTransport(
      baseUrl.resolve(path),
      body,
      timeout,
      headers,
    );
    _throwIfError(response);
  }

  Future<JsonMap> patchJson(
    String path,
    JsonMap body, [
    Map<String, String> headers = const {},
  ]) async {
    final response = await _patchJsonTransport(
      baseUrl.resolve(path),
      body,
      timeout,
      headers,
    );
    return _decodeResponse(response);
  }

  Future<JsonMap> postMultipart(String path, MultipartUpload upload) async {
    final response = await _multipartTransport(
      baseUrl.resolve(path),
      upload,
      timeout,
    );
    return _decodeResponse(response);
  }

  JsonMap _decodeResponse(ApiResponse response) {
    final decoded = _tryDecodeJson(response.body);

    _throwIfError(response, decoded);

    if (decoded is! Map<String, Object?>) {
      throw const ApiException('Invalid server response');
    }

    return decoded;
  }

  /// 모든 non-2xx 전송 결과를 앱의 안정적인 예외 형식으로 변환한다.
  void _throwIfError(ApiResponse response, [Object? decodedBody]) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return;
    }

    final decoded = decodedBody ?? _tryDecodeJson(response.body);
    final message =
        decoded is Map<String, Object?> && decoded['message'] is String
            ? decoded['message'] as String
            : 'Request failed with status ${response.statusCode}';
    throw ApiException(message, statusCode: response.statusCode);
  }

  Uri _buildUri(String path, Map<String, Object?> query) {
    final uri = baseUrl.resolve(path);
    final queryParameters = {
      ...uri.queryParameters,
      for (final entry in query.entries)
        if (entry.value != null) entry.key: entry.value.toString(),
    };

    if (queryParameters.isEmpty) {
      return uri;
    }

    return uri.replace(queryParameters: queryParameters);
  }

  Object? _tryDecodeJson(String body) {
    if (body.trim().isEmpty) {
      return null;
    }

    try {
      return jsonDecode(body);
    } on FormatException {
      return null;
    }
  }
}

const _productionApiBaseUrl = 'https://lingko-api.duckdns.org';

/// 앱이 사용할 Backend base URL을 결정한다.
///
/// 로컬 개발과 격리 테스트는 `LINGKO_API_BASE_URL`을 명시해 대상을 바꿀 수 있다.
/// 값을 주입하지 않은 Xcode·release 빌드는 실제 iPhone의 localhost로 잘못 연결되지
/// 않도록 운영 HTTPS 주소를 사용한다.
String resolveLingKoApiBaseUrl({
  String environmentOverride = const String.fromEnvironment(
    'LINGKO_API_BASE_URL',
  ),
}) {
  final trimmedOverride = environmentOverride.trim();
  // 테스트나 로컬 Backend를 명시한 빌드는 호출자가 선택한 주소를 가장 먼저 사용한다.
  if (trimmedOverride.isNotEmpty) {
    return trimmedOverride;
  }

  // 기본값은 인증 token을 평문 HTTP로 보내지 않는 운영 HTTPS endpoint로 고정한다.
  return _productionApiBaseUrl;
}

/// Api 응답 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class ApiResponse {
  const ApiResponse({required this.statusCode, required this.body});

  final int statusCode;
  final String body;
}

/// Api 예외 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class ApiException implements Exception {
  const ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

/// Multipart Upload 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class MultipartUpload {
  const MultipartUpload({required this.file, this.fields = const {}});

  final MultipartFileData file;
  final Map<String, String> fields;
}

/// Multipart File Data 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class MultipartFileData {
  const MultipartFileData({
    required this.fieldName,
    required this.path,
    required this.filename,
    required this.contentType,
  });

  final String fieldName;
  final String path;
  final String filename;
  final String contentType;
}

Future<ApiResponse> _getJsonWithDartIo(
  Uri uri,
  Duration timeout,
  Map<String, String> headers,
) async {
  final client = HttpClient();

  try {
    final request = await client.getUrl(uri).timeout(timeout);
    request.headers.set(HttpHeaders.acceptHeader, ContentType.json.mimeType);
    for (final header in headers.entries) {
      request.headers.set(header.key, header.value);
    }

    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();

    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Request timed out');
  } on SocketException {
    throw const ApiException('Cannot connect to LingKo server');
  } finally {
    client.close(force: true);
  }
}

Future<ApiResponse> _postJsonWithDartIo(
  Uri uri,
  JsonMap body,
  Duration timeout,
) async {
  final client = HttpClient();

  try {
    final request = await client.postUrl(uri).timeout(timeout);
    request.headers.contentType = ContentType.json;
    request.write(jsonEncode(body));

    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();

    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Request timed out');
  } on SocketException {
    throw const ApiException('Cannot connect to LingKo server');
  } finally {
    client.close(force: true);
  }
}

Future<ApiResponse> _postJsonWithHeadersWithDartIo(
  Uri uri,
  JsonMap body,
  Duration timeout,
  Map<String, String> headers,
) async {
  final client = HttpClient();

  try {
    final request = await client.postUrl(uri).timeout(timeout);
    request.headers.contentType = ContentType.json;
    for (final header in headers.entries) {
      request.headers.set(header.key, header.value);
    }
    request.write(jsonEncode(body));

    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();
    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Request timed out');
  } on SocketException {
    throw const ApiException('Cannot connect to LingKo server');
  } finally {
    client.close(force: true);
  }
}

Future<ApiResponse> _deleteJsonWithHeadersWithDartIo(
  Uri uri,
  JsonMap body,
  Duration timeout,
  Map<String, String> headers,
) async {
  final client = HttpClient();

  try {
    final request = await client.deleteUrl(uri).timeout(timeout);
    request.headers.contentType = ContentType.json;
    for (final header in headers.entries) {
      request.headers.set(header.key, header.value);
    }
    request.write(jsonEncode(body));

    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();
    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Request timed out');
  } on SocketException {
    throw const ApiException('Cannot connect to LingKo server');
  } finally {
    client.close(force: true);
  }
}

Future<ApiResponse> _putFileWithDartIo(
  Uri uri,
  String filePath,
  String contentType,
  Duration timeout,
  UploadProgressCallback? onProgress,
) async {
  final client = HttpClient();
  final file = File(filePath);

  try {
    final request = await client.putUrl(uri).timeout(timeout);
    request.headers.set(HttpHeaders.contentTypeHeader, contentType);
    final totalBytes = await file.length();
    request.contentLength = totalBytes;
    var sentBytes = 0;
    final measuredStream = file.openRead().map((chunk) {
      sentBytes += chunk.length;
      onProgress?.call(sentBytes, totalBytes);
      return chunk;
    });
    await request.addStream(measuredStream).timeout(timeout);
    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();
    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Audio upload timed out');
  } on SocketException {
    throw const ApiException('Cannot upload audio');
  } finally {
    client.close(force: true);
  }
}

Future<ApiResponse> _patchJsonWithDartIo(
  Uri uri,
  JsonMap body,
  Duration timeout,
  Map<String, String> headers,
) async {
  final client = HttpClient();

  try {
    final request = await client.openUrl('PATCH', uri).timeout(timeout);
    request.headers.contentType = ContentType.json;
    for (final header in headers.entries) {
      request.headers.set(header.key, header.value);
    }
    request.write(jsonEncode(body));

    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();

    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Request timed out');
  } on SocketException {
    throw const ApiException('Cannot connect to LingKo server');
  } finally {
    client.close(force: true);
  }
}

Future<ApiResponse> _postMultipartWithDartIo(
  Uri uri,
  MultipartUpload upload,
  Duration timeout,
) async {
  final client = HttpClient();
  final boundary = 'lingko-${DateTime.now().microsecondsSinceEpoch}';
  final file = File(upload.file.path);

  try {
    final request = await client.postUrl(uri).timeout(timeout);
    request.headers.set(
      HttpHeaders.contentTypeHeader,
      'multipart/form-data; boundary=$boundary',
    );

    for (final entry in upload.fields.entries) {
      request.write('--$boundary\r\n');
      request.write(
        'Content-Disposition: form-data; name="${entry.key}"\r\n\r\n',
      );
      request.write('${entry.value}\r\n');
    }

    request.write('--$boundary\r\n');
    request.write(
      'Content-Disposition: form-data; name="${upload.file.fieldName}"; filename="${upload.file.filename}"\r\n',
    );
    request.write('Content-Type: ${upload.file.contentType}\r\n\r\n');
    await request.addStream(file.openRead()).timeout(timeout);
    request.write('\r\n--$boundary--\r\n');

    final response = await request.close().timeout(timeout);
    final responseBody = await response.transform(utf8.decoder).join();

    return ApiResponse(statusCode: response.statusCode, body: responseBody);
  } on TimeoutException {
    throw const ApiException('Request timed out');
  } on SocketException {
    throw const ApiException('Cannot connect to LingKo server');
  } finally {
    client.close(force: true);
  }
}
