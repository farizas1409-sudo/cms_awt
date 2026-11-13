# Source files changed for AWT desktop delivery

- Added `src/main/java/com/cms/desktop/CmsDesktopApplication.java` as the Java application entry point.
- Added `src/main/java/com/cms/desktop/CmsFrame.java` for AWT desktop screens and navigation.
- Reused model, service, repository, configuration, SQL setup, and Log4j 2 resources from the CMS project.
- Removed web-only Struts/JSP/servlet source from this converted project copy.
- Added Java 17 Maven jar packaging, root `Application.config`, runtime jars in `lib/`, and `run-cms.bat`.
- Updated delivery notes for a desktop application.

The source project remains separate from the original web source tree.
