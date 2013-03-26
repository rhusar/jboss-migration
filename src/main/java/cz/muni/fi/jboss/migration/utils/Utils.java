package cz.muni.fi.jboss.migration.utils;

import cz.muni.fi.jboss.migration.Configuration;
import cz.muni.fi.jboss.migration.RollbackData;
import cz.muni.fi.jboss.migration.ex.CliScriptException;
import cz.muni.fi.jboss.migration.ex.CopyException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utils class containing helping classes
 *
 * @author Roman Jakubco
 */
public class Utils {

    
    /**
     * Method for testing if given string is null or empty and if it is then CliScriptException is thrown with given message
     *
     * @param string string for testing
     * @param errMsg message for exception
     * @param name   name of property of tested value
     * @throws CliScriptException if tested string is empty or null
     */
    public static void throwIfBlank(String string, String errMsg, String name) throws CliScriptException {
        if ((string == null) || (string.isEmpty())) {
            throw new CliScriptException(name + errMsg);
        }
    }

    
    /**
     * Helping method for writing help.
     */
    public static void writeHelp() {
        System.out.println();
        System.out.println(" Usage:");
        System.out.println();
        System.out.println("    java -jar AsMigrator.jar [<option>, ...] [as5.dir=]<as5.dir> [as7.dir=]<as7.dir>");
        System.out.println();
        System.out.println(" Options:");
        System.out.println();
        System.out.println("    as5.profile=<name>");
        System.out.println("        Path to AS 5 profile.");
        System.out.println("        Default: \"default\"");
        System.out.println();
        System.out.println("    as7.confPath=<path> ");
        System.out.println("        Path to AS 7 config file.");
        System.out.println("        Default: \"standalone/configuration/standalone.xml\"");
        System.out.println();
        System.out.println("    conf.<module>.<property>=<value> := Module-specific options.");
        System.out.println("        <module> := Name of one of modules. E.g. datasource, jaas, security, ...");
        System.out.println("        <property> := Name of the property to set. Specific per module. " +
                "May occur multiple times.");
        System.out.println();
    }

    /**
     * Utils class for finding name of jar file containing class from logging configuration.
     *
     * @param className  name of the class which must be found
     * @param dirAS5   AS5 home dir
     * @param profileAS5  name of AS5 profile
     * @return  name of jar file which contains given class
     * @throws FileNotFoundException if the jar file is not found
     * 
     * TODO: This would cause false positives - e.g. class = org.Foo triggered by org/Foo/Blah.class .
     */
    public static String findJarFileWithClass(String className, String dirAS5, String profileAS5) throws FileNotFoundException, IOException {
        
        String classFilePath = className.replace(".", "/");
        
        // First look for jar file in lib directory in given AS5 profile
        File dir = Utils.createPath(dirAS5, "server", profileAS5, "lib");
        File jar = lookForJarWithAClass( dir, classFilePath );
        if( jar != null )
            return jar.getName();
        
        // If not found in profile's lib directory then try common/lib folder (common jars for all profiles)
        dir = Utils.createPath(dirAS5, "common", profileAS5, "lib");
        jar = lookForJarWithAClass( dir, classFilePath );
        if( jar != null )
            return jar.getName();
                            
        throw new FileNotFoundException("Cannot find jar file which contains class: " + className);
    }
    
    private static File lookForJarWithAClass( File dir, String classFilePath ) throws IOException {
        //SuffixFileFilter sf = new SuffixFileFilter(".jar");
        //List<File> list = (List<File>) FileUtils.listFiles(dir, sf, FileFilterUtils.makeCVSAware(null));
        Collection<File> list = FileUtils.listFiles(dir, new String[]{".jar"}, true);

        for( File file : list ) {
            JarFile jarFile = new JarFile(file);
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if( ( ! entry.isDirectory() ) && entry.getName().contains(classFilePath)) {

                    // Assuming that jar file contains some package with class (common Java practice)
                    //return  StringUtils.substringAfterLast(file.getPath(), "/");
                    return file;
                }
            }
        }
        return null;
    }

    
    /**
     * Searching for file, which is represented as RollbackData in the application, in given directory
     *
     * @param rollData object representing file for search
     * @param dir directory for searching
     * @return list of found files
     */
    public static List<File> searchForFile(RollbackData rollData, File dir) {
        NameFileFilter nff;

        if (rollData.getType().equals(RollbackData.Type.DRIVER)) {
            final String name = rollData.getName();

            nff = new NameFileFilter(name) {
                @Override
                public boolean accept(File file) {
                    return file.getName().contains(name) && file.getName().contains("jar");
                }
            };
        } else {
            nff = new NameFileFilter(rollData.getName());
        }

        List<File> list = (List<File>) FileUtils.listFiles(dir, nff, FileFilterUtils.makeCVSAware(null));

        // One more search for driver jar. Other types of rollbackData just return list.
        if(rollData.getType().equals(RollbackData.Type.DRIVER)) {

            // For now only expecting one jar for driver. Pick the first one.
            if (list.isEmpty()) {

                // Special case for freeware jdbc driver jdts.jar
                if (rollData.getAltName() != null) {
                    final String altName = rollData.getAltName();

                    nff = new NameFileFilter(altName) {
                        @Override
                        public boolean accept(File file) {
                            return file.getName().contains(altName) && file.getName().contains("jar");
                        }
                    };
                    List<File> altList = (List<File>) FileUtils.listFiles(dir, nff,
                            FileFilterUtils.makeCVSAware(null));

                    return altList;
                }
            }
        }

        return list;
    }

    
    /**
     *  Builds up a File object with path consisting of given components.
     */
    public static File createPath( String parent, String child, String ... more) {
        File file = new File(parent, child);
        for( String component : more ) {
            file = new File(file, component);
        }
        return file;
    }

    
    /**
     *  Creates a new default document builder.
     */
    public static DocumentBuilder createXmlDocumentBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        try {
            return dbf.newDocumentBuilder();
        }
        catch( ParserConfigurationException ex ){
            throw new RuntimeException(ex); // Tunnel
        }
    }

    
    
}// class