package com.aituan.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import com.aituan.common.security.CurrentUserContext;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiAgentServiceTest {

  @Autowired private AiAgentService aiAgentService;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void assistantPersistsMessagesAndReturnsHistoryWithSteps() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("帮我看看最近订单和优惠券", null));

    assertThat(response.conversationId()).isNotBlank();
    assertThat(response.steps())
        .extracting(AiAssistantStep::title)
        .contains("调用了订单信息", "调用了优惠券信息");

    AiAssistantHistoryResponse history = aiAgentService.currentConversation(CurrentUserContext.required());

    assertThat(history.conversationId()).isEqualTo(response.conversationId());
    assertThat(history.messages()).hasSizeGreaterThanOrEqualTo(2);
    assertThat(history.messages())
        .anySatisfy(message -> {
          assertThat(message.role()).isEqualTo("user");
          assertThat(message.content()).contains("最近订单");
        })
        .anySatisfy(message -> {
          assertThat(message.role()).isEqualTo("assistant");
          assertThat(message.steps())
              .extracting(AiAssistantStep::title)
              .contains("调用了订单信息", "调用了优惠券信息");
        });
  }

  @Test
  void assistantTreatsGenericDiscountQuestionAsCouponSkill() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("看一下我有什么优惠可用", null));

    assertThat(response.usedSkills()).contains("coupon_lookup");
    assertThat(response.steps())
        .extracting(AiAssistantStep::title)
        .contains("调用了优惠券信息");
  }

  @Test
  void assistantKeepsGroupBuyQueriesFocusedOnDeals() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("帮我整理附近所有团购套餐", null));

    assertThat(response.usedSkills()).contains("store_lookup", "item_lookup");
    assertThat(response.steps())
        .extracting(AiAssistantStep::title)
        .contains("调用了店铺信息", "调用了商品服务信息");
    assertThat(response.cards()).isNotEmpty();
    assertThat(response.cards())
        .filteredOn(card -> "store".equals(card.type()))
        .allSatisfy(card -> {
          assertThat(card.payload()).containsEntry("businessType", "group_buy");
          assertThat(card.content()).contains("套餐", "¥", "热门", "评分", "月售");
          assertThat(card.payload()).containsKeys("dealCount", "minDealPrice", "topDealName");
        });
    assertThat(response.cards())
        .filteredOn(card -> "item".equals(card.type()))
        .allSatisfy(card -> assertThat(card.payload()).containsEntry("businessType", "group_buy"));
    assertThat(response.cards()).satisfies(cards -> {
      Long currentStoreId = null;
      for (AiAssistantCard card : cards) {
        if ("store".equals(card.type()) && "group_buy".equals(card.payload().get("businessType"))) {
          currentStoreId = ((Number) card.payload().get("storeId")).longValue();
        }
        if ("item".equals(card.type()) && "group_buy".equals(card.payload().get("businessType"))) {
          assertThat(currentStoreId).isEqualTo(((Number) card.payload().get("storeId")).longValue());
        }
      }
    });
    assertThat(response.reply()).contains("团购", "推荐逻辑");
  }

  @Test
  void assistantKeepsSpecificGroupBuyCardsRelevantToQuery() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("我想找烤肉套餐", null));

    assertThat(response.usedSkills()).contains("store_lookup", "item_lookup");
    assertThat(response.cards()).isNotEmpty();
    assertThat(response.cards())
        .filteredOn(card -> "store".equals(card.type()) || "item".equals(card.type()))
        .allSatisfy(card -> assertThat(card.title()).containsAnyOf("烤肉", "琥珀"));
    assertThat(response.cards())
        .extracting(AiAssistantCard::title)
        .noneMatch(title -> title.contains("江南小馆") || title.contains("汉堡") || title.contains("拌饭"));
  }

  @Test
  void assistantKeepsLeisureQueriesFocusedOnLeisureCards() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("附近有什么休闲玩乐", null));

    Set<String> leisureTypes = Set.of("movie", "ticket", "massage", "entertainment");

    assertThat(response.usedSkills()).contains("store_lookup", "item_lookup");
    assertThat(response.cards()).isNotEmpty();
    assertThat(response.cards())
        .allSatisfy(card -> {
          assertThat(card.payload()).containsKey("businessType");
          assertThat(card.payload().get("businessType")).isIn(leisureTypes);
        });
    assertThat(response.cards())
        .extracting(AiAssistantCard::title)
        .noneMatch(title -> title.contains("汉堡") || title.contains("拌饭") || title.contains("炸鸡"));
    assertThat(response.quickActions())
        .anySatisfy(action -> assertThat(action.payload()).containsEntry("keyword", "休闲玩乐"));
    assertThat(response.reply()).contains("休闲玩乐");
  }

  @Test
  void assistantQueriesBroadRealBusinessContext() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("帮我全面看看我的全部信息，订单优惠券地址收藏消息会员客服评价投诉购物车，附近汉堡店和商品也一起查", null));

    assertThat(response.steps())
        .extracting(AiAssistantStep::title)
        .contains(
            "调用了账号摘要",
            "调用了订单信息",
            "调用了优惠券信息",
            "调用了地址信息",
            "调用了收藏信息",
            "调用了站内消息",
            "调用了会员信息",
            "调用了客服会话信息",
            "调用了评价信息",
            "调用了投诉工单信息",
            "调用了购物车信息",
            "调用了店铺信息",
            "调用了商品服务信息");
    assertThat(response.cards())
        .extracting(AiAssistantCard::type)
        .contains("order", "coupon", "support", "store", "item");
    assertThat(response.reply()).contains("真实");
  }

  @Test
  void assistantUsesConversationMemoryForFollowUpReferences() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse first = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("帮我看看附近汉堡店", null));

    AiAssistantResponse followUp = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("刚才第一个还有什么商品", first.conversationId()));

    assertThat(followUp.conversationId()).isEqualTo(first.conversationId());
    assertThat(followUp.steps())
        .extracting(AiAssistantStep::title)
        .contains("调用了商品服务信息");
    assertThat(followUp.cards())
        .extracting(AiAssistantCard::type)
        .contains("item");
  }
}
