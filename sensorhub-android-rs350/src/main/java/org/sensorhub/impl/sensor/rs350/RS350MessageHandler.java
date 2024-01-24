package org.sensorhub.impl.sensor.rs350;

import static java.lang.Thread.sleep;

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
    private final RS350Sensor rs350Sensor;
    private static final String messageDelimiter = "</RadInstrumentData>";
    private static final String derivedDataDelimiter = "</DerivedData>";

    public interface MessageListener {
        void onNewMessage(RadInstrumentData message);
    }

    private final ArrayList<MessageListener> listeners = new ArrayList<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(true);

    private final Thread messageReader = new Thread(new Runnable() {
        @Override
        public void run() {
            InputStream inputStream = rs350Sensor.getInputStream();
            boolean continueProcessing = true;

            try {
                ArrayList<Character> buffer = new ArrayList<>();
                boolean readError = false;

                while (continueProcessing) {
                    if (readError) {
                        // If we have an error reading the message, then we need to restart the commProvider
                        // and try again. This is a workaround for a bug in the RS350 firmware that disconnects
                        // the socket in the middle of a long message.

                        logger.info("Attempting to restart commProvider.");
                        if (rs350Sensor.isCommProviderStarted()) {
                            rs350Sensor.stopCommProvider();
                        }
                        sleep(1000);
                        rs350Sensor.startCommProvider();
                        if (!rs350Sensor.isCommProviderStarted()) {
                            continue;
                        }
                        inputStream = rs350Sensor.getInputStream();
                        readError = false;
                    }

                    int character = inputStream.read();

                    // Detected STX
                    if (character == 0x02) {
                        character = inputStream.read();
                        // Detect ETX
                        while (character != 0x03 && character != -1) {
                            buffer.add((char) character);
                            character = inputStream.read();
                            if (character == -1) {
                                logger.info("Did not read complete message");
                                readError = true;
                            }
                        }
                        StringBuilder sb = new StringBuilder(buffer.size());

                        for (char c : buffer) {
                            sb.append(c);
                        }

                        String n42Message = sb.toString().replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
                        if (readError && n42Message.contains("</DerivedData>") && !n42Message.endsWith(messageDelimiter)) {
                            // If the message is incomplete, but contains a </DerivedData> tag, then we can
                            // close the message and process it. This is a workaround for a bug in the RS350
                            // firmware that disconnects the socket in the middle of a long message.

                            // Remove everything after "</DerivedData>"
                            n42Message = n42Message.split(derivedDataDelimiter)[0];
                            n42Message = n42Message + derivedDataDelimiter;

                            // Write the closing tag
                            n42Message = n42Message + "\n" + messageDelimiter;
                        }

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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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

    public RS350MessageHandler(RS350Sensor rs350Sensor) {
        this.rs350Sensor = rs350Sensor;

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
