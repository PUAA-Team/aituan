import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/section_header.dart';
import '../data/member_repository.dart';
import 'widgets/member_benefit_tile.dart';

class MemberCenterPage extends StatefulWidget {
  const MemberCenterPage({super.key});

  @override
  State<MemberCenterPage> createState() => _MemberCenterPageState();
}

class _MemberCenterPageState extends State<MemberCenterPage> {
  bool _loading = true;
  Object? _error;
  MemberInfo? _info;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('会员中心')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_info != null) ...[
            _LevelHeader(info: _info!),
            _GrowthCard(info: _info!),
            const SectionHeader(title: '会员权益'),
            const SizedBox(height: 8),
            if (_info!.benefits.isEmpty)
              const AppCard(child: Text('暂无权益说明'))
            else
              for (final benefit in _info!.benefits)
                MemberBenefitTile(benefit: benefit),
            const SectionHeader(title: '成长规则'),
            const SizedBox(height: 8),
            const _RuleCard(),
          ],
        ],
      ),
    ),
  );

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final info = await memberRepository.fetchMemberInfo();
      if (!mounted) return;
      setState(() {
        _info = info;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }
}

// 把后端等级色值(#RRGGBB)转为 Color，解析失败回退品牌色
Color _levelColor(String? hex) {
  if (hex == null) return AppColors.brand;
  var value = hex.replaceAll('#', '').trim();
  if (value.length == 6) value = 'FF$value';
  final parsed = int.tryParse(value, radix: 16);
  return parsed == null ? AppColors.brand : Color(parsed);
}

class _LevelHeader extends StatelessWidget {
  const _LevelHeader({required this.info});

  final MemberInfo info;

  @override
  Widget build(BuildContext context) {
    final color = _levelColor(info.currentColor);
    return AppCard(
      backgroundColor: color,
      borderColor: color,
      child: Row(
        children: [
          const Icon(Icons.workspace_premium, color: Colors.white, size: 40),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  info.currentLevelName,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '当前成长值 ${info.growthValue}',
                  style: const TextStyle(color: Colors.white70, fontSize: 13),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _GrowthCard extends StatelessWidget {
  const _GrowthCard({required this.info});

  final MemberInfo info;

  @override
  Widget build(BuildContext context) {
    if (info.isTopLevel) {
      return AppCard(
        child: Row(
          children: [
            const Icon(Icons.emoji_events, color: AppColors.brand),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                '已是最高等级 ${info.currentLevelName}',
                style: Theme.of(context).textTheme.titleSmall,
              ),
            ),
          ],
        ),
      );
    }
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '距 ${info.nextLevelName} 还需 ${info.growthToNextLevel} 成长值',
            style: Theme.of(context).textTheme.titleSmall,
          ),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: (info.progressPercent / 100).clamp(0.0, 1.0),
              minHeight: 10,
              backgroundColor: AppColors.soft,
              color: AppColors.brand,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            '${info.progressPercent}%',
            style: Theme.of(context).textTheme.labelSmall,
          ),
        ],
      ),
    );
  }
}

class _RuleCard extends StatelessWidget {
  const _RuleCard();

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: const [
        _RuleLine('消费成长值', '订单完成后按实付金额累计，1 元获得 1 成长值'),
        Divider(),
        _RuleLine('评价成长值', '首次发表订单评价获得 3 成长值'),
        Divider(),
        _RuleLine('等级门槛', '白银0、黄金300、白金1000、钻石3000、红钻10000、黑钻30000'),
        Divider(),
        _RuleLine('每周会员券', '每周按当前等级自动刷新，有效期 7 天'),
      ],
    ),
  );
}

class _RuleLine extends StatelessWidget {
  const _RuleLine(this.title, this.desc);

  final String title;
  final String desc;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 86,
          child: Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
        ),
        const SizedBox(width: 10),
        Expanded(child: Text(desc, style: Theme.of(context).textTheme.bodySmall)),
      ],
    ),
  );
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('会员信息加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
