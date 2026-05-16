import '../../../shared/enums/business_type.dart';
import '../../../shared/models/module_entry.dart';

const modules = [
  ModuleEntry(code: 'takeaway', title: '外卖', type: BusinessType.takeaway),
  ModuleEntry(code: 'group', title: '团购', type: BusinessType.groupBuy),
  ModuleEntry(code: 'hotel', title: '酒店', type: BusinessType.hotel),
  ModuleEntry(code: 'fun', title: '休闲娱乐', type: BusinessType.entertainment),
  ModuleEntry(code: 'movie', title: '电影演出', type: BusinessType.movie),
  ModuleEntry(code: 'beauty', title: '丽人医美', type: BusinessType.beauty),
  ModuleEntry(code: 'ticket', title: '景点门票', type: BusinessType.ticket),
  ModuleEntry(code: 'massage', title: '洗脚', type: BusinessType.massage),
];
