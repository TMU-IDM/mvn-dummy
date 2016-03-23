package eu.xxx.yyy.dispatcher;

import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.smscarrier.superrouter.activemq.PooledConnectionManager;
import eu.smscarrier.superrouter.messages.QMessage;
import eu.smscarrier.superrouter.messages.RouterInMessage;

public abstract class BaseProcessor implements Runnable, ExceptionListener {

    private static final Logger logger = LoggerFactory.getLogger(BaseProcessor.class);

    public static final String CFG_WAIT_TIME_ON_EMPTY_QUEUES = "dispatcher.processor.waittime";
    public static final String CFG_BROKER_URL = "amqp.brokerurl";
    public static final String CFG_QUEUE_IN = "amqp.queue.in";
    public static final String CFG_QUEUE_OUT = "amqp.queue.out";
    public static final String CFG_LOOKUP_PREFIX = "amqp.queue.prefixes.lookupPrefix";
    public static final String CFG_PDA_PREFIX = "amqp.queue.prefixes.pdaPrefix";
    public static final String CFG_GARBAGE_PREFIX = "amqp.queue.prefixes.garbagePrefix";
    public static final String CFG_PREFETCH_SIZE = "amqp.queue.prefetchSize";

    protected String inQueueName;
    protected String inQueueNameWithoutArgs;
    protected String outQueueName;
    protected String outPdaQueueName;
    protected String outLookupQueueName;
    protected String outGarbageQueueName;

    protected volatile boolean shutdownRequested = false;
    protected Configuration configuration;

    private long waitTime = 1L;

    protected PooledConnectionManager pooledConnectionManager;
    protected Session session;
    private MessageConsumer consumer;
    protected MessageProducer producer;

    protected QMessage qMessageIn;
    protected Message currentMessageIn;

    protected BaseProcessor(Configuration configuration) {
        this.configuration = configuration;
        initQueueNames();
        initProcessor();
        initActiveMQ();
        init();
        if (logger.isInfoEnabled()) {
            logger.info("Initialization complete");
        }
    }

    private void initQueueNames() {
        if (logger.isInfoEnabled()) {
            logger.info("Initializing queuenames...");
        }
        outQueueName = configuration.getString(CFG_QUEUE_OUT);
        inQueueNameWithoutArgs = configuration.getString(CFG_QUEUE_IN);
        inQueueName = inQueueNameWithoutArgs + "?consumer.prefetchSize=" + configuration.getString(CFG_PREFETCH_SIZE, "0");
        outPdaQueueName = outQueueName + "_" + configuration.getString(CFG_PDA_PREFIX, "pda") + "_";
        outLookupQueueName = outQueueName + "_" + configuration.getString(CFG_LOOKUP_PREFIX, "lookup") + "_";
        outGarbageQueueName = outQueueName + "_" + configuration.getString(CFG_GARBAGE_PREFIX, "garbage");
    }

    private void initProcessor() {
        if (logger.isInfoEnabled()) {
            logger.info("Initializing processor...");
        }
        waitTime = configuration.getLong(CFG_WAIT_TIME_ON_EMPTY_QUEUES);
    }

    private void initActiveMQ() {
        if (logger.isInfoEnabled()) {
            logger.info("Initializing ActiveMQ...");
        }
        try {
            initConnection();
            initConsumer();
        } catch (JMSException e) {
            if (logger.isErrorEnabled()) {
                logger.error("JMSException in Constructor: " + e.getErrorCode(), e);
            }
        }
    }

    private void initConnection() throws JMSException {
        pooledConnectionManager = PooledConnectionManager.getInstance(configuration);
        boolean transacted = false;
        int ackMode = Session.CLIENT_ACKNOWLEDGE;
        session = pooledConnectionManager.newSession(transacted, ackMode);
    }

    protected void init() {

    }

    private void initConsumer() throws JMSException {
        Destination destination = session.createQueue(configuration.getString(CFG_QUEUE_IN));
        consumer = session.createConsumer(destination);
    }

    protected QMessage getInboundMessage() {
        QMessage result = null;
        try {

            Message message = consumer.receive(waitTime);

            if (message != null) {
                currentMessageIn = message;

                if (message instanceof ObjectMessage) {
                    ObjectMessage objectMessage = (ObjectMessage) message;
                    result = new QMessage(objectMessage);
                    if (logger.isDebugEnabled()) {
                        @SuppressWarnings("unused")
                        String text = (String) result.getProperties().get("id");
                        // logger.debug("currentMessage = " + text);
                    }
                    Object inObj = result.getPayload();

                    if (inObj instanceof RouterInMessage) {

                    } else {

                    }
                }
            }
        } catch (JMSException e) {
            if (logger.isErrorEnabled()) {
                logger.error("JMSException in run: " + e.getErrorCode(), e);
            }
        }
        return result;
    }

    public void requestShutdown() {
        shutdownRequested = true;
    }

    @Override
    public synchronized void onException(JMSException exception) {
        if (logger.isErrorEnabled()) {
            logger.error("JMS Exception occured - ExceptionListener. Shutting down client.");
        }
    }

    /**
     * @return the inQueueName
     */
    public String getInQueueName() {
        return inQueueName;
    }

    /**
     * @return the inQueueNameWithoutArgs
     */
    public String getInQueueNameWithoutArgs() {
        return inQueueNameWithoutArgs;
    }

    /**
     * @return the outQueueNamePrefix
     */
    public String getOutQueueName() {
        return outQueueName;
    }

    /**
     * @return the outPdaQueueName
     */
    public String getOutPdaQueueName() {
        return outPdaQueueName;
    }

    /**
     * @return the outLookupQueueName
     */
    public String getOutLookupQueueName() {
        return outLookupQueueName;
    }

    /**
     * @return the outGarbageQueueName
     */
    public String getOutGarbageQueueName() {
        return outGarbageQueueName;
    }

}
