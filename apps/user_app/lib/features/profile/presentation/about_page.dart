import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';

class AboutPage extends StatelessWidget {
  const AboutPage({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('关于爱团')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppCard(
          backgroundColor: AppColors.brandSoft,
          borderColor: AppColors.brandLine,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 58,
                    height: 58,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: AppColors.brand,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: const Text(
                      '爱',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 26,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('爱团', style: Theme.of(context).textTheme.headlineSmall),
                        const SizedBox(height: 4),
                        Text(
                          '本地生活一站式服务平台',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  const Text('v1.0.9'),
                ],
              ),
              const SizedBox(height: 16),
              Text(
                '爱团围绕外卖、团购、酒店、电影、休闲娱乐、丽人医美、景点门票和到店服务，提供发现、下单、履约、评价和售后的一体化体验。',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
        const _InfoCard(
          title: '服务理念',
          content: '用稳定清晰的流程连接用户、商户和平台运营，让本地生活服务更可靠、更高效。',
        ),
        const _InfoCard(
          title: '核心能力',
          content: '商户与商品发现、外卖配送履约、到店券码核销、消息通知、收藏地址和评价反馈。',
        ),
        AppCard(
          child: Column(
            children: const [
              _LinkRow(title: '用户协议'),
              Divider(),
              _LinkRow(title: '隐私政策'),
              Divider(),
              _LinkRow(title: '平台资质与客服'),
            ],
          ),
        ),
      ],
    ),
  );
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.title, required this.content});

  final String title;
  final String content;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        Text(content, style: Theme.of(context).textTheme.bodyMedium),
      ],
    ),
  );
}

class _LinkRow extends StatelessWidget {
  const _LinkRow({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    visualDensity: VisualDensity.compact,
    title: Text(title, style: Theme.of(context).textTheme.titleSmall),
    trailing: const Icon(Icons.chevron_right, color: AppColors.textLight),
  );
}
