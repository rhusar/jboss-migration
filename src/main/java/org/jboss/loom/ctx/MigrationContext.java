/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.jboss.loom.ctx;

import org.jboss.loom.actions.IMigrationAction;
import org.jboss.loom.conf.AS7Config;
import org.jboss.loom.spi.IMigrator;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.impl.DefaultBatch;
import org.w3c.dom.Document;

import java.util.*;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 * Context of migration. Stores all necessary objects and information for all Migrators.
 *
 * @author Roman Jakubco
 */
public class MigrationContext {

    /**
     * Instances of IMigrator; In a form of map Class -> instance.
     */
    private Map<Class<? extends IMigrator>, IMigrator> migrators = new HashMap();


    private final Map<Class<? extends IMigrator>, MigrationData> migrationData = new HashMap();

    private final List<IMigrationAction> actions = new LinkedList();
    // TBC: Roman said there are cases when the same file is suggested for copying by multiple migrators?


    private Document as7ConfigXmlDoc;
    private Document as7ConfigXmlDocOriginal;

    // New batch holding all scripts from CliCommandAction
    private Batch batch = new DefaultBatch();
    
    private ModelControllerClient as7Client;

    private final AS7Config as7Config;
    
    private List<DeploymentInfo> deploymentInfos = new LinkedList();
            
    

    public MigrationContext( AS7Config as7Config ) {
        this.as7Config = as7Config;
    }


    //<editor-fold defaultstate="collapsed" desc="get/set">
    public Map<Class<? extends IMigrator>, IMigrator> getMigrators() { return migrators; }
    public Map<Class<? extends IMigrator>, MigrationData> getMigrationData() { return migrationData; }
    public List<IMigrationAction> getActions() { return actions; }
    public Document getAS7ConfigXmlDoc() { return as7ConfigXmlDoc; }
    public void setAS7ConfigXmlDoc(Document standaloneDoc) { this.as7ConfigXmlDoc = standaloneDoc; }
    public Document getAs7ConfigXmlDocOriginal() { return as7ConfigXmlDocOriginal; }
    public void setAs7ConfigXmlDocOriginal(Document as7XmlDocOriginal) { this.as7ConfigXmlDocOriginal = as7XmlDocOriginal; }
    public Batch getBatch() { return batch; }
    public void setAS7ManagementClient( ModelControllerClient as7Client ) { this.as7Client = as7Client; }
    public ModelControllerClient getAS7Client() { return as7Client; }

    public AS7Config getAs7Config() { return as7Config; }

    public List<DeploymentInfo> getDeployments() { return deploymentInfos; }
    public void setDeployments( List<DeploymentInfo> deploymentsDirs ) { this.deploymentInfos = deploymentsDirs; }

    //</editor-fold>
    
}// class