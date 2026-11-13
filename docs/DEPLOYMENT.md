# Desktop deployment guide

## Install prerequisites

1. Install a 64-bit Java 17 JDK and confirm `java -version` reports version 17 or later.
2. Create the Oracle CMS schema by running `database/CMS_Complete_Database_Setup.sql` as the schema owner in SQL Developer (F5). This setup resets the CMS-prefixed project data, so use a development schema.
3. Edit `Application.config` with the database URL and schema account for the machine that will run CMS.

## Start the application

On Windows, double-click `run-cms.bat`. It starts the AWT client using the included `cms-awt.jar` and `lib` dependencies. Run the command directly if you need console output:

```powershell
java -Dcms.config.path="Application.config" -cp "cms-awt.jar;lib/*" com.cms.desktop.CmsDesktopApplication
```

There is no web server, JBoss deployment, WAR, browser URL, or datasource CLI required for this desktop build. `database/cms-datasource.xml` and `configure-cms-datasource.cli` are retained as project reference artifacts for users who still maintain a separate JBoss web installation; the AWT app uses direct JDBC settings from `Application.config`.

## Build

From this directory, run `mvn clean package -DskipTests` using Maven and JDK 17. The resulting application jar is `target/cms-awt.jar`. The checked-in root `cms-awt.jar` is the packaged launcher build, and `lib/` contains runtime dependencies.

## Logs

The application loads `Application.config` before initializing Log4j 2. Logs are written to the configured directory and filename. The packaged defaults write to `logs/cms-application.log`; rollover archives go into `logs/archive`.

## Troubleshooting

- **Oracle connection refused:** confirm the listener, service name, URL, user, and password in `Application.config`.
- **Invalid user/password:** verify that the schema setup completed and that the account exists and is active.
- **Java not found:** install a JDK and add its `bin` directory to PATH.
- **Missing jar/dependency:** keep `cms-awt.jar` and all three dependency jars inside `lib/` in the delivered folder structure.
