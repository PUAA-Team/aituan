class AppBuildInfo {
  const AppBuildInfo._();

  static const versionName = '1.1.7';
  static const buildNumber = '24';
  static const fullVersion = '$versionName+$buildNumber';
  static const buildCommit = String.fromEnvironment(
    'AITUAN_BUILD_COMMIT',
    defaultValue: 'local',
  );
  static const buildSource = String.fromEnvironment(
    'AITUAN_BUILD_SOURCE',
    defaultValue: 'local',
  );

  static String get shortCommit {
    if (buildCommit.length <= 8) return buildCommit;
    return buildCommit.substring(0, 8);
  }
}
