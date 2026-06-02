const takeawayMoneyEpsilon = 0.009;

double takeawayStartMissing(double itemAmount, double startPrice) {
  final missing = startPrice - itemAmount;
  return missing > takeawayMoneyEpsilon ? missing : 0;
}

bool takeawayStartMet(double itemAmount, double startPrice) =>
    takeawayStartMissing(itemAmount, startPrice) <= 0;

String takeawayMoneyText(double value) {
  if (value <= takeawayMoneyEpsilon) return '0';
  final rounded = (value * 100).ceilToDouble() / 100;
  if ((rounded - rounded.roundToDouble()).abs() < 0.001) {
    return rounded.toStringAsFixed(0);
  }
  if ((rounded * 10 - (rounded * 10).roundToDouble()).abs() < 0.001) {
    return rounded.toStringAsFixed(1);
  }
  return rounded.toStringAsFixed(2);
}
