package com.aituan.engagementplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ServiceBoundaryTest {
  private static final List<String> FOREIGN_TABLES=List.of(
      "iam_account","iam_verification_code","iam_role","iam_permission","iam_account_role","iam_role_permission",
      "user_profile","user_address","user_favorite","member_level","member_growth_log","member_weekly_coupon_rule",
      "member_weekly_coupon_batch","member_weekly_coupon_issue","coupon_template","user_coupon","support_station_message",
      "merchant_profile","merchant_store","merchant_delivery_rule","merchant_takeaway_setting","merchant_application",
      "merchant_certification_material","merchant_audit_log","catalog_category","catalog_item","catalog_sku",
      "catalog_item_tag","catalog_item_tag_rel","ops_banner_config","member_recommend_config",
      "cart","cart_item","order_main","order_item","order_payment_record","order_voucher","order_booking_record",
      "order_refund_record","order_state_log","delivery_task","delivery_track_node");
  private static final Pattern FOREIGN_SQL=Pattern.compile(
      "(?i)\\b(?:from|join|update|into|table)\\s+`?(" + String.join("|", FOREIGN_TABLES) + ")`?\\b");

  @Test void productionSourcesDoNotQueryTablesOwnedByOtherServices() throws IOException {
    List<Path> roots=List.of(Path.of("src/main/java"),Path.of("src/main/resources/db"));
    List<String> violations=new java.util.ArrayList<>();
    for(Path root:roots){try(var paths=Files.walk(root)){
      paths.filter(Files::isRegularFile).forEach(path->{
        try{
          var matcher=FOREIGN_SQL.matcher(Files.readString(path));
          while(matcher.find()) violations.add(path+" -> "+matcher.group(1));
        }catch(IOException e){throw new IllegalStateException(e);}
      });
    }}
      assertThat(violations).isEmpty();
  }
}
