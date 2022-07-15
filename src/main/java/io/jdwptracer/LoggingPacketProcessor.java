package io.jdwptracer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoggingPacketProcessor implements PacketProcessor {
    private final VMInfo vm = new VMInfo();
    private final Map<Integer, Packet> requests = new HashMap<>();

    private final JDWPLogger logger = JDWPLogger.getLogger();

    @Override
    public void process(byte[] packet) {
        logger.log(packet);
    }

    @Override
    public void stop() {
        try {
            logger.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
