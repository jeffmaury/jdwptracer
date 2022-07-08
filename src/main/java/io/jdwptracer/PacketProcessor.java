package io.jdwptracer;

@FunctionalInterface
public interface PacketProcessor {
    void process(byte[] packet);
}
