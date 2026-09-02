package com.aituan.identity.member;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 会员权益条目
record MemberBenefitItem(String title, String desc) {}

// 用户端：我的会员信息（含成长进度与权益）
@JsonInclude(JsonInclude.Include.NON_NULL)
record MemberInfoView(
    String currentLevelCode,
    String currentLevelName,
    String currentColor,
    int growthValue,
    String nextLevelName,
    Integer nextLevelMinGrowth,
    Integer growthToNextLevel,
    int progressPercent,
    List<MemberBenefitItem> benefits) {}

// 后台：会员等级展示
@JsonInclude(JsonInclude.Include.NON_NULL)
record MemberLevelView(
    Long id,
    String levelCode,
    String levelName,
    int minGrowthValue,
    List<MemberBenefitItem> benefits,
    String iconUrl,
    String color,
    int sortOrder,
    String status) {}

// 后台：新增/编辑会员等级
record MemberLevelUpsertRequest(
    @NotBlank String levelCode,
    @NotBlank String levelName,
    @NotNull @Min(0) Integer minGrowthValue,
    List<MemberBenefitItem> benefits,
    String iconUrl,
    String color,
    Integer sortOrder,
    String status) {}
