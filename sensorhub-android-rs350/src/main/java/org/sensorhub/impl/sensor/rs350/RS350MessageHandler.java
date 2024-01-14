package org.sensorhub.impl.sensor.rs350;

import android.util.Xml;

import org.sensorhub.impl.sensor.rs350.messages.RadInstrumentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RS350MessageHandler {
    Logger logger = LoggerFactory.getLogger(RS350MessageHandler.class);

    final LinkedList<String> messageQueue = new LinkedList<>();

    private final InputStream msgIn;

    private final String messageDelimiter;

    public interface MessageListener {
        void onNewMessage(RadInstrumentData message);
    }

    private final ArrayList<MessageListener> listeners = new ArrayList<>();

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {

            boolean continueProcessing = true;

            try {

                ArrayList<Character> buffer = new ArrayList<>();

                while (continueProcessing) {

                    int character = msgIn.read();

                    // Detected STX
                    if (character == 0x02) {
                        character = msgIn.read();
                        // Detect ETX
                        while (character != 0x03 && character != -1) {
                            buffer.add((char) character);
                            character = msgIn.read();
                            if (character == -1) {
                                System.out.println("did not read complete message");
                            }
                        }
                        StringBuilder sb = new StringBuilder(buffer.size());

                        for (char c : buffer) {
                            sb.append(c);
                        }

                        String n42Message = sb.toString().replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();

                        synchronized (messageQueue) {
                            messageQueue.add(n42Message);
                            messageQueue.notifyAll();
                        }
                        buffer.clear();
                    }

                    synchronized (isProcessing) {
                        continueProcessing = isProcessing.get();
                    }
                }
            } catch (IOException exception) {
                logger.error("Error reading message.");
                logger.error(Arrays.toString(exception.getStackTrace()));
            }
        }
    });

    private final Thread messageNotifier = new Thread(() -> {
        boolean continueProcessing = true;

        while (continueProcessing) {

            String currentMessage = null;

            synchronized (messageQueue) {
                try {
                    while (messageQueue.isEmpty()) {
                        messageQueue.wait();
                    }

                    currentMessage = messageQueue.removeFirst();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (currentMessage != null && !currentMessage.isEmpty()) {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(new java.io.StringReader(currentMessage));
                    parser.nextTag();

                    RadInstrumentData message = new RadInstrumentData(parser);
                    message.parse();

                    for (MessageListener listener : listeners) {
                        listener.onNewMessage(message);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing message.");
                    logger.error(Arrays.toString(e.getStackTrace()));
                }
            }

            synchronized (isProcessing) {
                continueProcessing = isProcessing.get();
            }
        }
    });

    public RS350MessageHandler(InputStream msgIn, String messageDelimiter) {
        this.msgIn = msgIn;
        this.messageDelimiter = messageDelimiter;

        this.messageReader.start();
        this.messageNotifier.start();
    }

    public void addMessageListener(MessageListener listener) {

        listeners.add(listener);
    }

    public void stopProcessing() {

        synchronized (isProcessing) {

            isProcessing.set(false);
        }
    }
}
