package com.llmledger.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/avatar")
public class AvatarController {

    @GetMapping("/generate")
    public void generateAvatar(@RequestParam(defaultValue = "User") String seed,
            @RequestParam(defaultValue = "ff6b35") String backgroundColor,
            HttpServletResponse response) throws IOException {

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "public, max-age=86400");

        // 创建头像图片
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制圆形背景
        Color bgColor = Color.decode("#" + backgroundColor.replace("#", ""));
        g2d.setColor(bgColor);
        g2d.fillOval(0, 0, 200, 200);

        // 生成随机颜色用于头像
        Random random = new Random(seed.hashCode());
        Color textColor = new Color(255 - random.nextInt(100), 255 - random.nextInt(100), 255 - random.nextInt(100));
        g2d.setColor(textColor);

        // 绘制首字母
        String initial = seed.substring(0, 1).toUpperCase();
        Font font = new Font("Arial", Font.BOLD, 100);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int x = (200 - fm.stringWidth(initial)) / 2;
        int y = (200 + fm.getAscent()) / 2 - 10;
        g2d.drawString(initial, x, y);

        g2d.dispose();

        // 输出图片
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        response.getOutputStream().write(baos.toByteArray());
        response.getOutputStream().flush();
    }
}
