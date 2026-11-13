package com.cms.desktop;

import com.cms.config.SystemParameters;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;

/** AWT sign-in screen with a branded introduction and focused credential form. */
final class LoginScreen {
  @FunctionalInterface interface SignInAction { void signIn(String username, String password, Label status); }
  private LoginScreen() {}

  static Panel render(SignInAction signInAction) {
    Panel card = DesktopTheme.card();
    card.setLayout(new BorderLayout());
    card.setPreferredSize(new Dimension(800, 420));

    Panel welcome = new Panel(new BorderLayout(0, 18));
    welcome.setBackground(DesktopTheme.NAVY);
    welcome.setPreferredSize(new Dimension(340, 420));
    Panel intro = new Panel(new GridBagLayout()); intro.setBackground(DesktopTheme.NAVY);
    GridBagConstraints introC = new GridBagConstraints();
    introC.gridx = 0; introC.fill = GridBagConstraints.HORIZONTAL; introC.weightx = 1;
    introC.insets = new Insets(0, 42, 16, 34);
    LogoMark mark = new LogoMark(); mark.setBackground(DesktopTheme.NAVY);
    Panel logoRow = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    logoRow.setBackground(DesktopTheme.NAVY);
    logoRow.add(mark);
    introC.gridy = 0; introC.insets = new Insets(34, 42, 18, 34); intro.add(logoRow, introC);
    introC.gridy++; introC.insets = new Insets(4, 42, 8, 34);
    intro.add(label(SystemParameters.get("application.short-name"), 34, true, Color.WHITE), introC);
    introC.gridy++; introC.insets = new Insets(0, 42, 12, 34);
    intro.add(label(SystemParameters.get("application.company-name"), 16, true, new Color(220, 233, 251)), introC);
    introC.gridy++; introC.insets = new Insets(0, 42, 24, 34);
    intro.add(label("Collections, made clearer.", 18, true, Color.WHITE), introC);
    introC.gridy++; introC.insets = new Insets(0, 42, 4, 34);
    intro.add(label("One workspace for customer follow-up,", 12, false, new Color(192, 211, 236)), introC);
    introC.gridy++; introC.insets = new Insets(0, 42, 12, 34);
    intro.add(label("payment posting, risk, and performance.", 12, false, new Color(192, 211, 236)), introC);
    introC.gridy++; introC.insets = new Insets(12, 42, 8, 34);
    intro.add(label("CUSTOMER WORKLIST  ·  ACCOUNT HISTORY",
        10, true, new Color(149, 192, 255)), introC);
    welcome.add(intro, BorderLayout.CENTER);
    Panel brandFoot = new Panel(new FlowLayout(FlowLayout.LEFT, 36, 10)); brandFoot.setBackground(DesktopTheme.NAVY_HOVER);
    brandFoot.add(label("Secure access for authorized company users", 10, false, new Color(206, 220, 239)));
    welcome.add(brandFoot, BorderLayout.SOUTH);
    Panel signIn = new Panel(new GridBagLayout()); signIn.setBackground(DesktopTheme.SURFACE);
    Panel form = new Panel(new GridBagLayout()); form.setBackground(DesktopTheme.SURFACE);
    GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1; c.insets = new Insets(0, 42, 0, 42);
    c.gridy = 0; c.insets = new Insets(24, 32, 5, 32);
    form.add(label("Welcome back", 23, true, DesktopTheme.INK), c);
    c.gridy++; c.insets = new Insets(0, 32, 14, 32);
    form.add(label("Sign in to open your collection workspace.", 12, false, DesktopTheme.MUTED), c);

    TextField username = new TextField(28); username.setFont(DesktopTheme.BODY);
    TextField password = new TextField(28); password.setFont(DesktopTheme.BODY); password.setEchoChar('•');
    username.setPreferredSize(new Dimension(340, 32));
    password.setPreferredSize(new Dimension(340, 32));
    styleInput(username); styleInput(password);
    c.gridy++; c.insets = new Insets(0, 32, 5, 32); form.add(label("USERNAME", 10, true, DesktopTheme.MUTED), c);
    c.gridy++; c.insets = new Insets(0, 32, 10, 32); form.add(username, c);
    c.gridy++; c.insets = new Insets(0, 32, 5, 32); form.add(label("PASSWORD", 10, true, DesktopTheme.MUTED), c);
    c.gridy++; c.insets = new Insets(0, 32, 8, 32); form.add(password, c);
    Label status = label("Authorized company users only", 11, false, DesktopTheme.MUTED);
    c.gridy++; c.insets = new Insets(0, 32, 8, 32); form.add(status, c);
    ActionButton submit = new ActionButton("Sign in"); submit.setBackground(DesktopTheme.BLUE); submit.setForeground(Color.WHITE);
    submit.setFont(DesktopTheme.font(12, true)); submit.setPreferredSize(new Dimension(280, 32));
    submit.addActionListener(e -> signInAction.signIn(username.getText(), password.getText(), status));
    c.gridy++; c.insets = new Insets(0, 32, 10, 32); form.add(submit, c);
    username.addActionListener(e -> password.requestFocus());
    password.addActionListener(e -> signInAction.signIn(username.getText(), password.getText(), status));
    c.gridy++; c.insets = new Insets(2, 32, 16, 32);
    form.add(label("CMS  ·  COLLECTIONS MANAGEMENT SYSTEM", 9, true, DesktopTheme.MUTED), c);
    GridBagConstraints formConstraints = new GridBagConstraints();
    formConstraints.anchor = GridBagConstraints.CENTER;
    formConstraints.fill = GridBagConstraints.NONE;
    signIn.add(form, formConstraints);
    card.add(welcome, BorderLayout.WEST);
    card.add(signIn, BorderLayout.CENTER);
    username.requestFocus();
    return card;
  }

  private static void styleInput(TextField input) {
    input.setForeground(DesktopTheme.INK);
    input.setBackground(DesktopTheme.SOFT_BLUE);
  }

  private static Label label(String value, int size, boolean bold, Color color) {
    Label label = new Label(value);
    label.setFont(DesktopTheme.font(size, bold));
    label.setForeground(color);
    return label;
  }
}
