import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';

class AboutDocumentPage extends StatelessWidget {
  const AboutDocumentPage({super.key, required this.document});

  final AboutDocument document;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(document.title)),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                document.title,
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 12),
              for (final section in document.sections) ...[
                Text(
                  section.title,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  section.content,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: 16),
              ],
            ],
          ),
        ),
      ],
    ),
  );
}

class AboutDocument {
  const AboutDocument({required this.title, required this.sections});

  final String title;
  final List<AboutDocumentSection> sections;
}

class AboutDocumentSection {
  const AboutDocumentSection({required this.title, required this.content});

  final String title;
  final String content;
}

const userAgreementDocument = AboutDocument(
  title: '用户协议',
  sections: [
    AboutDocumentSection(
      title: '服务说明',
      content:
          '爱团为用户提供本地生活服务的信息浏览、商品或服务下单、订单履约、评价反馈、客服沟通及售后协助等功能。用户使用爱团服务时，应遵守法律法规、平台规则以及页面展示的服务说明。',
    ),
    AboutDocumentSection(
      title: '账号使用',
      content:
          '用户应妥善保管账号、密码、验证码和登录状态。通过用户账号产生的操作视为用户本人行为。发现账号异常时，应及时修改密码或联系平台客服处理。',
    ),
    AboutDocumentSection(
      title: '订单与履约',
      content:
          '用户提交订单前应核对商品、服务、地址、预约、餐具、费用等信息。订单支付、取消、核销、配送、退款等规则以页面展示、商家规则和平台治理规则为准。',
    ),
    AboutDocumentSection(
      title: '评价与内容',
      content:
          '用户发布评价、投诉、咨询或上传图片时，应保证内容真实、合法、文明，不得侵犯他人权益，不得发布违法违规、虚假误导或恶意攻击内容。',
    ),
    AboutDocumentSection(
      title: '责任边界',
      content:
          '平台将尽力保障服务稳定和交易安全。因不可抗力、网络故障、第三方服务异常、用户自身设备或操作原因导致的影响，平台将在合理范围内协助处理。',
    ),
  ],
);

const privacyPolicyDocument = AboutDocument(
  title: '隐私政策',
  sections: [
    AboutDocumentSection(
      title: '信息收集',
      content:
          '为了提供账号登录、下单履约、配送定位、到店核销、评价售后和客服沟通等服务，爱团可能收集手机号、邮箱、昵称、头像、收货地址、定位坐标、订单信息、评价内容和上传图片等必要信息。',
    ),
    AboutDocumentSection(
      title: '定位与地址',
      content:
          '在用户授权后，爱团会使用定位能力展示附近商户、计算配送距离和预计送达时间。用户也可以手动填写地址，平台会在必要时解析地址坐标用于配送判断。',
    ),
    AboutDocumentSection(
      title: '信息使用',
      content:
          '平台仅在提供服务、保障交易安全、处理售后投诉、改进产品体验和履行法律义务所必需的范围内使用用户信息。未经用户授权，平台不会将个人信息用于与服务无关的用途。',
    ),
    AboutDocumentSection(
      title: '信息保护',
      content: '平台会采取访问控制、数据校验、传输保护和日志审计等措施保护用户信息安全。用户也应妥善保管账号密码和验证码，避免向他人泄露。',
    ),
    AboutDocumentSection(
      title: '用户权利',
      content:
          '用户可以在应用内查看和修改部分资料、管理地址、查看订单和售后记录。如需进一步查询、更正或删除个人信息，可通过客服渠道联系平台处理。',
    ),
  ],
);

const qualificationSupportDocument = AboutDocument(
  title: '平台资质与客服',
  sections: [
    AboutDocumentSection(
      title: '平台说明',
      content:
          '爱团致力于连接用户、商户与平台运营，为外卖、团购、酒店、电影演出、休闲娱乐、丽人医美、景点门票和到店服务提供统一的浏览、下单与履约体验。',
    ),
    AboutDocumentSection(
      title: '商家资质',
      content:
          '平台要求商家提交必要的主体、门店、经营和服务资料，并根据业务类型进行审核与治理。用户下单前可结合门店信息、商品说明、评价和平台提示进行判断。',
    ),
    AboutDocumentSection(
      title: '服务提示',
      content:
          '外卖订单请确认收货地址、餐具需求和配送范围；到店服务请关注券码有效期、预约要求、退款规则和商家营业时间。特殊服务以商家页面说明和订单信息为准。',
    ),
    AboutDocumentSection(
      title: '客服渠道',
      content:
          '用户可以通过应用内客服会话、订单售后、投诉与建议等入口联系平台或商家。平台会根据问题类型协助核实订单、沟通商家、处理投诉和反馈结果。',
    ),
  ],
);
