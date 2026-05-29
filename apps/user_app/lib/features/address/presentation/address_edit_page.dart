import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../shared/models/address_model.dart';
import '../../home/data/backend_app_repository.dart';
import '../../location/application/location_scope.dart';
import '../../location/application/location_service.dart';

class AddressEditPage extends StatefulWidget {
  const AddressEditPage({super.key, required this.args});

  final AddressEditArgs args;

  @override
  State<AddressEditPage> createState() => _AddressEditPageState();
}

class _AddressEditPageState extends State<AddressEditPage> {
  final _contactName = TextEditingController();
  final _contactPhone = TextEditingController();
  final _province = TextEditingController(text: '北京市');
  final _city = TextEditingController(text: '北京市');
  final _district = TextEditingController(text: '海淀区');
  final _detailAddress = TextEditingController();
  final _tagName = TextEditingController(text: '家');
  final _deliveryNote = TextEditingController();
  double? _longitude;
  double? _latitude;
  bool _isDefault = false;
  bool _saving = false;
  bool _locating = false;
  bool _syncingLocationFields = false;

  AddressData? get _address => widget.args.address;

  @override
  void initState() {
    super.initState();
    for (final controller in [_province, _city, _district, _detailAddress]) {
      controller.addListener(_clearLocationOnManualEdit);
    }
    final address = _address;
    if (address != null) {
      _contactName.text = address.contactName;
      _contactPhone.text = address.contactPhone;
      _province.text = address.province;
      _city.text = address.city;
      _district.text = address.district;
      _detailAddress.text = address.detailAddress;
      _longitude = address.longitude;
      _latitude = address.latitude;
      _tagName.text = address.tagName;
      _deliveryNote.text = address.deliveryNote;
      _isDefault = address.isDefault;
    }
  }

  @override
  void dispose() {
    for (final controller in [_province, _city, _district, _detailAddress]) {
      controller.removeListener(_clearLocationOnManualEdit);
    }
    _contactName.dispose();
    _contactPhone.dispose();
    _province.dispose();
    _city.dispose();
    _district.dispose();
    _detailAddress.dispose();
    _tagName.dispose();
    _deliveryNote.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(_address == null ? '新增地址' : '编辑地址')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppCard(
          child: Column(
            children: [
              _Field(controller: _contactName, label: '收货人'),
              _Field(
                controller: _contactPhone,
                label: '手机号',
                keyboardType: TextInputType.phone,
              ),
              Row(
                children: [
                  Expanded(
                    child: _Field(controller: _province, label: '省份'),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _Field(controller: _city, label: '城市'),
                  ),
                ],
              ),
              _Field(controller: _district, label: '区县'),
              _Field(controller: _detailAddress, label: '详细地址', maxLines: 2),
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(
                  _locationHint,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: _longitude == null || _latitude == null ? AppColors.textSub : AppColors.brand,
                  ),
                ),
              ),
              Align(
                alignment: Alignment.centerLeft,
                child: OutlinedButton.icon(
                  onPressed: _saving || _locating ? null : _useCurrentLocation,
                  icon: _locating
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.my_location_outlined),
                  label: Text(_locating ? '定位中' : '使用当前位置'),
                ),
              ),
              _Field(controller: _tagName, label: '标签'),
              _Field(controller: _deliveryNote, label: '配送备注', maxLines: 2),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                value: _isDefault,
                activeThumbColor: AppColors.brand,
                title: const Text('设为默认地址'),
                onChanged: (value) => setState(() => _isDefault = value),
              ),
            ],
          ),
        ),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: Text(_saving ? '保存中' : '保存地址'),
        ),
        if (_address != null) ...[
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: _saving ? null : _delete,
            child: const Text('删除地址'),
          ),
        ],
      ],
    ),
  );

  Future<void> _useCurrentLocation() async {
    final location = LocationScope.of(context);
    try {
      setState(() => _locating = true);
      if (!location.hasLocation || location.current?.addressText.isEmpty == true) {
        await location.refresh();
      }
      final current = location.current;
      if (current == null) {
        throw const LocationException('未获取到当前位置');
      }
      setState(() {
        _syncingLocationFields = true;
        if (current.province.isNotEmpty) _province.text = current.province;
        if (current.city.isNotEmpty) _city.text = current.city;
        if (current.district.isNotEmpty) _district.text = current.district;
        final detail = current.addressText;
        if (detail.isNotEmpty) _detailAddress.text = detail;
        _longitude = current.longitude;
        _latitude = current.latitude;
        _locating = false;
        _syncingLocationFields = false;
      });
      if (!mounted) return;
      showAppSnackBar(context, '已填入当前位置');
    } catch (error) {
      if (!mounted) return;
      setState(() => _locating = false);
      showAppSnackBar(context, '定位失败：$error');
    }
  }

  Future<void> _save() async {
    final form = _formData();
    if (form == null) return;
    try {
      setState(() => _saving = true);
      final address = _address;
      if (address == null) {
        await backendRepository.createAddress(form);
      } else {
        await backendRepository.updateAddress(address.id, form);
      }
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showAppSnackBar(context, '保存失败：$error');
    }
  }

  Future<void> _delete() async {
    final address = _address;
    if (address == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('删除地址'),
        content: const Text('删除后外卖结算将不能继续使用该地址。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      setState(() => _saving = true);
      await backendRepository.deleteAddress(address.id);
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showAppSnackBar(context, '删除失败：$error');
    }
  }

  String get _locationHint {
    if (_longitude != null && _latitude != null) {
      return '已保存定位坐标：${_longitude!.toStringAsFixed(6)}, ${_latitude!.toStringAsFixed(6)}';
    }
    return '手动输入地址保存后，服务器会自动解析配送坐标';
  }

  void _clearLocationOnManualEdit() {
    if (_syncingLocationFields || _longitude == null || _latitude == null) return;
    setState(() {
      _longitude = null;
      _latitude = null;
    });
  }

  AddressFormData? _formData() {
    final values = [
      _contactName.text,
      _contactPhone.text,
      _province.text,
      _city.text,
      _district.text,
      _detailAddress.text,
    ];
    if (values.any((value) => value.trim().isEmpty)) {
      showAppSnackBar(context, '请填写收货人、手机号和完整地址');
      return null;
    }
    return AddressFormData(
      contactName: _contactName.text.trim(),
      contactPhone: _contactPhone.text.trim(),
      province: _province.text.trim(),
      city: _city.text.trim(),
      district: _district.text.trim(),
      detailAddress: _detailAddress.text.trim(),
      longitude: _longitude,
      latitude: _latitude,
      tagName: _tagName.text.trim().isEmpty ? '家' : _tagName.text.trim(),
      isDefault: _isDefault,
      deliveryNote: _deliveryNote.text.trim(),
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({
    required this.controller,
    required this.label,
    this.keyboardType,
    this.maxLines = 1,
  });

  final TextEditingController controller;
  final String label;
  final TextInputType? keyboardType;
  final int maxLines;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 10),
    child: TextField(
      controller: controller,
      keyboardType: keyboardType,
      maxLines: maxLines,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
    ),
  );
}
