package com.cms.desktop;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Cursor;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Lightweight rounded AWT button, avoiding platform-native beveled controls. */
final class ActionButton extends Canvas {
  private final String caption;
  private final List<ActionListener> listeners = new ArrayList<>();
  private boolean hovered;
  private boolean pressed;
  private final boolean calendarIcon;

  ActionButton(String caption) {
    this.caption = caption == null ? "" : caption;
    this.calendarIcon = "CALENDAR_ICON".equals(this.caption);
    setName(calendarIcon ? "Choose date" : this.caption);
    getAccessibleContext().setAccessibleName(calendarIcon ? "Choose date" : this.caption);
    setFont(DesktopTheme.MEDIUM);
    setBackground(DesktopTheme.BLUE);
    setForeground(Color.WHITE);
    setFocusable(true);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addMouseListener(new MouseAdapter() {
      @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
      @Override public void mouseExited(MouseEvent e) { hovered = false; pressed = false; repaint(); }
      @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
      @Override public void mouseReleased(MouseEvent e) {
        boolean activate = pressed && contains(e.getPoint());
        pressed = false;
        repaint();
        if (activate && isEnabled()) fireAction();
      }
    });
    addFocusListener(new FocusAdapter() {
      @Override public void focusGained(FocusEvent e) { repaint(); }
      @Override public void focusLost(FocusEvent e) { pressed = false; repaint(); }
    });
    addKeyListener(new KeyAdapter() {
      @Override public void keyPressed(KeyEvent e) {
        if (isEnabled() && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) fireAction();
      }
    });
  }

  void addActionListener(ActionListener listener) { listeners.add(listener); }

  private void fireAction() {
    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, caption);
    for (ActionListener listener : List.copyOf(listeners)) listener.actionPerformed(event);
  }

  @Override public void paint(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // Clear the opaque Canvas corners using the surrounding panel color.
    g.setColor(getParent() == null ? DesktopTheme.SURFACE : getParent().getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
    Color base = getBackground();
    if (!isEnabled()) base = blend(base, DesktopTheme.SURFACE, 0.55f);
    Color top = pressed ? blend(base, Color.BLACK, 0.08f)
        : hovered ? blend(base, Color.WHITE, 0.20f) : blend(base, Color.WHITE, 0.10f);
    Color bottom = pressed ? blend(base, Color.BLACK, 0.17f)
        : hovered ? blend(base, Color.BLACK, 0.02f) : blend(base, Color.BLACK, 0.10f);
    int arc = Math.max(1, getHeight() - 4);
    int width = Math.max(0, getWidth() - 3);
    int height = Math.max(0, getHeight() - 4);
    // Shared gradient, subtle shadow, and focus ring give every screen a
    // consistent modern treatment without changing layout or button sizing.
    g.setColor(new Color(20, 40, 75, isEnabled() ? 34 : 12));
    g.fillRoundRect(2, 3, width, height, arc, arc);
    g.setPaint(new GradientPaint(1, 1, top, 1, Math.max(2, height), bottom));
    g.fillRoundRect(1, 1, width, height, arc, arc);
    g.setPaint(null);
    g.setStroke(new BasicStroke(1f));
    g.setColor(blend(base, Color.BLACK, isEnabled() ? 0.17f : 0.08f));
    g.drawRoundRect(1, 1, width, height, arc, arc);
    if (hasFocus()) {
      g.setColor(blend(base, Color.WHITE, 0.68f));
      g.setStroke(new BasicStroke(1.5f));
      g.drawRoundRect(3, 3, Math.max(0, getWidth() - 7), Math.max(0, getHeight() - 8),
          Math.max(4, arc - 3), Math.max(4, arc - 3));
    }
    if (calendarIcon) {
      int size = Math.min(15, Math.min(getWidth() - 10, getHeight() - 8));
      int x = (getWidth() - size) / 2;
      int y = (getHeight() - size) / 2;
      g.setColor(isEnabled() ? getForeground() : DesktopTheme.MUTED);
      g.setStroke(new java.awt.BasicStroke(1.5f));
      g.drawRoundRect(x + 1, y + 2, size - 2, size - 3, 3, 3);
      g.drawLine(x + 1, y + 6, x + size - 1, y + 6);
      g.drawLine(x + 5, y, x + 5, y + 4);
      g.drawLine(x + size - 5, y, x + size - 5, y + 4);
      g.fillOval(x + 5, y + 9, 2, 2);
      g.fillOval(x + 9, y + 9, 2, 2);
    } else {
      g.setFont(getFont());
      g.setColor(isEnabled() ? getForeground() : DesktopTheme.MUTED);
      FontMetrics metrics = g.getFontMetrics();
      int x = Math.max(6, (getWidth() - metrics.stringWidth(caption)) / 2);
      int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
      g.drawString(caption, x, y);
    }
    g.dispose();
  }

  @Override public void update(Graphics graphics) { paint(graphics); }

  @Override public java.awt.Dimension getPreferredSize() {
    FontMetrics metrics = getFontMetrics(getFont());
    if (calendarIcon) return new java.awt.Dimension(32, 30);
    int width = Math.max(70, Math.min(184, metrics.stringWidth(caption) + 26));
    return new java.awt.Dimension(width, 30);
  }

  @Override public java.awt.Dimension getMinimumSize() { return getPreferredSize(); }

  @Override public java.awt.Dimension getMaximumSize() { return getPreferredSize(); }

  @Override public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    repaint();
  }

  private static Color blend(Color a, Color b, float ratio) {
    return new Color((int) (a.getRed() * (1 - ratio) + b.getRed() * ratio),
        (int) (a.getGreen() * (1 - ratio) + b.getGreen() * ratio),
        (int) (a.getBlue() * (1 - ratio) + b.getBlue() * ratio));
  }
}


