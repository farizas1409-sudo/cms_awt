package com.cms.desktop;

import java.awt.*;
import java.util.List;
import java.util.Map;

/** Role catalog and permission matrix screen layout. */
final class RolesPermissionsScreen {
  private RolesPermissionsScreen() {}

  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    List<Map<String,Object>> roles = frame.listOfMaps(data.get("roles"));
    List<Map<String,Object>> permissions = frame.listOfMaps(data.get("permissions"));
    Panel root = frame.panel(DesktopTheme.PALE); root.setLayout(new BorderLayout(0, 12));
    Panel toolbar = DesktopTheme.card(); toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
    ActionButton create = frame.button("+ Create role", DesktopTheme.BLUE); create.addActionListener(e -> frame.createRoleDialog());
    ActionButton edit = frame.button("Edit role permissions", DesktopTheme.BLUE); edit.addActionListener(e -> frame.showRolePermissions(data));
    toolbar.add(create); toolbar.add(edit); root.add(toolbar, BorderLayout.NORTH);
    Panel rolesGrid = frame.tableSection("Application roles", roles, List.of("NAME", "ACTIVE"));
    Panel catalog = frame.tableSection("Permission catalog", permissions, List.of("LABEL"));
    rolesGrid.setPreferredSize(new Dimension(240, 150));
    catalog.setPreferredSize(new Dimension(480, 150));

    Panel upper = frame.panel(DesktopTheme.PALE);
    upper.setLayout(new GridBagLayout());
    GridBagConstraints column = new GridBagConstraints();
    column.gridy = 0;
    column.weighty = 1;
    column.fill = GridBagConstraints.BOTH;
    column.gridx = 0;
    column.weightx = 1;
    column.insets = new Insets(0, 0, 12, 12);
    upper.add(rolesGrid, column);
    column.gridx = 1;
    column.weightx = 2;
    column.insets = new Insets(0, 0, 12, 0);
    upper.add(catalog, column);

    Panel center = frame.panel(DesktopTheme.PALE);
    center.setLayout(new BorderLayout());
    center.add(upper, BorderLayout.NORTH);
    center.add(roleMatrix(frame, data, roles, permissions), BorderLayout.CENTER);
    root.add(center, BorderLayout.CENTER); content.add(root, BorderLayout.CENTER);
  }

  private static Panel roleMatrix(CmsFrame frame, Map<String,Object> data,
      List<Map<String,Object>> roles, List<Map<String,Object>> permissions) {
    Panel section = frame.gridPanel("Role access matrix");
    Panel matrix = new Panel(new GridLayout(permissions.size() + 1, roles.size() + 1, 1, 1));
    matrix.setBackground(DesktopTheme.BORDER);
    matrix.add(cell(frame, "Permission", true));
    for (Map<String,Object> role : roles) matrix.add(cell(frame, frame.text(role.get("NAME")), true));
    Map<String,Object> allMatrices = frame.mapOf(data.get("permissionMatrix"));
    for (Map<String,Object> permission : permissions) {
      String code = frame.text(permission.get("CODE"));
      matrix.add(cell(frame, frame.text(permission.get("LABEL")), false));
      for (Map<String,Object> role : roles) {
        Map<String,Object> granted = frame.mapOf(allMatrices.get(frame.text(role.get("ID"))));
        matrix.add(permissionMark(Boolean.TRUE.equals(granted.get(code))));
      }
    }
    section.add(matrix, BorderLayout.CENTER);
    return section;
  }

  private static Canvas permissionMark(boolean granted) {
    Canvas mark = new Canvas() {
      @Override
      public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
          g.setColor(granted ? DesktopTheme.SUCCESS : DesktopTheme.DANGER);
          int x = getWidth() / 2;
          int y = getHeight() / 2;
          if (granted) {
            g.drawPolyline(new int[] {x - 5, x - 1, x + 6}, new int[] {y, y + 4, y - 4}, 3);
          } else {
            g.drawLine(x - 4, y - 4, x + 4, y + 4);
            g.drawLine(x - 4, y + 4, x + 4, y - 4);
          }
        } finally {
          g.dispose();
        }
      }
    };
    mark.setBackground(DesktopTheme.SURFACE);
    mark.getAccessibleContext().setAccessibleName(granted ? "Granted" : "Not granted");
    return mark;
  }

  private static Label cell(CmsFrame frame, String value, boolean header) {
    Label cell = frame.label(value, 11, true, header ? DesktopTheme.WHITE : DesktopTheme.INK);
    cell.setBackground(header ? DesktopTheme.NAVY : DesktopTheme.HEADER);
    return cell;
  }
}

