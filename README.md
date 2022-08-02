# jdwptracer
A Java JDWP dumper and tracer tool

# What's jdwptracer

jdwptracer is a proxy that will trace JDWP packets being
exchanged between a local TCP socket and a remote JDWP enabled process.

# Runnning jdwptracer

First, you need to build jdwptracer. You must have [Apache Maven](https://maven.apache.org) installed on your local
machine.

Once Apache Maven is installed, run the following command:

`mvn install`

Now, to start jdwptracer, run the following command:

`java -cp target/jdwptracer-1.0-SNAPSHOT.jar io.jdwptracer.JDWPTracer port address`

where

- `port` is the local port jdwptracer will be listening on
- `address` is the address of the remote JVM. It can take port or host:port forms

Then configure your Java debugger to connect to the port jdwptracer is listening on instead of the port the JVM under
debug is listening on and you will see JDWP packets being decoded and printed on the standard output.