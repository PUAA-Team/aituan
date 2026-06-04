package com.aituan.support;

import org.springframework.stereotype.Service;

@Service
class AiSupportService {
  AiSupportService() {}

  String reply(String content) {
    String text = content == null ? "" : content;
    if (containsAny(text, "退款", "退单", "取消")) {
      return "我已记录您的退款/取消诉求。请补充订单号、支付时间和期望处理方式；情况复杂时可点击“转人工”。";
    }
    if (containsAny(text, "投诉", "差评", "纠纷", "举报")) {
      return "我已记录您的投诉诉求。您可以点右上角投诉入口提交工单并上传证据，也可以发送“转人工”让平台客服接入。";
    }
    if (containsAny(text, "配送", "多久", "催", "慢", "骑手")) {
      return "我已收到配送/时效问题。请补充订单号和当前等待时长，我会优先帮您整理给平台；需要人工可发送“转人工”。";
    }
    if (containsAny(text, "发票", "票据", "抬头")) {
      return "发票问题请补充发票抬头、税号和接收邮箱；如订单状态异常，可发送“转人工”继续处理。";
    }
    if (containsAny(text, "优惠券", "红包", "满减", "活动")) {
      return "优惠券或活动问题请补充券名、订单金额和截图，平台会核对使用规则；需要人工可发送“转人工”。";
    }
    return "平台客服助手已收到。请继续补充订单或问题细节；如需人工处理，可点击“转人工”或直接发送“转人工”。";
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }
}
