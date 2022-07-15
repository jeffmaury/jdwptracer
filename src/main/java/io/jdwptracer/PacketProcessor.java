package io.jdwptracer;

public interface PacketProcessor {
    void process(byte[] packet);
    void stop();
}
