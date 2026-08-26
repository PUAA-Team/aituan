import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';
import '../../../core/network/json_codec.dart';

final memberRepository = MemberRepository();

// 会员中心数据访问：消费 /api/app/account/member/info
class MemberRepository {
  MemberRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  Future<MemberInfo> fetchMemberInfo() async {
    final json = await _client.get('/api/app/account/member/info');
    return MemberInfo.fromApi(jsonMap(json['data']));
  }
}

// 我的会员信息（含成长进度与权益）
class MemberInfo {
  const MemberInfo({
    required this.currentLevelCode,
    required this.currentLevelName,
    required this.currentColor,
    required this.growthValue,
    required this.nextLevelName,
    required this.nextLevelMinGrowth,
    required this.growthToNextLevel,
    required this.progressPercent,
    required this.benefits,
  });

  final String currentLevelCode;
  final String currentLevelName;
  final String? currentColor;
  final int growthValue;
  final String? nextLevelName; // 为空表示已是最高等级
  final int? nextLevelMinGrowth;
  final int? growthToNextLevel;
  final int progressPercent;
  final List<MemberBenefit> benefits;

  bool get isTopLevel => nextLevelName == null;

  factory MemberInfo.fromApi(Map<String, dynamic> json) => MemberInfo(
    currentLevelCode: jsonString(json['currentLevelCode']),
    currentLevelName: jsonString(json['currentLevelName'], fallback: '普通会员'),
    currentColor: jsonStringOrNull(json['currentColor']),
    growthValue: jsonInt(json['growthValue']),
    nextLevelName: jsonStringOrNull(json['nextLevelName']),
    nextLevelMinGrowth: jsonIntOrNull(json['nextLevelMinGrowth']),
    growthToNextLevel: jsonIntOrNull(json['growthToNextLevel']),
    progressPercent: jsonInt(json['progressPercent']),
    benefits: jsonList(
      json['benefits'],
    ).map((entry) => MemberBenefit.fromApi(jsonMap(entry))).toList(),
  );
}

// 会员权益条目
class MemberBenefit {
  const MemberBenefit({required this.title, required this.desc});

  final String title;
  final String desc;

  factory MemberBenefit.fromApi(Map<String, dynamic> json) => MemberBenefit(
    title: jsonString(json['title']),
    desc: jsonString(json['desc']),
  );
}
