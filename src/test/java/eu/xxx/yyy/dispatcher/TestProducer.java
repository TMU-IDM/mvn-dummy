package eu.xxx.yyy.dispatcher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.apache.commons.configuration.Configuration;

import eu.smscarrier.superrouter.domain.SuperrouterConstants;
import eu.smscarrier.superrouter.messages.QMessage;
import eu.smscarrier.superrouter.messages.RouterInMessage;
import eu.xxx.yyy.dispatcher.PermanentProcessor;

public class TestProducer extends Thread implements ExceptionListener {

        private Configuration configuration;
    
        private ConnectionFactory connectionFactory;
        private Connection connection;
        private Session session;
        private MessageProducer producer;

        protected TestProducer(Configuration configuration) {
            this.configuration = configuration;
            try {
                initConnection();
                initProducer();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

        private void initConnection() throws JMSException {
            connectionFactory = new ActiveMQConnectionFactory(configuration.getString(PermanentProcessor.CFG_BROKER_URL));
            connection = connectionFactory.createConnection();
            connection.start();
            connection.setExceptionListener(this);
            session = connection.createSession(false, ActiveMQSession.INDIVIDUAL_ACKNOWLEDGE);
        }

        private void initProducer() throws JMSException {
            Destination destOut = session.createQueue(configuration.getString(PermanentProcessor.CFG_QUEUE_IN));
            producer = session.createProducer(destOut);
        }

        @Override
        public void run() {
            List<QMessage> testMessages = getTestMessages();
            for (int i = 0; i < testMessages.size(); i++) {
                QMessage message = testMessages.get(i);
                sendOutboundMessage(message);
            }
        }

        @Override
        public void onException(JMSException exception) {

        }

        protected void sendOutboundMessage(QMessage out) {
            try {
                ObjectMessage objectMessage = out.getObjectMessage(this.session);
                producer.send(objectMessage);
            } catch (JMSException jmsE) {

            }
        }

        private List<QMessage> getTestMessages() {
            List<QMessage> testMessages = new LinkedList<QMessage>();

            // create lookup msgs
            for (int i = 0; i < 5; i++) {
                RouterInMessage msg = new RouterInMessage();
                QMessage qMsg = new QMessage(getLookupProperties(), msg);
                testMessages.add(qMsg);
            }

            // create pda msgs
            for (int i = 0; i < 5; i++) {
                RouterInMessage msg = new RouterInMessage();
                QMessage qMsg = new QMessage(getPdaProperties(), msg);
                testMessages.add(qMsg);
            }

            // create invalid msgs
            for (int i = 0; i < 5; i++) {
                RouterInMessage msg = new RouterInMessage();
                QMessage qMsg = new QMessage(getInvalidProperties(), msg);
                testMessages.add(qMsg);
            }

            return testMessages;
        }

        private HashMap<String, Object> getLookupProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put(SuperrouterConstants.KEY_PROP_LOOKUP_PRIO, 42);
            props.put(SuperrouterConstants.KEY_PROP_LOOKUP_URL_ID, 1337);
            return props;
        }

        private HashMap<String, Object> getPdaProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put(SuperrouterConstants.KEY_PROP_PDA_ID, 101);
            return props;
        }

        private HashMap<String, Object> getInvalidProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put(SuperrouterConstants.KEY_PROP_LOOKUP_PRIO, "");
            props.put(SuperrouterConstants.KEY_PROP_LOOKUP_URL_ID, null);
            props.put(SuperrouterConstants.KEY_PROP_PDA_ID, "");
            return props;
        }
    }