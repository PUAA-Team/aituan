package com.aituan.common.file;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/demo-images")
class DemoImageController {
  private static final Color[] PALETTE = {
      new Color(239, 68, 68),
      new Color(245, 158, 11),
      new Color(16, 185, 129),
      new Color(59, 130, 246),
      new Color(168, 85, 247)
  };

  @GetMapping("/{fileName:.+}")
  ResponseEntity<byte[]> image(@PathVariable String fileName) throws IOException {
    BufferedImage image = new BufferedImage(720, 540, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int index = Math.floorMod(fileName.hashCode(), PALETTE.length);
      Color base = PALETTE[index];
      g.setPaint(new GradientPaint(0, 0, soften(base, 0.88f), 720, 540, soften(base, 0.45f)));
      g.fillRect(0, 0, 720, 540);

      g.setColor(new Color(255, 255, 255, 96));
      g.fillRoundRect(48, 48, 624, 444, 36, 36);
      g.setColor(new Color(255, 255, 255, 160));
      g.fillOval(455, 55, 150, 150);
      g.fillOval(105, 315, 105, 105);

      g.setColor(new Color(33, 37, 41));
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
      g.drawString("AITUAN", 80, 150);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
      g.drawString("REVIEW PHOTO", 80, 215);
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
      g.drawString(cleanName(fileName), 80, 270);
      g.drawString("本地稳定演示图片", 80, 320);
    } finally {
      g.dispose();
    }

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
        .body(output.toByteArray());
  }

  private Color soften(Color color, float mix) {
    int red = Math.round(255 - (255 - color.getRed()) * mix);
    int green = Math.round(255 - (255 - color.getGreen()) * mix);
    int blue = Math.round(255 - (255 - color.getBlue()) * mix);
    return new Color(red, green, blue);
  }

  private String cleanName(String fileName) {
    String value = fileName == null ? "review.png" : fileName.replaceAll("[^A-Za-z0-9._-]", "");
    return value.length() > 36 ? value.substring(0, 36) : value;
  }
}
