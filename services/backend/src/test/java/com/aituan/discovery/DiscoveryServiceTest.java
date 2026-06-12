package com.aituan.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import com.aituan.common.api.PageResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DiscoveryServiceTest {

  @Autowired private DiscoveryService discoveryService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanup() {
    jdbcTemplate.update("delete from support_station_message where user_id = ?", 99001L);
    jdbcTemplate.update("delete from user_favorite where user_id = ?", 99001L);
    jdbcTemplate.update("delete from order_item where order_id in (select id from order_main where user_id = ?)", 99001L);
    jdbcTemplate.update("delete from order_main where user_id = ?", 99001L);
    TestAuthSupport.clear();
  }

  @Test
  void personalizedRecommendationsPreferUserHistory() {
    Long itemId = jdbcTemplate.queryForObject(
        "select id from catalog_item where business_type = 'hotel' and is_deleted = 0 and status = 'on_sale' order by id limit 1",
        Long.class);
    assertThat(itemId).isNotNull();
    jdbcTemplate.update(
        "insert into user_favorite(user_id, favorite_type, target_id, target_name) values (?, 'item', ?, '偏好酒店')",
        99001L,
        itemId);
    TestAuthSupport.loginAsUser(99001L, 99001L);

    PageResponse<ItemCardView> page = discoveryService.recommendations(1, 6, "personalized", null, null);

    assertThat(page.list()).isNotEmpty();
    assertThat(page.list().get(0).businessType()).isEqualTo("hotel");
    assertThat(page.list().get(0).recommendReason()).isNotBlank();
  }

  @Test
  void anonymousPersonalizedRecommendationsFallbackToHotSort() {
    PageResponse<ItemCardView> page = discoveryService.recommendations(1, 6, "personalized", null, null);

    assertThat(page.list()).isNotEmpty();
    assertThat(page.list().get(0).recommendReason()).isNotBlank();
  }

  @Test
  void anonymousHomeDoesNotExposeDemoUnreadMessages() {
    jdbcTemplate.update(
        "insert into support_station_message(user_id, message_type, title, content, read_status) values (?, 'system', '测试未读', '不应展示给游客', 'unread')",
        99001L);

    HomeView home = discoveryService.home(null, null);

    assertThat(home.unreadMessageCount()).isZero();
  }

  @Test
  void searchCanSortBySales() {
    PageResponse<StoreCardView> page = discoveryService.search("", 1, 6, "sales", null, null, null);

    assertThat(page.list()).hasSizeGreaterThan(1);
    assertThat(page.list().get(0).monthlySales()).isGreaterThanOrEqualTo(page.list().get(1).monthlySales());
  }

  @Test
  void searchCanSortByLowestAveragePrice() {
    PageResponse<StoreCardView> page = discoveryService.search("", 1, 6, "price_asc", null, null, null);

    assertThat(page.list()).hasSizeGreaterThan(1);
    BigDecimal first = page.list().get(0).avgPrice();
    BigDecimal second = page.list().get(1).avgPrice();
    assertThat(first.compareTo(second)).isLessThanOrEqualTo(0);
  }

  @Test
  void searchCanFilterByBusinessType() {
    PageResponse<StoreCardView> page = discoveryService.search("", 1, 20, "default", "takeaway", null, null);

    assertThat(page.list()).isNotEmpty();
    assertThat(page.list()).allMatch(store -> "takeaway".equals(store.businessType()));
  }
}
