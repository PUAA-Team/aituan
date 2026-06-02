package com.aituan.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
class EmailVerificationSender {
  private final JavaMailSender mailSender;
  private final boolean enabled;
  private final String from;
  private final String fromName;

  EmailVerificationSender(
      JavaMailSender mailSender,
      @Value("${aituan.mail.enabled:false}") boolean enabled,
      @Value("${aituan.mail.from:}") String from,
      @Value("${aituan.mail.from-name:爱团}") String fromName) {
    this.mailSender = mailSender;
    this.enabled = enabled;
    this.from = from;
    this.fromName = fromName;
  }

  boolean enabled() {
    return enabled && from != null && !from.isBlank();
  }

  void sendCode(String email, String scene, String code, long expireMinutes) {
    if (!enabled()) {
      return;
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject(subject(scene));
    message.setText("您的爱团验证码为：" + code + "。验证码 " + expireMinutes + " 分钟内有效，请勿转发给他人。若非本人操作，请忽略本邮件。");
    mailSender.send(message);
  }

  private String subject(String scene) {
    if ("reset_password".equals(scene)) {
      return "爱团找回密码验证码";
    }
    return "爱团邮箱验证码";
  }
}
