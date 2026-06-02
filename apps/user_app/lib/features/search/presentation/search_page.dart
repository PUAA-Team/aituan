import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_search_box.dart';
import '../../../core/widgets/section_header.dart';
import '../../location/presentation/location_picker_button.dart';
import 'search_suggestion_widgets.dart';

class SearchPage extends StatefulWidget {
  const SearchPage({super.key});

  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final _controller = TextEditingController();

  bool get _hasQuery => _controller.text.trim().isNotEmpty;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onInputChanged);
  }

  @override
  void dispose() {
    _controller
      ..removeListener(_onInputChanged)
      ..dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: AppSearchBox(
        controller: _controller,
        autofocus: true,
        hint: '汉堡 / 洗脚 / 双人套餐',
        onSubmitted: _goResult,
        fillColor: AppColors.soft,
      ),
      actions: [
        Padding(
          padding: const EdgeInsets.only(right: 8),
          child: TextButton(
            style: TextButton.styleFrom(
              foregroundColor: _hasQuery ? Colors.white : AppColors.textSub,
              backgroundColor: _hasQuery ? AppColors.brand : Colors.transparent,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(6),
              ),
            ),
            onPressed: _hasQuery
                ? () => _goResult(_controller.text)
                : () => Navigator.pop(context),
            child: Text(_hasQuery ? '搜索' : '取消'),
          ),
        ),
      ],
    ),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppCard(
          child: Row(
            children: [
              const Expanded(child: Text('搜索位置')),
              LocationPickerButton(compact: true),
            ],
          ),
        ),
        SearchChipPanel(
          title: '历史搜索',
          chips: const ['鸡排饭', '洗脚', '电影票', '汉堡'],
          onTap: _goResult,
        ),
        const SectionHeader(title: '搜索排行榜', action: '实时'),
        AppCard(
          child: Column(
            children: [
              SearchRankItem(
                index: 1,
                title: '晚餐外卖热销榜',
                desc: '汉堡、麻辣烫、鸡排饭搜索上涨',
                onTap: () => _goResult('汉堡'),
              ),
              SearchRankItem(
                index: 2,
                title: '周末团购口碑榜',
                desc: '火锅、烤肉、双人套餐热度高',
                onTap: () => _goResult('双人套餐'),
              ),
              SearchRankItem(
                index: 3,
                title: '放松休闲榜',
                desc: '洗脚、电影票、咖啡轻食',
                onTap: () => _goResult('洗脚'),
              ),
            ],
          ),
        ),
        SearchChipPanel(
          title: '发现',
          chips: const ['今晚吃什么', '双人团购', '临时放松', '附近热门'],
          onTap: _goResult,
        ),
      ],
    ),
  );

  void _onInputChanged() => setState(() {});

  void _goResult(String keyword) {
    final query = keyword.trim();
    if (query.isEmpty) return;
    Navigator.pushNamed(
      context,
      Routes.searchResult,
      arguments: SearchArgs(query),
    );
  }
}
