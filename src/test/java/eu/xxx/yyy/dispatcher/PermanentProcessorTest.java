package eu.xxx.yyy.dispatcher;

import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.smscarrier.superrouter.activemq.ActiveMQService;
import eu.xxx.yyy.dispatcher.PermanentProcessor;

public class PermanentProcessorTest {

    private PermanentProcessor permanentProcessor;
    private static Configuration configuration;

    static {
        configuration = new XMLConfiguration();
        configuration.setProperty(PermanentProcessor.CFG_BROKER_URL, "tcp://localhost:60001");
        configuration.setProperty(PermanentProcessor.CFG_QUEUE_IN, "K_router.IN");
        configuration.setProperty(PermanentProcessor.CFG_QUEUE_OUT, "K_router.IN");
        configuration.setProperty(PermanentProcessor.CFG_PREFETCH_SIZE, "0");
        configuration.setProperty(PermanentProcessor.CFG_WAIT_TIME_ON_EMPTY_QUEUES, "100");
        configuration.setProperty(PermanentProcessor.CFG_GARBAGE_PREFIX, "garbage");
        configuration.setProperty(PermanentProcessor.CFG_LOOKUP_PREFIX, "lookup");
        configuration.setProperty(PermanentProcessor.CFG_PDA_PREFIX, "pda");
    }

    @Before
    public void setup() {
        ActiveMQService.deleteAllQueues();
    }

    @Test
    public void testProcessor() throws JMSException, InterruptedException {
        TestProducer producer = new TestProducer(configuration);
        producer.start();
        
        permanentProcessor = new PermanentProcessor(configuration);
        new Thread(permanentProcessor).start();
        
        long startTime = System.currentTimeMillis();

        while (!isTimedOut(startTime) && ActiveMQService.getQueueSizeAlternative(permanentProcessor.getInQueueNameWithoutArgs()) > 0) {
            Thread.sleep(500);
        }
        
        int routerInQueueSize = ActiveMQService.getQueueSizeAlternative(permanentProcessor.getInQueueNameWithoutArgs());
        int garbageQueueSize = ActiveMQService.getQueueSizeAlternative(permanentProcessor.getOutGarbageQueueName());
        int lookupQueueSize = ActiveMQService.getQueueSizeAlternative(permanentProcessor.getOutLookupQueueName() + "001337_42");
        int pdaQueueSize = ActiveMQService.getQueueSizeAlternative(permanentProcessor.getOutPdaQueueName() + "101");
        
        Assert.assertEquals(routerInQueueSize, 0);
        Assert.assertEquals(garbageQueueSize, 5);
        Assert.assertEquals(lookupQueueSize, 5);
        Assert.assertEquals(pdaQueueSize, 5);
    }
    
    @After
    public void tearDown() {
        permanentProcessor.requestShutdown();
        ActiveMQService.deleteAllQueues();
    }
    
    private boolean isTimedOut(long startTime) {
        return startTime + 10000 <= System.currentTimeMillis();
    }
}
