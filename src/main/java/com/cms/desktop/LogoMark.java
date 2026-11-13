package com.cms.desktop;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small rupee mark that gives every window a consistent CMS identity. */
final class LogoMark extends Canvas {
  LogoMark() {
    setPreferredSize(new Dimension(44, 44));
    setBackground(DesktopTheme.SURFACE);
  }

  @Override
  public void paint(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int diameter = Math.min(getWidth(), getHeight()) - 2;
    g.setColor(DesktopTheme.ACCENT_RED);
    g.fillOval(1, 1, diameter, diameter);
    g.setColor(Color.WHITE);
    g.setFont(DesktopTheme.font(23, true));
    FontMetrics fm = g.getFontMetrics();
    String mark = "₹";
    g.drawString(mark, (getWidth() - fm.stringWidth(mark)) / 2,
        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
    g.dispose();
  }
}
