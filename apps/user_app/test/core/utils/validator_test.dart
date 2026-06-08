import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/utils/validator.dart';

void main() {
  group('Validator', () {
    test('isPhone 仅接受 11 位数字', () {
      expect(Validator.isPhone('18800001111'), isTrue);
      expect(Validator.isPhone('1880000111'), isFalse);
      expect(Validator.isPhone('18800001111a'), isFalse);
      expect(Validator.isPhone(' 18800001111'), isFalse);
    });

    test('isEmail 校验基础邮箱格式', () {
      expect(Validator.isEmail('user@example.com'), isTrue);
      expect(Validator.isEmail('user.name+tag@example.co'), isTrue);
      expect(Validator.isEmail('userexample.com'), isFalse);
      expect(Validator.isEmail('user@'), isFalse);
      expect(Validator.isEmail('user@example'), isFalse);
      expect(Validator.isEmail('user @example.com'), isFalse);
    });

    test('isAccount 支持手机号或邮箱', () {
      expect(Validator.isAccount('18800001111'), isTrue);
      expect(Validator.isAccount('user@example.com'), isTrue);
      expect(Validator.isAccount('demo_user'), isFalse);
    });
  });
}
