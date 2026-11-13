package com.cms.desktop;

import java.awt.*;

/** Change-password screen layout. */
final class ChangePasswordScreen {
  private ChangePasswordScreen() {}
  static void render(CmsFrame frame, Panel content) {
    Panel card = frame.gridPanel("Change your password");
    Panel form = frame.panel(Color.WHITE); form.setLayout(new GridBagLayout());
    TextField current = frame.field(""); current.setEchoChar('\u2022');
    TextField next = frame.field(""); next.setEchoChar('\u2022');
    addPasswordField(frame, form, 0, "Current password", current);
    addPasswordField(frame, form, 1, "New password", next);
    ActionButton save = frame.button("Update password", DesktopTheme.BLUE);
    save.addActionListener(e -> frame.updatePassword(current.getText(), next.getText()));
    GridBagConstraints button = new GridBagConstraints();
    button.gridx = 1; button.gridy = 2; button.weightx = 1; button.fill = GridBagConstraints.HORIZONTAL;
    button.insets = new Insets(2, 5, 2, 5); form.add(save, button);
    card.add(form, BorderLayout.CENTER);
    card.setPreferredSize(new Dimension(460, 150));
    Panel aligned = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    aligned.setBackground(DesktopTheme.PALE);
    aligned.add(card);
    content.add(aligned, BorderLayout.NORTH);
  }

  private static void addPasswordField(CmsFrame frame, Panel form, int row, String label, TextField field) {
    GridBagConstraints text = new GridBagConstraints();
    text.gridx = 0; text.gridy = row; text.weightx = 0; text.anchor = GridBagConstraints.WEST;
    text.insets = new Insets(3, 5, 3, 8);
    form.add(frame.label(label, 12, true, DesktopTheme.INK), text);
    GridBagConstraints input = new GridBagConstraints();
    input.gridx = 1; input.gridy = row; input.weightx = 1; input.fill = GridBagConstraints.HORIZONTAL;
    input.insets = new Insets(3, 5, 3, 5); field.setColumns(22);
    form.add(field, input);
  }
}

