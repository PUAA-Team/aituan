import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../home/data/backend_app_repository.dart';

/// Stage5-D 新增：预约详情页
/// 用于酒店、休闲娱乐、丽人医美、洗脚按摩等需要到店时间的非外卖订单
class BookingDetailPage extends StatefulWidget {
  const BookingDetailPage({super.key, required this.args});

  final BookingDetailArgs args;

  @override
  State<BookingDetailPage> createState() => _BookingDetailPageState();
}

class _BookingDetailPageState extends State<BookingDetailPage> {
  BookingData? _booking;
  OrderDetailData? _orderDetail;
  Object? _error;
  bool _loading = true;
  bool _saving = false;

  final _contactNameCtrl = TextEditingController();
  final _contactPhoneCtrl = TextEditingController();
  final _bookingDateCtrl = TextEditingController();
  final _bookingTimeSlotCtrl = TextEditingController();
  final _remarkCtrl = TextEditingController();
  int _guestCount = 1;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _contactNameCtrl.dispose();
    _contactPhoneCtrl.dispose();
    _bookingDateCtrl.dispose();
    _bookingTimeSlotCtrl.dispose();
    _remarkCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final detail = await backendRepository.fetchOrderDetail(widget.args.orderId);
      final booking = detail.booking ?? await backendRepository.fetchBooking(widget.args.orderId);
      if (!mounted) return;
      setState(() {
        _orderDetail = detail;
        _booking = booking;
        _loading = false;
        _bindControllers();
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }

  void _bindControllers() {
    final booking = _booking;
    if (booking == null) {
      _contactNameCtrl.text = '';
      _contactPhoneCtrl.text = '';
      _bookingDateCtrl.text = '';
      _bookingTimeSlotCtrl.text = '';
      _remarkCtrl.text = '';
      _guestCount = 1;
      return;
    }
    _contactNameCtrl.text = booking.contactName ?? '';
    _contactPhoneCtrl.text = booking.contactPhone ?? '';
    _bookingDateCtrl.text = booking.bookingDate ?? '';
    _bookingTimeSlotCtrl.text = booking.bookingTimeSlot ?? '';
    _remarkCtrl.text = booking.storeConfirmRemark ?? '';
    _guestCount = booking.guestCount;
  }

  Future<void> _submit() async {
    if (_contactNameCtrl.text.trim().isEmpty || _contactPhoneCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请填写联系人和手机号')),
      );
      return;
    }
    try {
      setState(() => _saving = true);
      final booking = await backendRepository.upsertBooking(
        orderId: widget.args.orderId,
        contactName: _contactNameCtrl.text.trim(),
        contactPhone: _contactPhoneCtrl.text.trim(),
        bookingDate: _bookingDateCtrl.text.trim().isEmpty
            ? null
            : _bookingDateCtrl.text.trim(),
        bookingTimeSlot: _bookingTimeSlotCtrl.text.trim().isEmpty
            ? null
            : _bookingTimeSlotCtrl.text.trim(),
        guestCount: _guestCount,
        remark: _remarkCtrl.text.trim().isEmpty ? null : _remarkCtrl.text.trim(),
      );
      if (!mounted) return;
      setState(() {
        _booking = booking;
        _saving = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('预约信息已提交，等待商家确认')),
      );
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('保存失败：$error')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('预约详情')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              AppCard(child: Text('加载失败：$_error'))
            else ...[
              _StatusCard(booking: _booking, detail: _orderDetail),
              _FormCard(
                booking: _booking,
                contactNameCtrl: _contactNameCtrl,
                contactPhoneCtrl: _contactPhoneCtrl,
                bookingDateCtrl: _bookingDateCtrl,
                bookingTimeSlotCtrl: _bookingTimeSlotCtrl,
                remarkCtrl: _remarkCtrl,
                guestCount: _guestCount,
                onGuestCountChange: (value) =>
                    setState(() => _guestCount = value),
              ),
              AppCard(
                child: SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: FilledButton(
                    onPressed: _saving ? null : _submit,
                    child: Text(_saving ? '提交中…' : '提交预约信息'),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.booking, required this.detail});

  final BookingData? booking;
  final OrderDetailData? detail;

  @override
  Widget build(BuildContext context) {
    final isConfirmed = booking?.isConfirmed ?? false;
    final label = booking == null
        ? '未提交预约'
        : (isConfirmed ? '商家已确认预约' : '等待商家确认');
    return AppCard(
      backgroundColor: AppColors.brandSoft,
      borderColor: AppColors.brandLine,
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 6),
                Text(
                  detail?.storeName ?? '',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                if (booking?.storeConfirmRemark != null &&
                    booking!.storeConfirmRemark!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    '商家备注：${booking!.storeConfirmRemark!}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ],
            ),
          ),
          BrandTag(
            isConfirmed ? '已确认' : '处理中',
            green: isConfirmed,
            selected: true,
          ),
        ],
      ),
    );
  }
}

class _FormCard extends StatelessWidget {
  const _FormCard({
    required this.booking,
    required this.contactNameCtrl,
    required this.contactPhoneCtrl,
    required this.bookingDateCtrl,
    required this.bookingTimeSlotCtrl,
    required this.remarkCtrl,
    required this.guestCount,
    required this.onGuestCountChange,
  });

  final BookingData? booking;
  final TextEditingController contactNameCtrl;
  final TextEditingController contactPhoneCtrl;
  final TextEditingController bookingDateCtrl;
  final TextEditingController bookingTimeSlotCtrl;
  final TextEditingController remarkCtrl;
  final int guestCount;
  final ValueChanged<int> onGuestCountChange;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('预约信息', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          TextField(
            controller: contactNameCtrl,
            decoration: const InputDecoration(labelText: '联系人姓名'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: contactPhoneCtrl,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(labelText: '联系手机号'),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: bookingDateCtrl,
            decoration: const InputDecoration(
              labelText: '预约日期 (YYYY-MM-DD)',
              hintText: '例如 2026-06-01',
            ),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: bookingTimeSlotCtrl,
            decoration: const InputDecoration(
              labelText: '预约时段',
              hintText: '例如 19:00-20:00 / 影音房 / 第 8 场',
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Text('人数 '),
              IconButton(
                icon: const Icon(Icons.remove_circle_outline),
                onPressed: guestCount > 1
                    ? () => onGuestCountChange(guestCount - 1)
                    : null,
              ),
              Text('$guestCount', style: const TextStyle(fontSize: 18)),
              IconButton(
                icon: const Icon(Icons.add_circle_outline),
                onPressed: guestCount < 20
                    ? () => onGuestCountChange(guestCount + 1)
                    : null,
              ),
            ],
          ),
          TextField(
            controller: remarkCtrl,
            maxLines: 2,
            decoration: const InputDecoration(
              labelText: '备注',
              hintText: '需要靠窗 / 不吃辣 / 增加床位等',
            ),
          ),
        ],
      ),
    );
  }
}
