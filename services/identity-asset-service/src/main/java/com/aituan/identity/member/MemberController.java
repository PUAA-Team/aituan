package com.aituan.identity.member;

import com.aituan.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 用户端会员信息
@RestController
@RequestMapping("/api/app/account/member")
class MemberController {
  private final MemberService memberService;

  MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping("/info")
  ApiResponse<MemberInfoView> info() {
    return ApiResponse.ok(memberService.myInfo());
  }
}
