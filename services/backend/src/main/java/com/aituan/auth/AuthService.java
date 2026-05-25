package com.aituan.auth;

import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.JwtTokenService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {
  private static final SecureRandom RANDOM = new SecureRandom();

  private final AuthRepository authRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;

  AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
    this.authRepository = authRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
  }

  EmailCodeResponse sendEmailCode(EmailCodeRequest request) {
    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    authRepository.insertCode(request.email(), request.scene(), code, LocalDateTime.now().plusMinutes(10));
    return new EmailCodeResponse(request.email(), request.scene(), code, 600);
  }

  @Transactional
  AuthResponse register(RegisterRequest request) {
    if (authRepository.existsPhone(request.phone())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "手机号已注册");
    }
    if (authRepository.existsEmail(request.email())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "邮箱已注册");
    }
    AuthRepository.VerificationCodeRow codeRow = authRepository.findValidCode(request.email(), "register", request.emailCode())
        .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID));
    authRepository.markCodeUsed(codeRow.id());
    Long accountId = authRepository.insertAccount(
        "U" + System.currentTimeMillis(),
        AccountType.USER.name(),
        request.phone(),
        request.phone(),
        request.email(),
        passwordEncoder.encode(request.password()));
    authRepository.insertAccountRole(accountId, 1L);
    authRepository.insertProfile(accountId, maskPhone(request.phone()));
    return issueToken(accountId);
  }

  AuthResponse login(LoginRequest request) {
    return loginForType(request, null);
  }

  AuthResponse loginMerchant(LoginRequest request) {
    return loginForType(request, AccountType.MERCHANT);
  }

  AuthResponse loginAdmin(LoginRequest request) {
    return loginForType(request, AccountType.ADMIN);
  }

  private AuthResponse loginForType(LoginRequest request, AccountType expectedType) {
    AuthRepository.AccountRow account = authRepository.findAccountByLogin(request.account())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD));
    if (expectedType != null && account.accountType() != expectedType) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!passwordEncoder.matches(request.password(), account.passwordHash()) && !request.password().equals(account.passwordHash())) {
      throw new BusinessException(ErrorCode.INVALID_PASSWORD);
    }
    authRepository.updateLastLogin(account.id(), "127.0.0.1");
    return issueToken(account.id());
  }

  @Transactional
  void resetPassword(ResetPasswordRequest request) {
    AuthRepository.VerificationCodeRow codeRow = authRepository.findValidCode(request.email(), "reset", request.emailCode())
        .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID));
    AuthRepository.AccountRow account = authRepository.findAccountByEmail(request.email())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    authRepository.markCodeUsed(codeRow.id());
    authRepository.updatePassword(account.id(), passwordEncoder.encode(request.newPassword()));
  }

  TokenCheckResponse checkToken(CurrentUser currentUser) {
    if (currentUser == null) {
      return new TokenCheckResponse(false, null);
    }
    AuthRepository.UserProfileRow profileRow = authRepository.findProfileByAccountId(currentUser.accountId()).orElse(null);
    if (profileRow != null) {
      return new TokenCheckResponse(true, toProfile(profileRow));
    }
    AuthRepository.AccountRow account = authRepository.findAccountById(currentUser.accountId()).orElse(null);
    return new TokenCheckResponse(true, account == null ? null : toProfile(account));
  }

  private AuthResponse issueToken(Long accountId) {
    AuthRepository.AccountRow account = authRepository.findAccountById(accountId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    AuthRepository.UserProfileRow profileRow = authRepository.findProfileByAccountId(accountId).orElse(null);
    AuthProfile profile = profileRow == null ? toProfile(account) : toProfile(profileRow);
    Long currentUserId = profileRow == null ? account.id() : profileRow.id();
    CurrentUser currentUser = new CurrentUser(account.id(), currentUserId, account.accountType(), profile.nickname());
    String token = jwtTokenService.createToken(currentUser);
    return new AuthResponse(token, jwtTokenService.ttlSeconds(), profile);
  }

  private AuthProfile toProfile(AuthRepository.UserProfileRow row) {
    return new AuthProfile(row.id(), row.nickname(), row.avatarUrl(), row.phone(), row.email(), row.memberLevelName());
  }

  private AuthProfile toProfile(AuthRepository.AccountRow row) {
    String nickname = row.loginName() == null || row.loginName().isBlank() ? row.accountNo() : row.loginName();
    return new AuthProfile(row.id(), nickname, null, row.phone(), row.email(), row.accountType().name());
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() != 11) {
      return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(7);
  }
}
