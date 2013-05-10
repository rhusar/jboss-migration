## What's this?

A program for migrating configuration of JBoss AS 5-based servers to JBoss AS 7-based servers.

##### Supported source servers

 * JBoss AS 5.1+
 * JBoss EAP 5.x

##### Supported target servers:

 * JBoss AS 7.1.1+
 * JBoss EAP 6.x
 * Wildfly 8.x


## What it does

This application automates the actions necessary to migrate the configuration of the source server to the target server.
This includes:

 * Parsing the configuration XML files from the source server directory
 * Performing CLI operations against the running target server
 * Copying certain files
   * deployments
   * security users and roles definitions
   * SSH keys
   * JDBC driver jars
   * resource adapters
   * logger configuration files

Various parts of configuration is covered by so-called "migrators", e.g.

 * DatasourceMigrator
 * LoggingMigrator
 * ResAdapterMigrator
 * SecurityMigrator
 * ServerMigrator for JBoss Web
 * etc...


If the migration fails (invalid input, missing files, wrong configuration in AS5 etc.),
then the application rolls back all changes made.


## Usage

The application is meant to be run on a fresh installation of the target server.

Currently, **the target server must be running** during the migration.
The future versions of the app will be able to start the server for you.

Currently, the **JBOSS_HOME** system property **must NOT be set**. [MIGR-84](https://issues.jboss.org/browse/MIGR-84)

To run the app, use the distribution jar as follows:


    java -jar AsMigrator.jar [<option>, ...] [as5.dir=]<as5.dir> [as7.dir=]<as7.dir>

       <as5.dir>   is expected to contain path to AS 5 or EAP 5 home directory, i.e. the one with server/ subdirectory.

       <as7.dir>   is expected to contain path to AS 7 or EAP 6 home directory, i.e. the one with jboss-modules.jar.

 Options:

    as5.profile=<name>
        Path to AS 5 profile.
        Default: "default"

    as7.confPath=<path> 
        Path to AS 7 config file.
        Default: "standalone/configuration/standalone.xml"

    conf.<module>.<property>=<value> := Module-specific options.
        <module> := Name of one of modules. E.g. datasource, jaas, security, ...
        <property> := Name of the property to set. Specific per module. May occur multiple times.

  For the full list of options please see the project [wiki pages](https://github.com/OndraZizka/jboss-migration/wiki).