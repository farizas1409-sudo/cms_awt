package com.cms.desktop;

import com.cms.config.SystemParameters;
import java.awt.EventQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Starts the CMS desktop client. The AWT presentation delegates to the existing service layer. */
public final class CmsDesktopApplication {
  private CmsDesktopApplication() {}

  public static void main(String[] args) {
    SystemParameters.initialize();
    Logger log = LogManager.getLogger(CmsDesktopApplication.class);
    log.info("Starting CMS AWT desktop application");
    EventQueue.invokeLater(() -> new CmsFrame().showLogin());
  }
}

