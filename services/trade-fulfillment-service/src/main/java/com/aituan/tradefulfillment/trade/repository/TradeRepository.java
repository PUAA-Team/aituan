package com.aituan.tradefulfillment.trade.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TradeRepository {
  private final JdbcTemplate jdbcTemplate;

  public TradeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long getOrCreateCart(long userId, long storeId) {
    Optional<Long> existing = findCartId(userId, storeId);
    if (existing.isPresent()) {
      return existing.get();
    }
    jdbcTemplate.update("insert into cart(user_id, store_id) values (?, ?)", userId, storeId);
    return findCartId(userId, storeId).orElseThrow();
  }

  public int findCartItemQuantity(long cartId, long itemId) {
    List<Integer> rows = jdbcTemplate.query(
        "select quantity from cart_item where cart_id = ? and item_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> rs.getInt("quantity"),
        cartId,
        itemId);
    return rows.stream().findFirst().orElse(0);
  }

  public List<CartItemRow> listCartItems(long cartId) {
    return jdbcTemplate.query(
        """
        select item_id, quantity
        from cart_item
        where cart_id = ? and is_deleted = 0
        order by updated_at desc, id desc
        """,
        (rs, rowNum) -> new CartItemRow(rs.getLong("item_id"), rs.getInt("quantity")),
        cartId);
  }

  public void upsertCartItem(long cartId, long itemId, int quantity) {
    int updated = jdbcTemplate.update(
        "update cart_item set quantity = quantity + ?, is_deleted = 0, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        quantity,
        cartId,
        itemId);
    if (updated == 0) {
      jdbcTemplate.update("insert into cart_item(cart_id, item_id, quantity) values (?, ?, ?)", cartId, itemId, quantity);
    }
    touchCart(cartId);
  }

  public void setCartItemQuantity(long cartId, long itemId, int quantity) {
    if (quantity <= 0) {
      removeCartItem(cartId, itemId);
      return;
    }
    int updated = jdbcTemplate.update(
        "update cart_item set quantity = ?, is_deleted = 0, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        quantity,
        cartId,
        itemId);
    if (updated == 0) {
      jdbcTemplate.update("insert into cart_item(cart_id, item_id, quantity) values (?, ?, ?)", cartId, itemId, quantity);
    }
    touchCart(cartId);
  }

  public void removeCartItem(long cartId, long itemId) {
    jdbcTemplate.update(
        "update cart_item set quantity = 0, is_deleted = 1, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        cartId,
        itemId);
    touchCart(cartId);
  }

  public void clearCart(long cartId) {
    jdbcTemplate.update(
        "update cart_item set quantity = 0, is_deleted = 1, updated_at = current_timestamp where cart_id = ? and is_deleted = 0",
        cartId);
    touchCart(cartId);
  }

  private Optional<Long> findCartId(long userId, long storeId) {
    List<Long> rows = jdbcTemplate.query(
        "select id from cart where user_id = ? and store_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> rs.getLong("id"),
        userId,
        storeId);
    return rows.stream().findFirst();
  }

  private void touchCart(long cartId) {
    jdbcTemplate.update("update cart set updated_at = current_timestamp where id = ?", cartId);
  }

  public record CartItemRow(Long itemId, int quantity) {}
}
