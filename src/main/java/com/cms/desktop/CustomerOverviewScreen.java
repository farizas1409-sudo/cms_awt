package com.cms.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.util.List;
import java.util.Map;

/** Compact customer identity, balance summary, and task navigation component. */
final class CustomerOverviewScreen {
  private CustomerOverviewScreen() {}

  static Panel render(CmsFrame frame, Map<String, Object> data) {
    Map<String, Object> customer = frame.mapOf(data.get("customer"));
    Panel card = frame.gridPanel("Customer overview");
    Panel summary = new Panel(new BorderLayout(18, 6));
    summary.setBackground(DesktopTheme.SURFACE);
    Panel identity = new Panel(new GridLayout(4, 1, 0, 3));
    identity.setBackground(DesktopTheme.SURFACE);
    if (customer.isEmpty()) {
      identity.add(frame.label("Customer details are not available.", 12, false, DesktopTheme.MUTED));
    } else {
      identity.add(frame.label(frame.text(customer.get("NAME")), 16, true, DesktopTheme.INK));
      identity.add(frame.label("Customer account  ·  " + frame.text(customer.get("CUSTOMER_NO")), 10, true, DesktopTheme.BLUE));
      identity.add(frame.label("Phone  " + frame.text(customer.get("PHONE")) + "     Email  " + frame.text(customer.get("EMAIL")), 11, false, DesktopTheme.MUTED));
      identity.add(frame.label(frame.text(customer.get("ADDRESS")), 11, false, DesktopTheme.MUTED));
    }
    summary.add(identity, BorderLayout.CENTER);
    Map<String, Object> totals = frame.mapOf(data.get("summary"));
    Panel metrics = new Panel(new GridLayout(1, 4, 8, 0));
    metrics.setBackground(DesktopTheme.SURFACE);
    metrics.add(metric("Loans", totals.get("ACCOUNTS"), DesktopTheme.BLUE));
    metrics.add(metric("Outstanding", frame.rupees(totals.get("OUTSTANDING")), DesktopTheme.SUCCESS));
    metrics.add(metric("Overdue", frame.rupees(totals.get("OVERDUE")), DesktopTheme.DANGER));
    metrics.add(metric("Risk", totals.get("RISK"), DesktopTheme.WARNING));
    summary.add(metrics, BorderLayout.EAST);
    Panel tabs = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 3));
    tabs.setBackground(DesktopTheme.PALE);
    for (String name : List.of("Follow-up", "Payment Posting", "Follow-up History", "Payment History", "Communications")) {
      ActionButton tab = frame.button(name, frame.currentScreenName().equals(name) ? DesktopTheme.BLUE : DesktopTheme.SOFT_BLUE);
      if (!frame.currentScreenName().equals(name)) tab.setForeground(DesktopTheme.BLUE);
      tab.addActionListener(e -> frame.openScreen(name));
      tabs.add(tab);
    }
    card.add(summary, BorderLayout.CENTER);
    card.add(tabs, BorderLayout.SOUTH);
    card.setPreferredSize(new Dimension(900, 150));
    return card;
  }

  private static Panel metric(String title, Object value, Color color) {
    Panel panel = DesktopTheme.card();
    panel.setLayout(new GridLayout(2, 1, 0, 1));
    panel.setBackground(DesktopTheme.SOFT_BLUE);
    java.awt.Label label = new java.awt.Label("  " + title);
    label.setFont(DesktopTheme.font(10, true));
    label.setForeground(DesktopTheme.MUTED);
    java.awt.Label number = new java.awt.Label("  " + (value == null ? "—" : value));
    number.setFont(DesktopTheme.font(12, true));
    number.setForeground(color);
    panel.add(label);
    panel.add(number);
    panel.setPreferredSize(new Dimension(118, 58));
    return panel;
  }
}

