package cz.muni.fi.jboss.migration;

import cz.muni.fi.jboss.migration.logging.SizeRotatingFileHandler;
import cz.muni.fi.jboss.migration.logging.Appender;
import cz.muni.fi.jboss.migration.logging.LoggingAS5;
import cz.muni.fi.jboss.migration.logging.CustomHandler;
import cz.muni.fi.jboss.migration.logging.Parameter;
import cz.muni.fi.jboss.migration.logging.LoggingAS7;
import cz.muni.fi.jboss.migration.logging.Category;
import cz.muni.fi.jboss.migration.logging.ConsoleHandler;
import cz.muni.fi.jboss.migration.logging.FileHandler;
import cz.muni.fi.jboss.migration.logging.PeriodicRotatingFileHandler;
import cz.muni.fi.jboss.migration.logging.Property;
import cz.muni.fi.jboss.migration.logging.AsyncHandler;
import cz.muni.fi.jboss.migration.logging.Logger;
import cz.muni.fi.jboss.migration.connectionFactories.ConnectionFactoryAS5;
import cz.muni.fi.jboss.migration.connectionFactories.ResourceAdapter;
import cz.muni.fi.jboss.migration.connectionFactories.ConnectionFactories;
import cz.muni.fi.jboss.migration.connectionFactories.ConnectionDefinition;
import cz.muni.fi.jboss.migration.connectionFactories.ConfigProperty;
import cz.muni.fi.jboss.migration.connectionFactories.ResourceAdaptersSub;
import cz.muni.fi.jboss.migration.server.Service;
import cz.muni.fi.jboss.migration.server.ConnectorAS5;
import cz.muni.fi.jboss.migration.server.SocketBinding;
import cz.muni.fi.jboss.migration.server.ServerSub;
import cz.muni.fi.jboss.migration.server.VirtualServer;
import cz.muni.fi.jboss.migration.server.ServerAS5;
import cz.muni.fi.jboss.migration.server.SocketBindingGroup;
import cz.muni.fi.jboss.migration.server.ConnectorAS7;
import cz.muni.fi.jboss.migration.security.SecurityAS7;
import cz.muni.fi.jboss.migration.security.SecurityAS5;
import cz.muni.fi.jboss.migration.security.ModuleOptionAS7;
import cz.muni.fi.jboss.migration.security.SecurityDomain;
import cz.muni.fi.jboss.migration.security.LoginModuleAS7;
import cz.muni.fi.jboss.migration.security.LoginModuleAS5;
import cz.muni.fi.jboss.migration.security.ModuleOptionAS5;
import cz.muni.fi.jboss.migration.security.ApplicationPolicy;
import cz.muni.fi.jboss.migration.dataSources.XaDatasourceAS5;
import cz.muni.fi.jboss.migration.dataSources.Driver;
import cz.muni.fi.jboss.migration.dataSources.DatasourceAS7;
import cz.muni.fi.jboss.migration.dataSources.DatasourceAS5;
import cz.muni.fi.jboss.migration.dataSources.DataSources;
import cz.muni.fi.jboss.migration.dataSources.DatasourcesSub;
import cz.muni.fi.jboss.migration.dataSources.XaDatasourceAS7;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author  Roman Jakubco
 * Date: 8/28/12
 * Time: 5:59 PM
 */
public class MigrationImpl implements Migration {


    private Integer randomSocket = 1;
    private Integer randomConnector =1 ;
    private SocketBindingGroup socketBindingGroup;
    private Set<String> drivers = new HashSet();
    private Set<String> xaDatasourceClasses = new HashSet();
    private Set<CopyMemory> copyMemories = new HashSet();
    private boolean copy;

    MigrationImpl(boolean copy){
        this.copy=copy;
    }



    @Override
    public SocketBindingGroup getSocketBindingGroup() {
        return socketBindingGroup;
    }
    @Override
    public Set<CopyMemory> getCopyMemories() {
        return copyMemories;
    }

    // TODO:  Security-Domain must reference something what exists in subsystem security...
    @Override
    public List<DatasourceAS7> datasourceMigration(Collection<DatasourceAS5> datasources) {

        List<DatasourceAS7> datasourceAS7Collection = new ArrayList();

        for (DatasourceAS5 datasourceAS5 : datasources) {
            DatasourceAS7 datasourceAS7 = new DatasourceAS7();

            drivers.add(datasourceAS5.getDriverClass());

            // Standalone elements in AS7
            datasourceAS7.setJndiName("java:jboss/datasources/" + datasourceAS5.getJndiName());
            datasourceAS7.setPoolName(datasourceAS5.getJndiName());
            datasourceAS7.setEnabled("true");
            datasourceAS7.setUseJavaContext(datasourceAS5.getUseJavaContext());
            datasourceAS7.setUrlDelimeter(datasourceAS5.getUrlDelimeter());
            datasourceAS7.setUrlSelector(datasourceAS5.getUrlSelectorStrategyClassName());
            datasourceAS7.setConnectionUrl(datasourceAS5.getConnectionUrl());
            datasourceAS7.setConnectionProperties(datasourceAS5.getConnectionProperties());
            datasourceAS7.setTransactionIsolation(datasourceAS5.getTransactionIsolation());
            datasourceAS7.setNewConnectionSql(datasourceAS5.getNewConnectionSql());

            String[] split = datasourceAS5.getDriverClass().split("\\.");
            datasourceAS7.setDriver(split[1]);

            // Elements in element <security> in AS7
            datasourceAS7.setUserName(datasourceAS5.getUserName());
            datasourceAS7.setPassword(datasourceAS5.getPassword());
            // TODO: some problems with elements in AS5(security-domain/application-managed-security/security-domain-and-application)
            datasourceAS7.setSecurityDomain(datasourceAS5.getSecurityDomain());

            // Elements in element <pool> in AS7
            datasourceAS7.setMinPoolSize(datasourceAS5.getMinPoolSize());
            datasourceAS7.setMaxPoolSize(datasourceAS5.getMaxPoolSize());
            datasourceAS7.setPrefill(datasourceAS5.getPrefill());

            // Elements in element <timeout> in AS7
            datasourceAS7.setBlockingTimeoutMillis(datasourceAS5.getBlockingTimeoutMillis());
            datasourceAS7.setIdleTimeoutMinutes(datasourceAS5.getIdleTimeoutMinutes());
            datasourceAS7.setQueryTimeout(datasourceAS5.getQueryTimeout());
            datasourceAS7.setAllocationRetry(datasourceAS5.getAllocationRetry());
            datasourceAS7.setAllocationRetryWaitMillis(datasourceAS5.getAllocationRetryWaitMillis());
            datasourceAS7.setSetTxQueryTimeout(datasourceAS5.getSetTxQueryTimeout());
            datasourceAS7.setUseTryLock(datasourceAS5.getUseTryLock());

            // Elements in element <validation> in AS7
            datasourceAS7.setCheckValidConnectionSql(datasourceAS5.getCheckValidConnectionSql());
            datasourceAS7.setValidateOnMatch(datasourceAS5.getValidateOnMatch());
            datasourceAS7.setBackgroundValidation(datasourceAS5.getBackgroundValidation());
            datasourceAS7.setExceptionSorter(datasourceAS5.getExceptionSorterClassName());
            datasourceAS7.setValidConnectionChecker(datasourceAS5.getValidConnectionCheckerClassName());
            datasourceAS7.setStaleConnectionChecker(datasourceAS5.getStaleConnectionCheckerClassName());
            // Millis represents Milliseconds?:p
            if (datasourceAS5.getBackgroundValidationMillis() != null) {
                Integer tmp = Integer.valueOf(datasourceAS5.getBackgroundValidationMillis()) / 60000;
                datasourceAS7.setBackgroundValidationMinutes(tmp.toString());
            }

            // Elements in element <statement> in AS7
            datasourceAS7.setTrackStatements(datasourceAS5.getTrackStatements());
            datasourceAS7.setSharePreparedStatements(datasourceAS5.getSharePreparedStatements());
            datasourceAS7.setQueryTimeout(datasourceAS5.getQueryTimeout());

            // Strange element use-fast-fail
            //datasourceAS7.setUseFastFail(datasourceAS5.gF);

            datasourceAS7Collection.add(datasourceAS7);
        }
        return datasourceAS7Collection;
    }

    @Override
    public List<XaDatasourceAS7> xaDatasourceMigration(Collection<XaDatasourceAS5> datasources) {
        List<XaDatasourceAS7> xaDatasourceAS7Collection = new ArrayList();
        for (XaDatasourceAS5 xaDatasourceAS5 : datasources) {
            XaDatasourceAS7 xaDatasourceAS7 = new XaDatasourceAS7();

            xaDatasourceAS7.setJndiName("java:jboss/datasources/" + xaDatasourceAS5.getJndiName());
            xaDatasourceAS7.setPoolName(xaDatasourceAS5.getJndiName());
            xaDatasourceAS7.setUseJavaContext(xaDatasourceAS5.getUseJavaContext());
            xaDatasourceAS7.setEnabled("true");


            // xa-datasource-class should be declared in drivers no in datasource.
            // xa-datasource then reference xa-datasource-class with element name
            //xaDatasourceAS7.setXaDatasourceClass(xaDatasourceAS5.getXaDatasourceClass());
            xaDatasourceClasses.add(xaDatasourceAS5.getXaDatasourceClass());
            String[] split = xaDatasourceAS5.getXaDatasourceClass().split("\\.");
            xaDatasourceAS7.setDriver(split[1]);

            xaDatasourceAS7.setXaDatasourceProperties(xaDatasourceAS5.getXaDatasourceProperties());
            xaDatasourceAS7.setUrlDelimeter(xaDatasourceAS5.getUrlDelimeter());
            xaDatasourceAS7.setUrlSelector(xaDatasourceAS5.getUrlSelectorStrategyClassName());
            xaDatasourceAS7.setTransactionIsolation(xaDatasourceAS5.getTransactionIsolation());
            xaDatasourceAS7.setNewConnectionSql(xaDatasourceAS5.getNewConnectionSql());

            // Elements in element <xa-pool> in AS7
            xaDatasourceAS7.setMinPoolSize(xaDatasourceAS5.getMinPoolSize());
            xaDatasourceAS7.setMaxPoolSize(xaDatasourceAS5.getMaxPoolSize());
            xaDatasourceAS7.setPrefill(xaDatasourceAS5.getPrefill());
            xaDatasourceAS7.setSameRmOverride(xaDatasourceAS5.getSameRM());
            xaDatasourceAS7.setInterleaving(xaDatasourceAS5.getInterleaving());
            xaDatasourceAS7.setNoTxSeparatePools(xaDatasourceAS5.getNoTxSeparatePools());

            // Elements in element <security> in AS7
            xaDatasourceAS7.setUserName(xaDatasourceAS5.getUserName());
            xaDatasourceAS7.setPassword(xaDatasourceAS5.getPassword());
            // TODO: same problem as in datasourceMigration()
            xaDatasourceAS7.setSecurityDomain(xaDatasourceAS5.getSecurityDomain());

            // Elements in element <validation> in AS7
            xaDatasourceAS7.setCheckValidConnectionSql(xaDatasourceAS5.getCheckValidConnectionSql());
            xaDatasourceAS7.setValidateOnMatch(xaDatasourceAS5.getValidateOnMatch());
            xaDatasourceAS7.setBackgroundValidation(xaDatasourceAS5.getBackgroundValidation());
            xaDatasourceAS7.setExceptionSorter(xaDatasourceAS5.getExceptionSorterClassName());
            xaDatasourceAS7.setValidConnectionChecker(xaDatasourceAS5.getValidConnectionCheckerClassName());
            xaDatasourceAS7.setStaleConnectionChecker(xaDatasourceAS5.getStaleConnectionCheckerClassName());
            //Millis represents Milliseconds?:p
            if (xaDatasourceAS5.getBackgroundValidationMillis() != null) {
                Integer tmp= Integer.valueOf(xaDatasourceAS5.getBackgroundValidationMillis()) / 60000;
                xaDatasourceAS7.setBackgroundValidationMinutes(tmp.toString());
            }

            // Elements in element <timeout> in AS7
            xaDatasourceAS7.setBlockingTimeoutMillis(xaDatasourceAS5.getBlockingTimeoutMillis());
            xaDatasourceAS7.setIdleTimeoutMinutes(xaDatasourceAS5.getIdletimeoutMinutes());
            xaDatasourceAS7.setQueryTimeout(xaDatasourceAS5.getQueryTimeout());
            xaDatasourceAS7.setAllocationRetry(xaDatasourceAS5.getAllocationRetry());
            xaDatasourceAS7.setAllocationRetryWaitMillis(xaDatasourceAS5.getAllocationRetryWaitMillis());
            xaDatasourceAS7.setSetTxQueryTimeout(xaDatasourceAS5.getSetTxQueryTimeout());
            xaDatasourceAS7.setUseTryLock(xaDatasourceAS5.getUseTryLock());
            xaDatasourceAS7.setXaResourceTimeout(xaDatasourceAS5.getXaResourceTimeout());

            // Elements in element <statement> in AS7
            xaDatasourceAS7.setPreparedStatementCacheSize(xaDatasourceAS5.getPreparedStatementCacheSize());
            xaDatasourceAS7.setTrackStatements(xaDatasourceAS5.getTrackStatements());
            xaDatasourceAS7.setSharePreparedStatements(xaDatasourceAS5.getSharePreparedStatements());


            // Strange element use-fast-fail
            //datasourceAS7.setUseFastFail(datasourceAS5.gF);

            xaDatasourceAS7Collection.add(xaDatasourceAS7);
        }
        return xaDatasourceAS7Collection;
    }




    // Resource-adapters .... maybe change name in the future?
    @Override
    public ResourceAdapter connectionFactoryMigration(ConnectionFactoryAS5 connectionFactoryAS5) {
        ResourceAdapter resourceAdapter = new ResourceAdapter();
        resourceAdapter.setJndiName(connectionFactoryAS5.getJndiName());

        if(copy){
            CopyMemory copyMemory = new CopyMemory();
            copyMemory.setName(connectionFactoryAS5.getRarName());
            copyMemory.setType("resource");
            copyMemories.add(copyMemory);
        }
        resourceAdapter.setArchive(connectionFactoryAS5.getRarName());
        // TODO:  not sure what exactly this element represents and what it is in AS5
        resourceAdapter.setTransactionSupport("XATransaction");
        ConnectionDefinition connDef = new ConnectionDefinition();
        connDef.setJndiName("java:jboss/" + connectionFactoryAS5.getJndiName());
        connDef.setPoolName(connectionFactoryAS5.getJndiName());
        connDef.setEnabled("true");
        connDef.setUseJavaContext("true");
        connDef.setEnabled("true");
        connDef.setClassName(connectionFactoryAS5.getConnectionDefinition());
        connDef.setPrefill(connectionFactoryAS5.getPrefill());

        for (ConfigProperty configProperty : connectionFactoryAS5.getConfigProperties()) {
            configProperty.setType(null);
        }
        connDef.setConfigProperties(connectionFactoryAS5.getConfigProperties());

        if (connectionFactoryAS5.getApplicationManagedSecurity() != null) {
            connDef.setApplicationManagedSecurity(connectionFactoryAS5.getApplicationManagedSecurity());
        }
        if (connectionFactoryAS5.getSecurityDomain() != null) {
            connDef.setSecurityDomain(connectionFactoryAS5.getSecurityDomain());
        }
        if (connectionFactoryAS5.getSecurityDomainAndApp() != null) {
            connDef.setSecurityDomainAndApp(connectionFactoryAS5.getSecurityDomainAndApp());
        }

        connDef.setMinPoolSize(connectionFactoryAS5.getMinPoolSize());
        connDef.setMaxPoolSize(connectionFactoryAS5.getMaxPoolSize());

        connDef.setBackgroundValidation(connectionFactoryAS5.getBackgroundValidation());
        connDef.setBackgroundValidationMillis(connectionFactoryAS5.getBackgroundValidationMillis());

        connDef.setBlockingTimeoutMillis(connectionFactoryAS5.getBlockingTimeoutMillis());
        connDef.setIdleTimeoutMinutes(connectionFactoryAS5.getIdleTimeoutMinutes());
        connDef.setAllocationRetry(connectionFactoryAS5.getAllocationRetry());
        connDef.setAllocationRetryWaitMillis(connectionFactoryAS5.getAllocationRetryWaitMillis());
        connDef.setXaResourceTimeout(connectionFactoryAS5.getXaResourceTimeout());

        List<ConnectionDefinition> connectionDefinitionCollection = new ArrayList();
        connectionDefinitionCollection.add(connDef);
        resourceAdapter.setConnectionDefinitions(connectionDefinitionCollection);


        return resourceAdapter;
    }

    @Override
    public ResourceAdaptersSub resourceAdaptersMigration(Collection<ConnectionFactories> connectionFactories){
        if(connectionFactories.isEmpty()){
            return null;
        }

        ResourceAdaptersSub resourceAdaptersSub = new ResourceAdaptersSub();
        
        Set<ResourceAdapter> resourceAdapters = new HashSet();
        for(ConnectionFactories cf: connectionFactories){
            for(ConnectionFactoryAS5 connectionFactoryAS5 : cf.getConnectionFactories()){
                 resourceAdapters.add(connectionFactoryMigration(connectionFactoryAS5));
            }
        }
        resourceAdaptersSub.setResourceAdapters(resourceAdapters);
        
        return resourceAdaptersSub;
    }


    @Override
    public DatasourcesSub datasourceSubMigration(Collection<DataSources> dataSources) {
        DatasourcesSub datasourcesSub = new DatasourcesSub();
        List<DatasourceAS7> ds7 = new ArrayList();
        List<XaDatasourceAS7> xads7 = new ArrayList();
        List<Driver> driverCollection= new ArrayList();
        List<Driver> xaDatasourceClassCollection = new ArrayList();
        for (DataSources dataSources1 : dataSources) {
            if(dataSources1.getLocalDatasourceAS5s() != null){
                ds7.addAll(datasourceMigration(dataSources1.getLocalDatasourceAS5s()));
            }
            if(dataSources1.getXaDatasourceAS5s() != null){
                xads7.addAll(xaDatasourceMigration(dataSources1.getXaDatasourceAS5s()));
            }
        }
        for(String driverClass : drivers){
            Driver driver = new Driver();
            String[] split = driverClass.split("\\.");
            driver.setDriverClass(driverClass);
            driver.setDriverName(split[1]);
            // TODO:  not sure how to set module.. only test
            driver.setDriverModule("module");
            driverCollection.add(driver);
        }
        for(String xaDatasourceClass : xaDatasourceClasses){
            Driver driver = new Driver();
            String[] split = xaDatasourceClass.split("\\.");
            driver.setXaDatasourceClass(xaDatasourceClass);
            driver.setDriverName(split[1]);
            // TODO:  not sure how to set module.. only test
            driver.setDriverModule("module");
            xaDatasourceClassCollection.add(driver);
        }

        datasourcesSub.setDatasource(ds7);
        datasourcesSub.setXaDatasource(xads7);
        datasourcesSub.setDrivers(driverCollection);
        datasourcesSub.setXaDatasourceClasses(xaDatasourceClassCollection);

        return datasourcesSub;
    }


    //yatial prva implementacia na socket binding
    public String createSocketBinding(String port, String name) {
        SocketBinding socketBinding = new SocketBinding();
        List<SocketBinding> socketBindings = new ArrayList();
        if (socketBindingGroup.getSocketBindings() == null) {
            socketBinding.setSocketName(name);
            socketBinding.setSocketPort(port);
            socketBindings.add(socketBinding);
            socketBindingGroup.setSocketBindings(socketBindings);
            return name;
        }
        for (SocketBinding socketBinding1 : socketBindingGroup.getSocketBindings()) {
            if (socketBinding1.getSocketPort().equals(port)) {
                return socketBinding1.getSocketName();
            }
        }

        socketBinding.setSocketPort(port);
        for (SocketBinding socketBinding1 : socketBindingGroup.getSocketBindings()) {
            if (socketBinding1.getSocketName().equals(name)) {
                name = name.concat(randomSocket.toString());
                randomSocket++;
            }
        }
        socketBinding.setSocketName(name);
        socketBindingGroup.getSocketBindings().add(socketBinding);
        return name;
    }


    // TODO: 
    //Basic ...
    @Override
    public ServerSub serverMigration(ServerAS5 serverAS5) {
        socketBindingGroup = new SocketBindingGroup();
        ServerSub serverSub = new ServerSub();
        List<ConnectorAS7> subConnectors = new ArrayList();
        List<VirtualServer> virtualServers = new ArrayList();
        for (Service service : serverAS5.getServices()) {
            for (ConnectorAS5 connector : service.getConnectorAS5s()) {
                ConnectorAS7 connectorAS7 = new ConnectorAS7();
                connectorAS7.setEnabled("true");
                connectorAS7.setEnableLookups(connector.getEnableLookups());
                connectorAS7.setMaxPostSize(connector.getMaxPostSize());
                connectorAS7.setMaxSavePostSize(connector.getMaxSavePostSize());
                connectorAS7.setProtocol(connector.getProtocol());
                connectorAS7.setProxyName(connector.getProxyName());
                connectorAS7.setProxyPort(connector.getProxyPort());
                connectorAS7.setRedirectPort(connector.getRedirectPort());

                // TODO: getting error in AS7 when deploying ajp connector with empty scheme or without attribute.
                // TODO:  only solution is http?
                connectorAS7.setScheme("http");


                connectorAS7.setConnectorName("connector"+randomConnector);
                randomConnector++;

                //socket-binding
                //provizorne
                if (connector.getProtocol().equals("HTTP/1.1")) {

                    if (connector.getSslEnabled() == null) {
                        connectorAS7.setSocketBinding(createSocketBinding(connector.getPort(), "http"));
                    } else {
                        if (connector.getSslEnabled().equals("true")) {
                            connectorAS7.setSocketBinding(createSocketBinding(connector.getPort(), "https"));
                        } else {
                            connectorAS7.setSocketBinding(createSocketBinding(connector.getPort(), "http"));
                        }
                    }
                } else {
                    connectorAS7.setSocketBinding(createSocketBinding(connector.getPort(), "ajp"));
                }


                try {
                    if (connector.getSslEnabled().equals("true")) {
                        connectorAS7.setScheme("https");
                        connectorAS7.setSecure(connector.getSecure());

                        connectorAS7.setSslName("ssl");
                        connectorAS7.setVerifyClient(connector.getClientAuth());
                        // TODO: problem with place of the file
                        connectorAS7.setCertificateKeyFile(connector.getKeystoreFile());


                        // TODO:  no sure which protocols can be in AS5
                        if (connector.getSslProtocol().equals("TLS")) {
                            connectorAS7.setSslProtocol("TLSv1");
                        }
                        connectorAS7.setSslProtocol(connector.getSslProtocol());

                        connectorAS7.setCiphers(connector.getCiphers());
                        connectorAS7.setKeyAlias(connectorAS7.getKeyAlias());


                        //problem s heslami.. treba to premysliet  kedze password zastresuje aj keystorePass aj truststorePass
                        connectorAS7.setPassword(connector.getKeysotrePass());
                    }


                } catch (NullPointerException ex) {

                }
                subConnectors.add(connectorAS7);
            }
            VirtualServer virtualServer = new VirtualServer();
            virtualServer.setVirtualServerName(service.getEngineName());
            virtualServer.setEnableWelcomeRoot("true");
            virtualServer.setAliasName(service.getHostNames());

            virtualServers.add(virtualServer);

        }

        //tu treba zistit ci treba zadat aj default-host napr prvy virtualny server alebo nech si to dopisu sami
        serverSub.setConnectors(subConnectors);
        serverSub.setVirtualServers(virtualServers);
        return serverSub;

    }

    //basic...Todo:
    @Override
    public LoggingAS7 loggingMigration(LoggingAS5 loggingAS5) {
        LoggingAS7 loggingAS7 = new LoggingAS7();
        List<AsyncHandler> asyncHandlers = new ArrayList();
        List<SizeRotatingFileHandler> sizeRotatingFileHandlers = new ArrayList();
        List<FileHandler> fileHandlers = new ArrayList();
        List<PeriodicRotatingFileHandler> periodicRotatingFileHandlers = new ArrayList();
        List<ConsoleHandler> consoleHandlers = new ArrayList();
        List<CustomHandler> customHandlers = new ArrayList();
        for (Appender appender : loggingAS5.getAppenders()) {
            String type = appender.getAppenderClass();
            String[] strings = type.split("\\.");

            switch (strings[strings.length - 1]) {
                case "DailyRollingFileAppender":
                    PeriodicRotatingFileHandler periodic = new PeriodicRotatingFileHandler();
                    periodic.setName(appender.getAppenderName());
                    for (Parameter parameter : appender.getParameters()) {
                        if (parameter.getParamName().equalsIgnoreCase("Append")) {
                            periodic.setAppend(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equals("File")) {
                            String value = parameter.getParamValue();
                            String[] split = value.split("\\/");

                            periodic.setFileRelativeTo("jboss.server.log.dir");
                            periodic.setPath(split[split.length-1]);
                            if(copy){
                                CopyMemory copyMemory = new CopyMemory();
                                copyMemory.setName(split[split.length-1]);
                                copyMemory.setType("log");
                                copyMemories.add(copyMemory);
                            }
                        }
                        if (parameter.getParamName().equalsIgnoreCase("DatePattern")) {
                            //basic.. neviem co s uvodzovkami
                            periodic.setSuffix(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equalsIgnoreCase("Threshold")) {
                            periodic.setLevel(parameter.getParamValue());
                            continue;
                        }
                    }
                    periodic.setFormatter(appender.getLayoutParamValue());
                    periodicRotatingFileHandlers.add(periodic);
                    break;
                case "RollingFileAppender":
                    SizeRotatingFileHandler size = new SizeRotatingFileHandler();
                    size.setName(appender.getAppenderName());
                    for (Parameter parameter : appender.getParameters()) {
                        if (parameter.getParamName().equalsIgnoreCase("Append")) {
                            size.setAppend(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equals("File")) {
                            String value = parameter.getParamValue();
                            String[] split = value.split("\\/");
                            // TODO:  problem with bad parse? same thing in DailyRotating
                            size.setRelativeTo("jboss.server.log.dir");
                            if(split.length>1){
                                size.setPath(split[split.length-1]);
                                if(copy){
                                    CopyMemory copyMemory = new CopyMemory();
                                    copyMemory.setName(split[split.length-1]);
                                    copyMemory.setType("log");
                                    copyMemories.add(copyMemory);
                                }
                            } else {

                            }
                        }
                        if (parameter.getParamName().equalsIgnoreCase("MaxFileSize")) {
                            size.setRotateSize(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equalsIgnoreCase("MaxBackupIndex")) {
                            size.setMaxBackupIndex(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equalsIgnoreCase("Threshold")) {
                            size.setLevel(parameter.getParamValue());
                            continue;
                        }
                    }
                    size.setFormatter(appender.getLayoutParamValue());
                    sizeRotatingFileHandlers.add(size);
                    break;
                case "ConsoleAppender":
                    ConsoleHandler consoleHandler = new ConsoleHandler();
                    consoleHandler.setName(appender.getAppenderName());
                    for (Parameter parameter : appender.getParameters()) {
                        if (parameter.getParamName().equalsIgnoreCase("Target")) {
                            consoleHandler.setTarget(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equalsIgnoreCase("Threshold")) {
                            consoleHandler.setLevel(parameter.getParamValue());
                            continue;
                        }
                    }
                    consoleHandler.setFormatter(appender.getLayoutParamValue());
                    consoleHandlers.add(consoleHandler);
                    break;
                case "AsyncAppender":
                    AsyncHandler asyncHandler = new AsyncHandler();
                    asyncHandler.setName(appender.getAppenderName());
                    for (Parameter parameter : appender.getParameters()) {
                        if (parameter.getParamName().equalsIgnoreCase("BufferSize")) {
                            asyncHandler.setQueueLength(parameter.getParamValue());
                            continue;
                        }
                        if (parameter.getParamName().equalsIgnoreCase("Blocking")) {
                            asyncHandler.setOverflowAction(parameter.getParamValue());
                            continue;
                        }
                    }
                    List<String> appendersRef = new ArrayList();
                    for (String ref : appender.getAppenderRefs()) {
                        appendersRef.add(ref);
                    }
                    asyncHandler.setSubhandlers(appendersRef);
                    asyncHandler.setFormatter(appender.getLayoutParamValue());
                    asyncHandlers.add(asyncHandler);
                    break;
                // TODO:  There is not such thing as FileAppender in AS5. Only sizeRotating or dailyRotating
                // TODO:  so i think that FileAppneder in AS7 is then useless?
                // THINK !!

                //case "FileAppender" :

                //basic implemenation of Custom Handler
                // TODO:  problem with module
                default:
                    CustomHandler customHandler = new CustomHandler();
                    customHandler.setName(appender.getAppenderName());
                    customHandler.setClassValue(appender.getAppenderClass());
                    List<Property> properties = new ArrayList();
                    for (Parameter parameter : appender.getParameters()) {
                        if (parameter.getParamName().equalsIgnoreCase("Threshold")) {
                            customHandler.setLevel(parameter.getParamValue());
                            continue;
                        }
                        Property property = new Property();
                        property.setName(parameter.getParamName());
                        property.setValue(parameter.getParamValue());
                        properties.add(property);

                    }
                    customHandler.setProperties(properties);
                    customHandler.setFormatter(appender.getLayoutParamValue());
                    customHandlers.add(customHandler);
                    break;
            }
        }

        loggingAS7.setAsyncHandlers(asyncHandlers);
        loggingAS7.setConsoleHandlers(consoleHandlers);
        loggingAS7.setCustomHandlers(customHandlers);
        loggingAS7.setPeriodicRotatingFileHandlers(periodicRotatingFileHandlers);
        loggingAS7.setSizeRotatingFileHandlers(sizeRotatingFileHandlers);

        List<Logger> loggers = new ArrayList();
        for (Category category : loggingAS5.getCategories()) {
            Logger logger = new Logger();
            logger.setLoggerCategory(category.getCategoryName());
            logger.setLoggerLevelName(category.getCategoryValue());
            logger.setHandlers(category.getAppenderRef());
            loggers.add(logger);
        }
        loggingAS7.setLoggers(loggers);
        /*
        TODO:problem with level, because there is relative path in AS:<priority value="${jboss.server.log.threshold}"/>
           for now only default INFO
         */
        loggingAS7.setRootLoggerLevel("INFO");
        loggingAS7.setRootLoggerHandlers(loggingAS5.getRootAppenderRefs());

        return loggingAS7;
    }

    //basic implementation
    @Override
    public SecurityAS7 securityMigration(SecurityAS5 securityAS5) {
        SecurityAS7 securityAS7 = new SecurityAS7();
        List<SecurityDomain> securityDomains = new ArrayList();

        for (ApplicationPolicy applicationPolicy : securityAS5.getApplicationPolicies()) {
            List<LoginModuleAS7> loginModules = new ArrayList();
            SecurityDomain securityDomain = new SecurityDomain();
            securityDomain.setSecurityDomainName(applicationPolicy.getApplicationPolicyName());
            securityDomain.setCacheType("default");

            for (LoginModuleAS5 loginModuleAS5 : applicationPolicy.getLoginModules()) {
                List<ModuleOptionAS7> moduleOptions = new ArrayList();
                LoginModuleAS7 loginModuleAS7 = new LoginModuleAS7();
                loginModuleAS7.setLoginModuleFlag(loginModuleAS5.getLoginModuleFlag());

                String[] split = loginModuleAS5.getLoginModule().split("\\.");

                switch (split[split.length - 1]) {
                    case "ClientLoginModule":
                        loginModuleAS7.setLoginModuleCode("Client");
                        break;
                    //*
                    case "BaseCertLoginModule":
                        loginModuleAS7.setLoginModuleCode("Certificate");
                        break;
                    case "CertRolesLoginModule":
                        loginModuleAS7.setLoginModuleCode("CertificateRoles");
                        break;
                    //*
                    case "DatabaseServerLoginModule":
                        loginModuleAS7.setLoginModuleCode("Database");
                        break;
                    case "DatabaseCertLoginModule":
                        loginModuleAS7.setLoginModuleCode("DatabaseCertificate");
                        break;
                    case "IdentityLoginModule":
                        loginModuleAS7.setLoginModuleCode("Identity");
                        break;
                    case "LdapLoginModule":
                        loginModuleAS7.setLoginModuleCode("Ldap");
                        break;
                    case "LdapExtLoginModule":
                        loginModuleAS7.setLoginModuleCode("LdapExtended");
                        break;
                    case "RoleMappingLoginModule":
                        loginModuleAS7.setLoginModuleCode("RoleMapping");
                        break;
                    case "RunAsLoginModule":
                        loginModuleAS7.setLoginModuleCode("RunAs");
                        break;
                    case "SimpleServerLoginModule":
                        loginModuleAS7.setLoginModuleCode("Simple");
                        break;
                    case "ConfiguredIdentityLoginModule":
                        loginModuleAS7.setLoginModuleCode("ConfiguredIdentity");
                        break;
                    case "SecureIdentityLoginModule":
                        loginModuleAS7.setLoginModuleCode("SecureIdentity");
                        break;
                    case "PropertiesUsersLoginModule":
                        loginModuleAS7.setLoginModuleCode("PropertiesUsers");
                        break;
                    case "SimpleUsersLoginModule":
                        loginModuleAS7.setLoginModuleCode("SimpleUsers");
                        break;
                    case "LdapUsersLoginModule":
                        loginModuleAS7.setLoginModuleCode("LdapUsers");
                        break;
                    case "Krb5loginModule":
                        loginModuleAS7.setLoginModuleCode("Kerberos");
                        break;
                    case "SPNEGOLoginModule":
                        loginModuleAS7.setLoginModuleCode("SPNEGOUsers");
                        break;
                    case "AdvancedLdapLoginModule":
                        loginModuleAS7.setLoginModuleCode("AdvancedLdap");
                        break;
                    case "AdvancedADLoginModule":
                        loginModuleAS7.setLoginModuleCode("AdvancedADldap");
                        break;
                    case "UsersRolesLoginModule":
                        loginModuleAS7.setLoginModuleCode("UsersRoles");
                        break;
                    default:
                        loginModuleAS7.setLoginModuleCode(loginModuleAS5.getLoginModule());
                }

                if (loginModuleAS5.getModuleOptions() != null) {
                    for (ModuleOptionAS5 moduleOptionAS5 : loginModuleAS5.getModuleOptions()) {
                        ModuleOptionAS7 moduleOptionAS7 = new ModuleOptionAS7();
                        moduleOptionAS7.setModuleOptionName(moduleOptionAS5.getModuleName());
                        // TODO:  problem with module option which use properties files...maybe change path only?
                        moduleOptionAS7.setModuleOptionValue(moduleOptionAS5.getModuleValue());
                        moduleOptions.add(moduleOptionAS7);
                    }
                }
                loginModuleAS7.setModuleOptions(moduleOptions);
                loginModules.add(loginModuleAS7);
            }
            securityDomain.setLoginModules(loginModules);
            securityDomains.add(securityDomain);
        }
        securityAS7.setSecurityDomains(securityDomains);
        return securityAS7;
    }


}
