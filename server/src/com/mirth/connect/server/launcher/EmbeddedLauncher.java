package com.mirth.connect.server.launcher;

import com.mirth.connect.server.Mirth;
import com.mirth.connect.server.extprops.ExtensionStatuses;
import com.mirth.connect.server.extprops.LoggerWrapper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A launcher that starts OpenIntegrationEngine in embedded mode.
 * Kiran Ayyagari (kiran@sereen.io)
 */
public class EmbeddedLauncher extends MirthLauncher {
    /** the root directory of a server installation */
    private File baseDir;

    private List<URL> extensionUrls = new ArrayList<>();

    public EmbeddedLauncher(File baseDir) {
        this.baseDir = baseDir;
    }

    public void addExtension(File extensionDir) throws Exception {
        String version = readVersionNumber(baseDir);
        getExtensionClasspathUrls(extensionDir, version);
    }

    public EmbeddedEngine launch(Properties overridingProperties) throws Exception {
        List<URL> launcherJarUrls = new ArrayList<>();

        // Always add log4j
        for (String log4jJar : LOG4J_JAR_FILES) {
            launcherJarUrls.add(new File(baseDir, log4jJar).toURI().toURL());
        }

        //addServerLauncherLibJarsToClasspath(baseDir, launcherJarUrls);

        ParentFirstClassLoader mirthLauncherClassLoader = new ParentFirstClassLoader(launcherJarUrls.toArray(new URL[launcherJarUrls.size()]), ClassLoader.getSystemClassLoader());

        String includeCustomLib = null;//serverProperties.getProperty(PROPERTY_INCLUDE_CUSTOM_LIB);
        String basedirPath = baseDir.getAbsolutePath() + File.separator;
        ManifestFile mirthServerJar = new ManifestFile(basedirPath + "server-lib/mirth-server.jar");
        //ManifestFile mirthClientCoreJar = new ManifestFile(basedirPath + MIRTH_CLIENT_CORE_JAR_RELATIVE_PATH);
        ManifestDirectory serverLibDir = new ManifestDirectory(basedirPath + "server-lib");
        ManifestDirectory confDir = new ManifestDirectory(basedirPath + "conf");
        //serverLibDir.setExcludes(new String[] { "mirth-client-core.jar" });

        List<ManifestEntry> manifestList = new ArrayList<ManifestEntry>();
        manifestList.add(mirthServerJar);
        //manifestList.add(mirthClientCoreJar);
        manifestList.add(serverLibDir);
        manifestList.add(confDir);

        // We want to include custom-lib if the property isn't found, or if it equals "true"
        if (includeCustomLib == null || Boolean.valueOf(includeCustomLib)) {
            manifestList.add(new ManifestDirectory(basedirPath + "custom-lib"));
        }

        ManifestEntry[] manifest = manifestList.toArray(new ManifestEntry[manifestList.size()]);

        String currentVersion = readVersionNumber(baseDir);

        List<URL> classpathUrls = new ArrayList<>();
        classpathUrls.addAll(extensionUrls);

        Thread.currentThread().setContextClassLoader(mirthLauncherClassLoader);
        System.setProperty("log4j2.enableThreadlocals", "false");

        logger = new LoggerWrapper(mirthLauncherClassLoader.loadClass("org.apache.logging.log4j.LogManager").getMethod("getLogger", Class.class).invoke(null, EmbeddedLauncher.class));


        Properties serverProperties = readProperties(baseDir);
        if(overridingProperties != null) {
            serverProperties.putAll(overridingProperties);
        }

        createAppdataDir(baseDir, serverProperties);
        ExtensionStatuses.init(serverProperties);

        addManifestToClasspath(manifest, classpathUrls);
        addExtensionsToClasspath(baseDir, classpathUrls, currentVersion);
        ParentFirstClassLoader classLoader = new ParentFirstClassLoader(classpathUrls.toArray(new URL[classpathUrls.size()]), mirthLauncherClassLoader);

        try {
            uninstallPendingExtensions(baseDir);
            installPendingExtensions(baseDir);
        } catch (Exception e) {
            logger.error("Error uninstalling or installing pending extensions.", e);
        }

        Class<?> mirthClass = classLoader.loadClass("com.mirth.connect.server.Mirth");
        Constructor<?> constructor = mirthClass.getConstructor(Properties.class);
        Thread mirthThread = (Thread) constructor.newInstance(overridingProperties);
        mirthThread.setContextClassLoader(classLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
        System.out.println("current thread classloader " + classLoader);
        mirthThread.start();

        //Mirth mirthThread = new Mirth(overridingProperties);
        //Thread oieThread = launch(baseDir, overridingProperties, extensionUrls, currentLoader);
        return new EmbeddedEngine(mirthThread);
//        return (Mirth) mirthThread;
    }

    public static void main(String[] args) throws Exception {
        EmbeddedLauncher el = new EmbeddedLauncher(new File("/Users/dbugger/projects/zen/engine/server/setup"));
        EmbeddedEngine ee = el.launch(null);
        System.out.println(ee);
        ee.shutdown();
    }
}
