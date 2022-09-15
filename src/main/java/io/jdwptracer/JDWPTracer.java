package io.jdwptracer;

import com.sun.jdi.connect.TransportTimeoutException;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;
import com.sun.tools.jdi.SocketTransportService;

import java.io.IOException;

public class JDWPTracer {
    public static void main(String[] args) throws IOException {
        SocketTransportService service = new SocketTransportService();
        TransportService.ListenKey key = service.startListening(args[0]);
        System.out.println("Listening on " + key);
        while (true) {
            try {
                Connection inConnection = service.accept(key, 1000L,10000L);
                Connection outConnection = service.attach(args[1], 10000, 10000);
                PacketProcessor logger = new LoggingPacketProcessor();
                new Thread(() -> copy(inConnection , outConnection, logger)).start();
                new Thread(() -> copy(outConnection, inConnection , logger)).start();

            } catch (TransportTimeoutException e) {}
        }
    }

    private static void copy(Connection inConnection, Connection outConnection, PacketProcessor logger) {
        try {
            while (inConnection.isOpen()) {
                try {
                    byte[] pkt = inConnection.readPacket();
                    if (pkt.length > 0) {
                        logger.process(pkt);
                        outConnection.writePacket(pkt);
                    } else {
                        inConnection.close();
                    }
                } catch (Exception e) {}
            }
            outConnection.close();
        } catch (IOException e) {}
    }
}
