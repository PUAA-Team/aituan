package com.aituan.identity.member;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 后台会员等级配置
@RestController
@RequestMapping("/api/admin/operation/member-levels")
@Validated
class MemberAdminController {
  private final MemberService memberService;

  MemberAdminController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping
  ApiResponse<List<MemberLevelView>> list() {
    return ApiResponse.ok(memberService.listLevels());
  }

  @PostMapping
  ApiResponse<MemberLevelView> create(@Valid @RequestBody MemberLevelUpsertRequest request) {
    return ApiResponse.ok(memberService.createLevel(request));
  }

  @PutMapping("/{id}")
  ApiResponse<MemberLevelView> update(@PathVariable long id, @Valid @RequestBody MemberLevelUpsertRequest request) {
    return ApiResponse.ok(memberService.updateLevel(id, request));
  }
}
