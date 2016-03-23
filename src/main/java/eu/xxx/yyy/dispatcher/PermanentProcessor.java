package eu.xxx.yyy.dispatcher;

import java.util.HashMap;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.smscarrier.superrouter.domain.SuperrouterConstants;
import eu.smscarrier.superrouter.messages.QMessage;
import eu.xxx.yyy.dispatcher.util.DispatcherUtils;

public class PermanentProcessor extends BaseProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PermanentProcessor.class);

    private Map<String, MessageProducer> allProducers = new HashMap<String, MessageProducer>();

    public PermanentProcessor(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void run() {
        while (!shutdownRequested) {
            qMessageIn = getInboundMessage();
            if (qMessageIn != null) {
                if (!isValidMessage(qMessageIn)) {
                    handleInvalidMessage(qMessageIn);
                } else {
                    doProcess(qMessageIn);
                }
            }
        }
    }

    private boolean isValidMessage(QMessage qMessage) {
        return isPdaMessage(qMessage) || isLookupMessage(qMessage);
    }

    private boolean isPdaMessage(QMessage qMessage) {
        // has to be object, casting a property that is null resp. not available to string directly would raise NPE
        Object property = qMessage.getProperties().get(SuperrouterConstants.KEY_PROP_PDA_ID);
        return property != null && !String.valueOf(property).isEmpty();

    }

    private boolean isLookupMessage(QMessage qMessage) {
        // has to be object, casting a property that is null resp. not available to string directly would raise NPE
        Object lookUpUrlIdProperty = qMessage.getProperties().get(SuperrouterConstants.KEY_PROP_LOOKUP_URL_ID);
        Object lookUpPrioProperty = qMessage.getProperties().get(SuperrouterConstants.KEY_PROP_LOOKUP_PRIO);
        return lookUpPrioProperty != null 
                && lookUpUrlIdProperty != null
                && !String.valueOf(lookUpPrioProperty).isEmpty() 
                && !String.valueOf(lookUpUrlIdProperty).isEmpty();
    }

    private void handleInvalidMessage(QMessage message) {
        if (message != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("handling invalid message: " + message);
                logger.debug("publishing invalid message to queue '" + outGarbageQueueName + "'");
            }
            send(message, outGarbageQueueName);
        }
    }

    public void doProcess(QMessage qMessage) {
        if (isPdaMessage(qMessage)) {
            handlePdaMessage(qMessage);
        } else if (isLookupMessage(qMessage)) {
            handleLookupMessage(qMessage);
        }
    }

    private void handlePdaMessage(QMessage message) {
        if (logger.isDebugEnabled()) {
            logger.debug("handling pda message");
        }
        String pdaId = String.valueOf(message.getProperties().get(SuperrouterConstants.KEY_PROP_PDA_ID));
        // queuename pattern: K_router.IN_pda_<pda_id>
        String queueName = outPdaQueueName + pdaId;
        if (logger.isDebugEnabled()) {
            logger.debug("publishing pda message to queue '" + queueName + "': " + message);
        }
        send(message, queueName);
    }
    
    private void handleLookupMessage(QMessage message) {
        if (logger.isDebugEnabled()) {
            logger.debug("handling lookup message");
        }
        String lookUpUrlId = String.valueOf(message.getProperties().get(SuperrouterConstants.KEY_PROP_LOOKUP_URL_ID));
        String lookUpPrio = String.valueOf(message.getProperties().get(SuperrouterConstants.KEY_PROP_LOOKUP_PRIO));
        // queuename pattern: K_router.IN_lookup_<look_up_url_id>_<prio>
        String queueName = outLookupQueueName + DispatcherUtils.nnnnnn(lookUpUrlId) + "_" + lookUpPrio;
        if (logger.isDebugEnabled()) {
            logger.debug("publishing lookup message to queue '" + queueName + "': " + message);
        }
        send(message, queueName);
    }
    
    private void send(QMessage qMessage, String queueName) {
        try {
            MessageProducer producer = getProducer(queueName);
            sendOutboundMessage(producer, qMessage);
        } catch (JMSException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Unable to create new producer for queue '" + queueName
                        + "', therefore sending of message not possible", e);
            }
            e.printStackTrace();
        }
    }
    
    private MessageProducer getProducer(String queueName) throws JMSException {
        MessageProducer producer = allProducers.get(queueName);
        if (producer == null) {
            producer = createNewProducer(queueName);
        }
        return producer;
    }

    private MessageProducer createNewProducer(String queueName) throws JMSException {
        Destination destOut = session.createQueue(queueName);
        MessageProducer producer = session.createProducer(destOut);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        allProducers.put(queueName, producer);
        if (logger.isDebugEnabled()) {
            logger.debug("created new producer for queue '" + queueName + "'");
        }
        return producer;
    }

    protected void sendOutboundMessage(MessageProducer producer, QMessage out) {
        try {
            ObjectMessage objectMessage = out.getObjectMessage(this.session);
            producer.send(objectMessage);
            qMessageIn.getObjectMessage(session).acknowledge();
            currentMessageIn.acknowledge();
            currentMessageIn = null;
        } catch (JMSException jmsE) {
            if (logger.isErrorEnabled()) {
                logger.error("JMSException in run:" + jmsE.getErrorCode(), jmsE);
            }
        }
    }
}
