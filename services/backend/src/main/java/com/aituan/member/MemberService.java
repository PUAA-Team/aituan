package com.aituan.member;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MemberService {
  private final MemberRepository memberRepository;
  private final ObjectMapper objectMapper;

  MemberService(MemberRepository memberRepository, ObjectMapper objectMapper) {
    this.memberRepository = memberRepository;
    this.objectMapper = objectMapper;
  }

  // 用户端：当前会员等级与成长进度
  MemberInfoView myInfo() {
    long userId = CurrentUserContext.required().userId();
    int growth = Optional.ofNullable(memberRepository.findGrowthValue(userId)).orElse(0);
    List<MemberRepository.MemberLevelRow> levels = memberRepository.listEnabledLevels();
    if (levels.isEmpty()) {
      return new MemberInfoView(null, null, null, growth, null, null, null, 0, List.of());
    }

    MemberRepository.MemberLevelRow current = levels.get(0);
    MemberRepository.MemberLevelRow next = null;
    for (MemberRepository.MemberLevelRow level : levels) {
      if (level.minGrowthValue() <= growth) {
        current = level;
      } else {
        next = level;
        break;
      }
    }

    if (next == null) {
      return new MemberInfoView(current.levelCode(), current.levelName(), current.color(), growth,
          null, null, null, 100, parseBenefits(current.benefits()));
    }
    int span = next.minGrowthValue() - current.minGrowthValue();
    int progress = span <= 0 ? 0
        : (int) Math.min(100, Math.round((growth - current.minGrowthValue()) * 100.0 / span));
    return new MemberInfoView(current.levelCode(), current.levelName(), current.color(), growth,
        next.levelName(), next.minGrowthValue(), Math.max(0, next.minGrowthValue() - growth),
        progress, parseBenefits(current.benefits()));
  }

  // 后台：等级列表
  List<MemberLevelView> listLevels() {
    return memberRepository.listAllLevels().stream().map(this::toView).toList();
  }

  @Transactional
  MemberLevelView createLevel(MemberLevelUpsertRequest request) {
    Long actorId = CurrentUserContext.required().accountId();
    Long id = memberRepository.insertLevel(request, writeBenefits(request.benefits()));
    memberRepository.insertAudit(actorId, "member_level_create", "member_level", id, "新增会员等级:" + request.levelName());
    return memberRepository.findLevel(id).map(this::toView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  MemberLevelView updateLevel(long id, MemberLevelUpsertRequest request) {
    Long actorId = CurrentUserContext.required().accountId();
    memberRepository.findLevel(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    memberRepository.updateLevel(id, request, writeBenefits(request.benefits()));
    memberRepository.insertAudit(actorId, "member_level_update", "member_level", id, "更新会员等级:" + request.levelName());
    return memberRepository.findLevel(id).map(this::toView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private MemberLevelView toView(MemberRepository.MemberLevelRow row) {
    return new MemberLevelView(row.id(), row.levelCode(), row.levelName(), row.minGrowthValue(),
        parseBenefits(row.benefits()), row.iconUrl(), row.color(), row.sortOrder(), row.status());
  }

  private List<MemberBenefitItem> parseBenefits(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<MemberBenefitItem>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private String writeBenefits(List<MemberBenefitItem> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(items);
    } catch (Exception e) {
      return null;
    }
  }
}
