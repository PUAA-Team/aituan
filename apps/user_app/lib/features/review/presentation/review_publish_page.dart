import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';

class ReviewPublishPage extends StatefulWidget {
  const ReviewPublishPage({super.key});

  @override
  State<ReviewPublishPage> createState() => _ReviewPublishPageState();
}

class _ReviewPublishPageState extends State<ReviewPublishPage> {
  int _score = 5;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('发布评价')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('江南小馆双人套餐', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 4),
                Text(
                  '订单已完成，发布评价后可帮助更多同学选择',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('本次体验评分', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                Row(
                  children: [
                    for (var i = 1; i <= 5; i += 1)
                      IconButton(
                        onPressed: () => setState(() => _score = i),
                        icon: Icon(
                          i <= _score ? Icons.star : Icons.star_border,
                          color: AppColors.brand,
                          size: 30,
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
          const AppCard(
            child: TextField(
              maxLines: 5,
              decoration: InputDecoration(hintText: '分享本次体验，帮助更多同学选择'),
            ),
          ),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: '提交评价',
        onPrimary: () => Navigator.pop(context),
      ),
    );
  }
}
