import 'package:flutter/material.dart';

typedef WebUrlOpener = void Function(String url);

class AituanUnsupportedWebApp extends StatelessWidget {
  const AituanUnsupportedWebApp({super.key, required this.openUrl});

  static const apkDownloadPath = '/downloads/aituan-user-server-debug.apk';
  static const landingPath = '/';

  final WebUrlOpener openUrl;

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: '爱团用户端 Web',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: _brand),
      fontFamily: 'Microsoft YaHei',
      useMaterial3: true,
    ),
    home: _UnsupportedWebPage(openUrl: openUrl),
  );
}

class _UnsupportedWebPage extends StatelessWidget {
  const _UnsupportedWebPage({required this.openUrl});

  final WebUrlOpener openUrl;

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final isDesktop = size.width >= 840;
    return Scaffold(
      body: DecoratedBox(
        decoration: const BoxDecoration(color: _paper),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: EdgeInsets.symmetric(
                horizontal: isDesktop ? 44 : 22,
                vertical: isDesktop ? 34 : 22,
              ),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 1120),
                child: isDesktop
                    ? Row(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          Expanded(child: _MessagePanel(openUrl: openUrl)),
                          const SizedBox(width: 42),
                          const Expanded(child: _PreviewPanel()),
                        ],
                      )
                    : Column(
                        children: [
                          _MessagePanel(openUrl: openUrl, compact: true),
                          const SizedBox(height: 22),
                          const _PreviewPanel(compact: true),
                        ],
                      ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _MessagePanel extends StatelessWidget {
  const _MessagePanel({required this.openUrl, this.compact = false});

  final WebUrlOpener openUrl;
  final bool compact;

  @override
  Widget build(BuildContext context) => Container(
    padding: EdgeInsets.all(compact ? 24 : 34),
    decoration: BoxDecoration(
      color: Colors.white.withValues(alpha: 0.84),
      border: Border.all(color: _line),
      borderRadius: BorderRadius.circular(compact ? 28 : 36),
      boxShadow: const [
        BoxShadow(
          color: Color(0x1F2D1D14),
          blurRadius: 48,
          offset: Offset(0, 24),
        ),
      ],
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _BrandHeader(),
        SizedBox(height: compact ? 26 : 34),
        const _StatusPill(),
        const SizedBox(height: 18),
        Text(
          compact ? '请下载 App 使用完整服务' : '暂时不支持电脑端用户服务',
          style: Theme.of(context).textTheme.displaySmall?.copyWith(
            color: _ink,
            fontWeight: FontWeight.w900,
            letterSpacing: -1.6,
            height: 1.08,
          ),
        ),
        const SizedBox(height: 16),
        Text(
          compact
              ? '爱团用户端 Web 入口已上线展示页，但完整点单、下单和个人服务暂未开放浏览器访问。'
              : '当前网页版用户端仍在适配中，电脑浏览器暂不提供完整点单、下单和个人中心服务。请下载 Android APK 使用完整爱团体验。',
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
            color: _muted,
            height: 1.8,
          ),
        ),
        const SizedBox(height: 26),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: [
            _ActionButton(
              label: '下载 Android APK',
              primary: true,
              onTap: () => openUrl(AituanUnsupportedWebApp.apkDownloadPath),
            ),
            _ActionButton(
              label: '返回首页',
              primary: false,
              onTap: () => openUrl(AituanUnsupportedWebApp.landingPath),
            ),
          ],
        ),
        const SizedBox(height: 22),
        const _LinkHint(),
      ],
    ),
  );
}

class _BrandHeader extends StatelessWidget {
  const _BrandHeader();

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Container(
        width: 46,
        height: 46,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: _brand,
          borderRadius: BorderRadius.circular(15),
          boxShadow: const [
            BoxShadow(
              color: Color(0x3DE4002B),
              blurRadius: 24,
              offset: Offset(0, 12),
            ),
          ],
        ),
        child: const Text(
          '爱',
          style: TextStyle(
            color: Colors.white,
            fontSize: 24,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
      const SizedBox(width: 12),
      const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '爱团用户端 Web',
            style: TextStyle(
              color: _ink,
              fontSize: 18,
              fontWeight: FontWeight.w900,
            ),
          ),
          SizedBox(height: 2),
          Text('吃喝玩乐购，一站触达', style: TextStyle(color: _muted)),
        ],
      ),
    ],
  );
}

class _StatusPill extends StatelessWidget {
  const _StatusPill();

  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: _brandSoft,
      borderRadius: BorderRadius.circular(999),
      border: Border.all(color: const Color(0x33E4002B)),
    ),
    child: const Padding(
      padding: EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      child: Text(
        'Web 预览入口 · 用户服务适配中',
        style: TextStyle(color: _brandDark, fontWeight: FontWeight.w800),
      ),
    ),
  );
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.label,
    required this.primary,
    required this.onTap,
  });

  final String label;
  final bool primary;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Material(
    color: primary ? _brand : Colors.white,
    borderRadius: BorderRadius.circular(999),
    child: InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
        decoration: BoxDecoration(
          border: Border.all(color: primary ? _brand : _line),
          borderRadius: BorderRadius.circular(999),
          boxShadow: primary
              ? const [
                  BoxShadow(
                    color: Color(0x33E4002B),
                    blurRadius: 28,
                    offset: Offset(0, 14),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            color: primary ? Colors.white : _ink,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
    ),
  );
}

class _LinkHint extends StatelessWidget {
  const _LinkHint();

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: const Color(0xFFFFF7ED),
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: const Color(0x1FC59B5C)),
    ),
    child: const Text(
      '备用下载地址：/downloads/aituan-user-server-debug.apk',
      style: TextStyle(color: _muted, fontWeight: FontWeight.w700),
    ),
  );
}

class _PreviewPanel extends StatelessWidget {
  const _PreviewPanel({this.compact = false});

  final bool compact;

  @override
  Widget build(BuildContext context) => Stack(
    alignment: Alignment.center,
    children: [
      Container(
        height: compact ? 280 : 460,
        decoration: BoxDecoration(
          color: _cream,
          borderRadius: BorderRadius.circular(compact ? 32 : 44),
          border: Border.all(color: _line),
        ),
      ),
      Transform.rotate(
        angle: compact ? 0 : -0.08,
        child: Container(
          width: compact ? 210 : 270,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: _ink,
            borderRadius: BorderRadius.circular(34),
            boxShadow: const [
              BoxShadow(
                color: Color(0x332D1D14),
                blurRadius: 42,
                offset: Offset(0, 22),
              ),
            ],
          ),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(26),
            ),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '爱团 App',
                  style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900),
                ),
                SizedBox(height: 6),
                Text('移动端完整服务', style: TextStyle(color: _muted)),
                SizedBox(height: 18),
                _PreviewRow(icon: '🥡', title: '外卖点单', text: '起送、配送、下单闭环'),
                _PreviewRow(icon: '🎟️', title: '团购券码', text: '预约、核销、售后管理'),
                _PreviewRow(icon: '⭐', title: '真实评价', text: '评价反馈与客服投诉'),
              ],
            ),
          ),
        ),
      ),
    ],
  );
}

class _PreviewRow extends StatelessWidget {
  const _PreviewRow({required this.icon, required this.title, required this.text});

  final String icon;
  final String title;
  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Row(
      children: [
        Container(
          width: 42,
          height: 42,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: _brandSoft,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Text(icon, style: const TextStyle(fontSize: 21)),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontWeight: FontWeight.w900)),
              Text(text, style: const TextStyle(color: _muted, fontSize: 12)),
            ],
          ),
        ),
      ],
    ),
  );
}

const _brand = Color(0xFFE4002B);
const _brandDark = Color(0xFF9F001E);
const _brandSoft = Color(0xFFFFF0F2);
const _ink = Color(0xFF17110F);
const _muted = Color(0xFF6F635F);
const _paper = Color(0xFFFFFAF2);
const _cream = Color(0xFFF5E7D3);
const _line = Color(0x1F291C18);
