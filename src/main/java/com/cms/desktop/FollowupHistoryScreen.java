package com.cms.desktop;

import java.awt.Panel;
import java.util.Map;

/** Follow-up history screen. */
final class FollowupHistoryScreen {
  private FollowupHistoryScreen() {}
  static void render(CmsFrame frame, Panel content, Map<String, Object> data) {
    CustomerHistoryLayout.render(frame, content, data, "Follow-up history", "Follow-up History", "rows", "followupTotal");
  }
}

