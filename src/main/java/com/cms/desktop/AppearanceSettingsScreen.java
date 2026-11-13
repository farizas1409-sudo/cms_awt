package com.cms.desktop;

import java.awt.*;
import java.util.Map;

/** Shared appearance preference screen layout. */
final class AppearanceSettingsScreen {
  private AppearanceSettingsScreen() {}
  static void render(CmsFrame frame, Panel content, Map<String,Object> data) {
    Panel card = frame.gridPanel("Application appearance");
    Panel form = frame.panel(Color.WHITE); form.setLayout(new GridLayout(0, 2, 10, 8));
    Choice themes = new Choice(); Map<String,String> themeOptions = frame.mapOfStrings(data.get("themeOptions"));
    themeOptions.forEach((key, value) -> themes.add(key + " · " + value));
    select(themes, frame.text(data.get("theme")));
    TextField accent = frame.field(frame.text(data.get("accent")));
    Map<String,String> accents = frame.mapOfStrings(data.get("themeAccents"));
    themes.addItemListener(event -> accent.setText(accents.getOrDefault(key(themes), accent.getText())));
    Choice fonts = new Choice(); Map<String,String> fontOptions = frame.mapOfStrings(data.get("fontFamilyOptions"));
    fontOptions.forEach((key, value) -> fonts.add(key + " · " + value));
    select(fonts, frame.text(data.get("fontFamily")));
    TextField size = frame.field(frame.text(data.get("fontSize")));
    frame.addField(form, "Theme", themes); frame.addField(form, "Accent color", accent);
    frame.addField(form, "Font family", fonts); frame.addField(form, "Font size", size);
    ActionButton save = frame.button("Apply settings", DesktopTheme.BLUE);
    save.addActionListener(e -> frame.applyAppearance(choice(themes), accent.getText(), size.getText(), choice(fonts)));
    form.add(new Label("")); form.add(save); card.add(form, BorderLayout.CENTER);
    card.setPreferredSize(new Dimension(680, 300)); content.add(card, BorderLayout.NORTH);
  }
  private static String choice(Choice choice) {
    String selected = choice.getSelectedItem(); return selected == null ? "" : selected.split(" · ", 2)[0];
  }
  private static String key(Choice choice) { return choice(choice); }
  private static void select(Choice choice, String key) {
    for (int index = 0; index < choice.getItemCount(); index++)
      if (choice.getItem(index).startsWith(key + " · ")) { choice.select(index); return; }
  }
}

