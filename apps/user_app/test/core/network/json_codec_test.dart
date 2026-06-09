import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/json_codec.dart';

void main() {
  group('jsonStringList', () {
    test('支持列表和逗号字符串', () {
      expect(jsonStringList(['外卖', 123, '']), ['外卖', '123']);
      expect(jsonStringList('外卖, 团购,,酒店'), ['外卖', '团购', '酒店']);
      expect(jsonStringList(null), isEmpty);
    });
  });

  group('json weak type helpers', () {
    test('字符串、数字、布尔值提供稳定 fallback', () {
      expect(jsonString(' 爱团 '), '爱团');
      expect(jsonString('', fallback: '默认'), '默认');
      expect(jsonStringOrNull('  '), isNull);
      expect(jsonInt('42'), 42);
      expect(jsonInt('bad'), 0);
      expect(jsonIntOrNull(null), isNull);
      expect(jsonDouble('12.5'), 12.5);
      expect(jsonDouble('bad'), 0);
      expect(jsonDoubleOrNull(null), isNull);
      expect(jsonBool(true), isTrue);
      expect(jsonBool('true'), isTrue);
      expect(jsonBool('false'), isFalse);
    });

    test('map/list/date 解析异常输入时不抛错', () {
      expect(jsonMap({'id': 1})['id'], 1);
      expect(jsonMap('bad'), isEmpty);
      expect(jsonList([1, 2]), [1, 2]);
      expect(jsonList('bad'), isEmpty);
      expect(jsonDateTime('2026-06-08T12:30:00'), isNotNull);
      expect(jsonDateTime('bad'), isNull);
    });
  });
}
