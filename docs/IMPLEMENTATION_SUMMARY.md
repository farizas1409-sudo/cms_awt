# AWT conversion summary

The CMS web presentation was converted into a Java 17 desktop application using `java.awt` controls and windows. Struts actions, JSP pages, servlet startup, and server deployment are excluded from the desktop source.

The existing model, service workflows, authorization checks, repositories, Oracle SQL, `Application.config`, and Log4j 2 logging are retained. `CmsDesktopApplication` initializes settings and starts the desktop frame. `CmsFrame` renders role-sensitive navigation, customer workflows, and administrative/report screens, then delegates data operations to the existing service classes.

The original project at `C:\Users\HomePC\Documents\ChatGPT\CMS-AWT` was not modified. This desktop project is the converted copy.
