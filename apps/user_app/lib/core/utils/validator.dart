class Validator {
  const Validator._();

  static bool isPhone(String value) => RegExp(r'^\d{11}$').hasMatch(value);

  static bool isEmail(String value) {
    return RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(value);
  }

  static bool isAccount(String value) => isPhone(value) || isEmail(value);
}
