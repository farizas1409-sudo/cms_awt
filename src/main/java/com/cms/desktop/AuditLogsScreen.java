package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Administrative audit trail screen layout. */
final class AuditLogsScreen {
  private AuditLogsScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    List<Map<String,Object>> rows = frame.listOfMaps(data.get("rows"));
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    Panel filters = frame.gridPanel("Find recorded activity");
    Panel fields = frame.panel(Color.WHITE); fields.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
    TextField from = frame.field("YYYY-MM-DD"), to = frame.field("YYYY-MM-DD");
    from.setText(frame.auditFromValue()); to.setText(frame.auditToValue());
    from.setColumns(12); to.setColumns(12);
    ActionButton search = frame.button("Filter activity", DesktopTheme.BLUE);
    search.addActionListener(e -> frame.filterAudit(from.getText(), to.getText()));
    fields.add(frame.label("From", 12, true, DesktopTheme.INK));
    fields.add(frame.dateField(from, "from date"));
    fields.add(frame.label("To", 12, true, DesktopTheme.INK));
    fields.add(frame.dateField(to, "to date"));
    fields.add(search);
    filters.add(fields, BorderLayout.CENTER); root.add(filters, BorderLayout.NORTH);
    root.add(frame.tableSection("Administrative activity · " + data.getOrDefault("auditTotal", rows.size()) + " events",
        rows, List.of("CREATED_AT", "USERNAME", "ACTION", "ENTITY", "DETAILS", "STATUS")), BorderLayout.CENTER);
    content.add(root, BorderLayout.CENTER);
  }
}

