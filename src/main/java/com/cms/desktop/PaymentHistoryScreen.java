package com.cms.desktop;

import java.awt.Panel;
import java.util.Map;

/** Payment history screen. */
final class PaymentHistoryScreen {
  private PaymentHistoryScreen() {}
  static void render(CmsFrame frame, Panel content, Map<String, Object> data) {
    CustomerHistoryLayout.render(frame, content, data, "Payment history", "Payment History", "rows", "paymentTotal");
  }
}

