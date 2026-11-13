package com.cms.desktop;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Soft, rounded AWT card used for screen sections and dashboard tiles. */
final class SurfacePanel extends Panel {
  SurfacePanel() {
    setBackground(DesktopTheme.SURFACE);
  }

  @Override
  public void paint(Graphics graphics) {
    Graphics2D graphics2D = (Graphics2D) graphics.create();
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int width = Math.max(0, getWidth() - 2);
    int height = Math.max(0, getHeight() - 3);
    graphics2D.setColor(DesktopTheme.SHADOW);
    graphics2D.fill(new RoundRectangle2D.Float(1, 3, width, height, 18, 18));
    graphics2D.setColor(getBackground() == null ? DesktopTheme.SURFACE : getBackground());
    graphics2D.fill(new RoundRectangle2D.Float(1, 1, width, height, 18, 18));
    graphics2D.dispose();
    super.paint(graphics);
    graphics2D = (Graphics2D) graphics.create();
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics2D.setColor(DesktopTheme.BORDER);
    graphics2D.draw(new RoundRectangle2D.Float(1, 1, width, height, 18, 18));
    graphics2D.dispose();
  }
}
