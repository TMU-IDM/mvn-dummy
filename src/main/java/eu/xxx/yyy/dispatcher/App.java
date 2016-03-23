package eu.xxx.yyy.dispatcher;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    private static volatile Configuration configuration;
    private static int neededArgsCnt = 2;
    private String dispatcherId;
    
    private List<PermanentProcessor> processorList = new LinkedList<PermanentProcessor>();

    public static void main(String[] args) {
        if (args.length != neededArgsCnt) {
            throw new RuntimeException(neededArgsCnt + " arguments needed: configName dispatcherId");
        }
        
        String configName = args[0];
        String dispatcherId = args[1];

        if (logger.isDebugEnabled()) {
            logger.debug("Starting dispatcher '" + dispatcherId + "'");
        }
        
        App app = new App(configName, dispatcherId);
        app.startUp();
        
        addShutDownHook(app);
    }

    public App(String configName, String dispatcherId) {
        this.dispatcherId = dispatcherId;

        Thread.currentThread().setName("controller");
        System.setProperty("dispatcher.id", dispatcherId);
        
        if (logger.isInfoEnabled()) {
            logger.info("smsc Dispatcher: v3.1.0.0");
            logger.info("(c) 2014 by interactive digital media");
        }
        
        initConfiguration(configName);
    }
    
    @SuppressWarnings("static-access")
    public App(boolean forTestCaseOnly, Configuration configuration) {
        this.configuration = configuration;
        dispatcherId = "42";
    }
    
    private void initConfiguration(String configName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing configuration...");
        }
        try {
            XMLConfiguration.setDefaultListDelimiter('|');
            configuration = new XMLConfiguration();
            ((XMLConfiguration)configuration).setDelimiterParsingDisabled(false);
            ((XMLConfiguration)configuration).load(configName);
            ((XMLConfiguration)configuration).setReloadingStrategy(new FileChangedReloadingStrategy());
            if (logger.isWarnEnabled()) {
                logger.warn("from config: feature.type" + configuration.getString("feature.type"));
            }
        } catch (ConfigurationException e) {
            logger.error("Error while reading config", e);
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public void startUp() {
        startPermanentProcessor();
    }
    
    private void startPermanentProcessor() {
        PermanentProcessor permanentProcessor = new PermanentProcessor(configuration);
        permanentProcessor.init();
        processorList.add(permanentProcessor);
        
        Thread t = new Thread(permanentProcessor);
        t.start();
    }
    
    public void shutdown() {
        if (logger.isDebugEnabled()) {
            logger.debug("Shutting down PermanentProcessors...");
        }
        for (PermanentProcessor p : processorList) {
            p.requestShutdown();
        }
    }

    private static void addShutDownHook(final App app) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                if (logger.isWarnEnabled()) {
                    logger.warn("Kill signal received, starting shutdown...");
                }
                app.shutdown();
                if (logger.isWarnEnabled()) {
                    logger.warn("Shutdown done, going down.");
                }
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                String currentThreadName = Thread.currentThread().getName();
                for (Thread thread : threadSet) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Killing : " + thread.getName() + " " + thread.getClass().getName());
                    }
                    if (!thread.getName().equals(currentThreadName)) {
                        thread.stop();
                        // thread.interrupt(); // will not work.
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Shutdown successful");
                }
                System.exit(0);
            }
        });
    }
    
    public String getDispatcherId() {
        return dispatcherId;
    }


}
