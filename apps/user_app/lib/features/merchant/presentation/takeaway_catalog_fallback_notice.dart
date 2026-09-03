import 'package:flutter/material.dart';

class TakeawayCatalogFallbackNotice extends StatelessWidget {
  const TakeawayCatalogFallbackNotice({
    super.key,
    this.notice,
    this.onRefresh,
    this.compact = false,
  });

  static const fallbackNotice = '商品服务暂不可用，已显示最近一次购物车快照；暂不可新增商品或修改数量，仍可移除或清空。';

  final String? notice;
  final VoidCallback? onRefresh;
  final bool compact;

  @override
  Widget build(BuildContext context) => Container(
    key: const Key('catalog-fallback-notice'),
    width: double.infinity,
    margin: EdgeInsets.only(bottom: compact ? 10 : 12),
    padding: EdgeInsets.all(compact ? 10 : 12),
    decoration: BoxDecoration(
      color: const Color(0xFFFFF8E1),
      borderRadius: BorderRadius.circular(10),
      border: Border.all(color: const Color(0xFFFFC64B)),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Icon(Icons.warning_amber_rounded, color: Color(0xFFB26A00)),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '商品服务异常 · 已启用备用结果',
                style: TextStyle(
                  color: Color(0xFF7A4700),
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                notice?.trim().isNotEmpty == true
                    ? notice!.trim()
                    : fallbackNotice,
                style: const TextStyle(color: Color(0xFF7A4700), height: 1.35),
              ),
              if (!compact) ...[
                const SizedBox(height: 5),
                const Text(
                  '故障已隔离，其他业务仍可正常访问。',
                  style: TextStyle(
                    color: Color(0xFF7A4700),
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ],
          ),
        ),
        if (onRefresh != null)
          TextButton(onPressed: onRefresh, child: const Text('重新检测')),
      ],
    ),
  );
}
