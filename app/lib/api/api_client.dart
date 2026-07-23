import 'dart:async';
import 'dart:convert';
import 'dart:io';

typedef JsonMap = Map<String, Object?>;
typedef GetJsonTransport =
    Future<ApiResponse> Function(
      Uri uri,
      Duration timeout,
      Map<String, String> headers,
    );
typedef PostJsonTransport =
    Future<ApiResponse> Function(Uri uri, JsonMap body, Duration timeout);
typedef PatchJsonTransport =
    Future<ApiResponse> Function(
      Uri uri,
      JsonMap body,
      Duration timeout,
      Map<String, String> headers,
    );
typedef MultipartTransport =
    Future<ApiResponse> Function(
      Uri uri,
      MultipartUpload upload,
      Duration timeout,
    );

class ApiClient {
  ApiClient({
    String? baseUrl,
    this.timeout = const Duration(seconds: 8),
    GetJsonTransport? getJsonTransport,
    PostJsonTransport? postJsonTransport,
    PatchJsonTransport? patchJsonTransport,
    MultipartTransport? multipartTransport,
  }) : baseUrl = Uri.parse(baseUrl ?? resolveLingKoApiBaseUrl()),
       _getJsonTransport = getJsonTransport ?? _getJsonWithDartIo,
       _postJsonTransport = postJsonTransport ?? _postJsonWithDartIo,
       _patchJsonTransport = patchJsonTransport ?? _patchJsonWithDartIo,
       _multipartTransport = multipartTransport ?? _postMultipartWithDartIo;

  final Uri baseUrl;
  final Duration timeout;
  final GetJsonTransport _getJsonTransport;
  final PostJsonTransport _postJsonTransport;
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

  Future<void> postJsonWithoutResponse(String path, JsonMap body) async {
    final response = await _postJsonTransport(
      baseUrl.resolve(path),
      body,
      timeout,
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

String resolveLingKoApiBaseUrl({
  String environmentOverride = const String.fromEnvironment(
    'LINGKO_API_BASE_URL',
  ),
  bool? isAndroid,
}) {
  final trimmedOverride = environmentOverride.trim();
  if (trimmedOverride.isNotEmpty) {
    return trimmedOverride;
  }

  return (isAndroid ?? Platform.isAndroid)
      ? 'http://10.0.2.2:8080'
      : 'http://localhost:8080';
}

class ApiResponse {
  const ApiResponse({required this.statusCode, required this.body});

  final int statusCode;
  final String body;
}

class ApiException implements Exception {
  const ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

class MultipartUpload {
  const MultipartUpload({required this.file, this.fields = const {}});

  final MultipartFileData file;
  final Map<String, String> fields;
}

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
