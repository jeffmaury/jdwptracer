package io.jdwptracer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class JDWPLogger implements Closeable {
    private final VMInfo vm = new VMInfo();
    private final Map<Integer, Packet> requests = new HashMap<>();
    private final Consumer<String> consumer;

    private boolean running = true;

    private static class Pair {
        private byte[] packet;
        private long time;

        private Pair(byte[] packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private final BlockingQueue<Pair> queue = new LinkedBlockingQueue<>();
    
    public static JDWPLogger getLogger(Consumer<String> consumer) {
        return new JDWPLogger(consumer);
    }
    
    public static JDWPLogger getLogger() {
        return getLogger(System.out::println);
    }
    
    protected JDWPLogger(Consumer<String> consumer) {
        this.consumer = consumer;
        new Thread(this::processQueue).start();
    }

    public void log(byte[] packet) {
        queue.add(new Pair(packet, System.currentTimeMillis()));
    }

    @Override
    public void close() throws IOException {
        running = false;
    }

    private void processQueue() {
        try {
            while (running) {
                Pair pair = queue.poll(1L, TimeUnit.MINUTES);
                if (pair != null) {
                    try {
                        Packet p = Packet.fromByteArray(pair.packet, vm);
                        if (p.flags == Packet.NoFlags) {
                            requests.put(p.id, p);
                        } else {
                            Packet request = requests.remove(p.id);
                            if (request != null) {
                                p.cmdSet = request.cmdSet;
                                p.cmd = request.cmd;
                            }
                        }

                        if (p.flags == Packet.Reply && p.cmdSet == JDWPVirtualMachine.VirtualMachine.COMMAND_SET && p.cmd == JDWPVirtualMachine.VirtualMachine.IDSizes.COMMAND) {
                            vm.sizeofFieldRef = p.readInt();
                            vm.sizeofMethodRef = p.readInt();
                            vm.sizeofObjectRef = p.readInt();
                            vm.sizeofClassRef = p.readInt();
                            vm.sizeofFrameRef = p.readInt();
                            p = new Packet(p);
                        }
                        dump(p);
                    } catch (IOException e) {
                        consumer.accept("Can't parse packet");
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void dump(Packet pkt) {
        Map<Integer, Class> cmdSet = JDWP.COMMANDS.get(Integer.valueOf(pkt.cmdSet));
        Class command = cmdSet != null ? cmdSet.get(Integer.valueOf(pkt.cmd)) : null;
        consumer.accept("JDWP Packet id=" + pkt.id +
                " cmdSet=" + (command != null ? command.getDeclaringClass().getSimpleName() : pkt.cmdSet) +
                " cmd=" + (command != null ? command.getSimpleName() : pkt.cmd) + " flags=" + pkt.flags +
                " errorCode=" + errorCodeToString(pkt.errorCode));
        if (pkt.errorCode == 0) {
            switch (pkt.cmdSet) {
                case JDWPVirtualMachine.VirtualMachine.COMMAND_SET:
                    dumpVirtualMachine(pkt);
                    break;
                case JDWPReferenceType.ReferenceType.COMMAND_SET:
                    dumpReferenceType(pkt);
                    break;
                case JDWPClassType.ClassType.COMMAND_SET:
                    dumpClassType(pkt);
                    break;
                case JDWPArrayType.ArrayType.COMMAND_SET:
                    dumpArrayType(pkt);
                    break;
                case JDWPInterfaceType.InterfaceType.COMMAND_SET:
                    dumpInterfaceType(pkt);
                    break;
                case JDWPMethod.Method.COMMAND_SET:
                    dumpMethod(pkt);
                    break;
                case JDWPField.Field.COMMAND_SET:
                    dumpField(pkt);
                    break;
                case JDWPObjectReference.ObjectReference.COMMAND_SET:
                    dumpObjectReference(pkt);
                    break;
                case JDWPStringReference.StringReference.COMMAND_SET:
                    dumpStringReference(pkt);
                    break;
                case JDWPThreadReference.ThreadReference.COMMAND_SET:
                    dumpThreadReference(pkt);
                    break;
                case JDWPThreadGroupReference.ThreadGroupReference.COMMAND_SET:
                    dumpThreadGroupReference(pkt);
                    break;
                case JDWPArrayReference.ArrayReference.COMMAND_SET:
                    dumpArrayReference(pkt);
                    break;
                case JDWPClassLoaderReference.ClassLoaderReference.COMMAND_SET:
                    dumpClassLoaderReference(pkt);
                    break;
                case JDWPEventRequest.EventRequest.COMMAND_SET:
                    dumpEventRequest(pkt);
                    break;
                case JDWPStackFrame.StackFrame.COMMAND_SET:
                    dumpStackFrame(pkt);
                    break;
                case JDWPClassObjectReference.ClassObjectReference.COMMAND_SET:
                    dumpClassObjectReference(pkt);
                    break;
                case JDWPModuleReference.ModuleReference.COMMAND_SET:
                    dumpModuleReference(pkt);
                    break;
                case JDWPEvent.Event.COMMAND_SET:
                    dumpEvent(pkt);
                    break;
            }
        }
    }

    private String errorCodeToString(short errorCode) {
        switch (errorCode) {
            case JDWP.Error.NONE:
                return "NONE";
            case JDWP.Error.INVALID_THREAD:
                return "INVALID_THREAD";
            case JDWP.Error.INVALID_THREAD_GROUP:
                return "INVALID_THREAD_GROUP";
            case JDWP.Error.INVALID_PRIORITY:
                return "INVALID_PRIORITY";
            case JDWP.Error.THREAD_NOT_SUSPENDED:
                return "THREAD_NOT_SUSPENDED";
            case JDWP.Error.THREAD_SUSPENDED:
                return "THREAD_SUSPENDED";
            case JDWP.Error.THREAD_NOT_ALIVE:
                return "THREAD_THREAD_NOT_ALIVE";
            case JDWP.Error.INVALID_OBJECT:
                return "INVALID_OBJECT";
            case JDWP.Error.INVALID_CLASS:
                return "INVALID_CLASS";
            case JDWP.Error.CLASS_NOT_PREPARED:
                return "CLASS_NOT_PREPARED";
            case JDWP.Error.INVALID_METHODID:
                return "INVALID_METHODID";
            case JDWP.Error.INVALID_LOCATION:
                return "INVALID_LOCATION";
            case JDWP.Error.INVALID_FIELDID:
                return "INVALID_FIELDID";
            case JDWP.Error.INVALID_FRAMEID:
                return "INVALID_FRAMEID";
            case JDWP.Error.NO_MORE_FRAMES:
                return "NO_MORE_FRAMES";
            case JDWP.Error.OPAQUE_FRAME:
                return "OPAQUE_FRAME";
            case JDWP.Error.NOT_CURRENT_FRAME:
                return "NOT_CURRENT_FRAME";
            case JDWP.Error.TYPE_MISMATCH:
                return "TYPE_MISMATCH";
            case JDWP.Error.INVALID_SLOT:
                return "INVALID_SLOT";
            case JDWP.Error.DUPLICATE:
                return "DUPLICATE";
            case JDWP.Error.NOT_FOUND:
                return "NOT_FOUND";
            case JDWP.Error.INVALID_MONITOR:
                return "INVALID_MONITOR";
            case JDWP.Error.NOT_MONITOR_OWNER:
                return "NOT_MONITOR_OWNER";
            case JDWP.Error.INTERRUPT:
                return "INTERRUPT";
            case JDWP.Error.INVALID_CLASS_FORMAT:
                return "INVALID_CLASS_FORMAT";
            case JDWP.Error.CIRCULAR_CLASS_DEFINITION:
                return "CIRCULAR_CLASS_DEFINITION";
            case JDWP.Error.FAILS_VERIFICATION:
                return "FAILS_VERIFICATION";
            case JDWP.Error.ADD_METHOD_NOT_IMPLEMENTED:
                return "ADD_METHOD_NOT_IMPLEMENTED";
            case JDWP.Error.SCHEMA_CHANGE_NOT_IMPLEMENTED:
                return "SCHEMA_CHANGE_NOT_IMPLEMENTED";
            case JDWP.Error.INVALID_TYPESTATE:
                return "INVALID_TYPESTATE";
            case JDWP.Error.HIERARCHY_CHANGE_NOT_IMPLEMENTED:
                return "HIERARCHY_CHANGE_NOT_IMPLEMENTED";
            case JDWP.Error.DELETE_METHOD_NOT_IMPLEMENTED:
                return "DELETE_METHOD_NOT_IMPLEMENTED";
            case JDWP.Error.UNSUPPORTED_VERSION:
                return "UNSUPPORTED_VERSION";
            case JDWP.Error.NAMES_DONT_MATCH:
                return "NAMES_DONT_MATCH";
            case JDWP.Error.CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
                return "CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED";
            case JDWP.Error.METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
                return "METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED";
            case JDWP.Error.NOT_IMPLEMENTED:
                return "NOT_IMPLEMENTED";
            case JDWP.Error.NULL_POINTER:
                return "NULL_POINTER";
            case JDWP.Error.ABSENT_INFORMATION:
                return "ABSENT_INFORMATION";
            case JDWP.Error.INVALID_EVENT_TYPE:
                return "INVALID_EVENT_TYPE";
            case JDWP.Error.ILLEGAL_ARGUMENT:
                return "ILLEGAL_ARGUMENT";
            case JDWP.Error.OUT_OF_MEMORY:
                return "OUT_OF_MEMORY";
            case JDWP.Error.ACCESS_DENIED:
                return "ACCESS_DENIED";
            case JDWP.Error.VM_DEAD:
                return "VM_DEAD";
            case JDWP.Error.INTERNAL:
                return "INTERNAL";
            case JDWP.Error.UNATTACHED_THREAD:
                return "UNATTACHED_THREAD";
            case JDWP.Error.INVALID_TAG:
                return "INVALID_TAG";
            case JDWP.Error.ALREADY_INVOKING:
                return "ALREADY_INVOKING";
            case JDWP.Error.INVALID_INDEX:
                return "INVALID_INDEX";
            case JDWP.Error.INVALID_LENGTH:
                return "INVALID_LENGTH";
            case JDWP.Error.INVALID_STRING:
                return "INVALID_STRING";
            case JDWP.Error.INVALID_CLASS_LOADER:
                return "INVALID_CLASS_LOADER";
            case JDWP.Error.INVALID_ARRAY:
                return "INVALID_ARRAY";
            case JDWP.Error.TRANSPORT_LOAD:
                return "TRANSPORT_LOAD";
            case JDWP.Error.TRANSPORT_INIT:
                return "TRANSPORT_INIT";
            case JDWP.Error.NATIVE_METHOD:
                return "NATIVE_METHOD";
            case JDWP.Error.INVALID_COUNT:
                return "INVALID_COUNT";
            default:
                return "invalid " + errorCode;
        }
    }

    private void dumpEvent(Packet pkt) {
        if (pkt.cmd == JDWPEvent.Event.Composite.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("suspendPolicy=" + suspendPolicyToString(pkt.readByte()));
                int events = pkt.readInt();
                for(int i=0; i < events;++i) {
                    byte eventKind = pkt.readByte();
                    switch (eventKind) {
                        case JDWP.EventKind.VM_START:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef());
                            break;
                        case JDWP.EventKind.SINGLE_STEP:
                        case JDWP.EventKind.BREAKPOINT:
                        case JDWP.EventKind.METHOD_ENTRY:
                        case JDWP.EventKind.METHOD_EXIT:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt));
                            break;
                        case JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) + " value=" + valueToString(pkt));
                            break;
                        case JDWP.EventKind.MONITOR_CONTENDED_ENTER:
                        case JDWP.EventKind.MONITOR_CONTENDED_ENTERED:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef() + " location=" + locationToString(pkt));
                            break;
                        case JDWP.EventKind.MONITOR_WAIT:
                        case JDWP.EventKind.MONITOR_WAITED:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef() + " location=" + locationToString(pkt) +
                                    " timed_out=" + pkt.readBoolean());
                            break;
                        case JDWP.EventKind.EXCEPTION:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef() +
                                    " catchLocation=" + locationToString(pkt));
                            break;
                        case JDWP.EventKind.THREAD_START:
                        case JDWP.EventKind.THREAD_DEATH:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef());
                            break;
                        case JDWP.EventKind.CLASS_PREPARE:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readClassRef() +
                                    " signature=" + pkt.readString() + " status=" + statusToString(pkt.readInt()));
                            break;
                        case JDWP.EventKind.CLASS_UNLOAD:
                            consumer.accept("requestID=" + pkt.readInt() + " signature=" + pkt.readString());
                            break;
                        case JDWP.EventKind.FIELD_ACCESS:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) +
                                    " refTypeTag=" + typeTagToString(pkt.readByte()) +
                                    " typeID=" + pkt.readClassRef() + " fieldID+" + pkt.readFieldRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef());
                            break;
                        case JDWP.EventKind.FIELD_MODIFICATION:
                            consumer.accept("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) +
                                    " refTypeTag=" + typeTagToString(pkt.readByte()) +
                                    " typeID=" + pkt.readClassRef() + " fieldID+" + pkt.readFieldRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef() + " valueToBe=" + valueToString(pkt));
                            break;
                        case JDWP.EventKind.VM_DEATH:
                            consumer.accept("requestID=" + pkt.readInt());
                            break;
                    }
                }
            }
        }
    }

    private static String suspendPolicyToString(byte suspendPolicy) {
        switch (suspendPolicy) {
            case JDWP.SuspendPolicy.NONE:
                return "NONE";
            case JDWP.SuspendPolicy.EVENT_THREAD:
                return "EVENT_THREAD";
            case JDWP.SuspendPolicy.ALL:
                return "ALL";
            default:
                return "UNKNOWN(" + suspendPolicy + ")";
        }
    }

    private void dumpModuleReference(Packet pkt) {
        if (pkt.cmd == JDWPModuleReference.ModuleReference.Name.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("module=" + pkt.readObjectRef());
            } else {
                consumer.accept("name=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPModuleReference.ModuleReference.ClassLoader.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("module=" + pkt.readObjectRef());
            } else {
                consumer.accept("classLoader=" + pkt.readObjectRef());
            }
        }
    }

    private void dumpClassObjectReference(Packet pkt) {
        if (pkt.cmd == JDWPClassObjectReference.ClassObjectReference.ReflectedType.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("classObject=" + pkt.readObjectRef());
            } else {
                consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readClassRef());
            }
        }
    }

    private void dumpStackFrame(Packet pkt) {
        if (pkt.cmd == JDWPStackFrame.StackFrame.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                int slots;
                consumer.accept("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef() +
                        " slots=" + (slots = pkt.readInt()));
                for(int i=0; i < slots;++i) {
                    consumer.accept("slot=" + pkt.readInt() + " sigByte=" + pkt.readByte());
                }
            } else {
                int values = pkt.readInt();
                consumer.accept("values=" + values);
                for(int i=0; i < values;++i) {
                    consumer.accept("slotValue=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPStackFrame.StackFrame.SetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                int slotsValues = pkt.readInt();
                consumer.accept("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef() +
                        " slotsValues=" + slotsValues);
                for(int i=0; i < slotsValues;++i) {
                    consumer.accept("slot=" + pkt.readInt() + " slotValue=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPStackFrame.StackFrame.ThisObject.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef());
            } else {
                consumer.accept("objectThis=" + valueToString(pkt));
            }
        } else if (pkt.cmd == JDWPStackFrame.StackFrame.PopFrames.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef());
            }
        }
    }

    private void dumpEventRequest(Packet pkt) {
        if (pkt.cmd == JDWPEventRequest.EventRequest.Set.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("eventKind=" + eventKindToString(pkt.readByte()) + " suspendPolicy=" + suspendPolicyToString(pkt.readByte()));
                int modifiers = pkt.readInt();
                for(int i=0; i < modifiers;++i) {
                    byte modKind = pkt.readByte();
                    System.out.print("modKind=" + dumpModKind(modKind));
                    switch (modKind) {
                        case JDWP.ModKind.Count:
                            consumer.accept(" count=" + pkt.readInt());
                            break;
                        case JDWP.ModKind.Conditional:
                            consumer.accept(" exprID=" + pkt.readInt());
                            break;
                        case JDWP.ModKind.ThreadOnly:
                            consumer.accept(" thread=" + pkt.readObjectRef());
                            break;
                        case JDWP.ModKind.ClassOnly:
                            consumer.accept(" clazz=" + pkt.readClassRef());
                            break;
                        case JDWP.ModKind.ClassMatch:
                        case JDWP.ModKind.ClassExclude:
                            consumer.accept(" classPattern=" + pkt.readString());
                            break;
                        case JDWP.ModKind.LocationOnly:
                            consumer.accept(" loc=" + locationToString(pkt));
                            break;
                        case JDWP.ModKind.ExceptionOnly:
                            consumer.accept(" exceptionOrNull=" + pkt.readClassRef() + " caught=" + pkt.readBoolean() +
                                    " uncaught=" + pkt.readBoolean());
                            break;
                        case JDWP.ModKind.FieldOnly:
                            consumer.accept(" declaring=" + pkt.readClassRef() + " fieldID=" + pkt.readFieldRef());
                            break;
                        case JDWP.ModKind.Step:
                            consumer.accept(" thread=" + pkt.readObjectRef() + " size=" + stepSizeToString(pkt.readInt()) +
                                    " depth=" + stepDepthToString(pkt.readInt()));
                            break;
                        case JDWP.ModKind.InstanceOnly:
                            consumer.accept(" instance=" + pkt.readObjectRef());
                            break;
                        case JDWP.ModKind.SourceNameMatch:
                            consumer.accept(" sourceNamePattern=" + pkt.readString());
                            break;
                    }
                }
            } else {
                consumer.accept("requestID=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPEventRequest.EventRequest.Clear.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("eventKind=" + eventKindToString(pkt.readByte()) + " requestID=" + pkt.readInt());
            }
        }
    }

    private static String stepDepthToString(int stepDepth) {
        switch (stepDepth) {
            case JDWP.StepDepth.INTO:
                return "INTO";
            case JDWP.StepDepth.OVER:
                return "OVER";
            case JDWP.StepDepth.OUT:
                return "OUT";
            default:
                return String.valueOf(stepDepth);
        }
    }

    private static String stepSizeToString(int stepSize) {
        switch (stepSize) {
            case JDWP.StepSize.MIN:
                return "MIN";
            case JDWP.StepSize.LINE:
                return "LINE";
            default:
                return String.valueOf(stepSize);
        }
    }

    private static String dumpModKind(byte modKind) {
        switch (modKind) {
            case JDWP.ModKind.Count:
                return "Count";
            case JDWP.ModKind.Conditional:
                return "Conditional";
            case JDWP.ModKind.ThreadOnly:
                return "ThreadOnly";
            case JDWP.ModKind.ClassOnly:
                return "ClassOnly";
            case JDWP.ModKind.ClassMatch:
                return "ClassMatch";
            case JDWP.ModKind.ClassExclude:
                return "ClassExclude";
            case JDWP.ModKind.LocationOnly:
                return "LocationOnly";
            case JDWP.ModKind.ExceptionOnly:
                return "ExceptionOnly";
            case JDWP.ModKind.FieldOnly:
                return "FieldOnly";
            case JDWP.ModKind.Step:
                return "Step";
            case JDWP.ModKind.InstanceOnly:
                return "InstanceOnly";
            case JDWP.ModKind.SourceNameMatch:
                return "SourceNameMatch";
            default:
                return "Unknown(" + modKind + ")";
        }

    }

    private static String eventKindToString(byte eventKind) {
        switch (eventKind) {
            case JDWP.EventKind.SINGLE_STEP:
                return "SINGLE_STEP";
            case JDWP.EventKind.BREAKPOINT:
                return "BREAKPOINT";
            case JDWP.EventKind.FRAME_POP:
                return "FRAME_POP";
            case JDWP.EventKind.EXCEPTION:
                return "EXCEPTION";
            case JDWP.EventKind.USER_DEFINED:
                return "USER_DEFINED";
            case JDWP.EventKind.THREAD_START:
                return "THREAD_START";
            case JDWP.EventKind.THREAD_DEATH:
                return "THREAD_DEATH";
            case JDWP.EventKind.CLASS_PREPARE:
                return "CLASS_PREPARE";
            case JDWP.EventKind.CLASS_UNLOAD:
                return "CLASS_UNLOAD";
            case JDWP.EventKind.CLASS_LOAD:
                return "CLASS_LOAD";
            case JDWP.EventKind.FIELD_ACCESS:
                return "FIELD_ACCESS";
            case JDWP.EventKind.FIELD_MODIFICATION:
                return "FIELD_MODIFICATION";
            case JDWP.EventKind.EXCEPTION_CATCH:
                return "EXCEPTION_CATCH";
            case JDWP.EventKind.METHOD_ENTRY:
                return "METHOD_ENTRY";
            case JDWP.EventKind.METHOD_EXIT:
                return "METHOD_EXIT";
            case JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                return "METHOD_EXIT_WITH_RETURN_VALUE";
            case JDWP.EventKind.MONITOR_CONTENDED_ENTER:
                return "MONITOR_CONTENDED_ENTER";
            case JDWP.EventKind.MONITOR_CONTENDED_ENTERED:
                return "MONITOR_CONTENDED_ENTERED";
            case JDWP.EventKind.MONITOR_WAIT:
                return "MONITOR_WAIT";
            case JDWP.EventKind.VM_START:
                return "VM_START";
            case JDWP.EventKind.VM_DEATH:
                return "VM_DEATH";
            case JDWP.EventKind.VM_DISCONNECTED:
                return "VM_DISCONNECTED";
            default:
                return "UNKNOWN(" + eventKind + ")";
        }
    }

    private void dumpClassLoaderReference(Packet pkt) {
        if (pkt.cmd == JDWPClassLoaderReference.ClassLoaderReference.VisibleClasses.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("classLoaderObject=" + pkt.readObjectRef());
            } else {
                int classes = pkt.readInt();
                for(int i=0; i < classes;++i) {
                    consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readObjectRef());
                }
            }
        }
    }

    private void dumpArrayReference(Packet pkt) {
        if (pkt.cmd == JDWPArrayReference.ArrayReference.Length.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("arrayObject=" + pkt.readObjectRef());
            } else {
                consumer.accept("arrayLength=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPArrayReference.ArrayReference.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("arrayObject=" + pkt.readObjectRef() + " firstIndex=" + pkt.readInt() +
                        " length=" + pkt.readInt());
            } else {
                byte type = pkt.readByte();
                int size = pkt.readInt();
                consumer.accept("type=" + tagToString(type) + " size=" + size);
                for(int i=0; i < size;++i) {
                    String value;
                    if (type == JDWP.Tag.ARRAY || type == JDWP.Tag.OBJECT || type == JDWP.Tag.STRING ||
                            type == JDWP.Tag.THREAD || type == JDWP.Tag.THREAD_GROUP || type == JDWP.Tag.CLASS_LOADER ||
                            type == JDWP.Tag.CLASS_OBJECT) {
                        value = valueToString(pkt);
                    } else {
                        value = valueToString(pkt, type);
                    }
                    consumer.accept("value" + i + "=" + value);
                }

            }
        } else if (pkt.cmd == JDWPArrayReference.ArrayReference.SetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("arrayObject=" + pkt.readObjectRef() + " firstIndex=" + pkt.readInt() +
                        " values=" + pkt.readInt());
            }
        }
    }

    private void dumpThreadGroupReference(Packet pkt) {
        if (pkt.cmd == JDWPThreadGroupReference.ThreadGroupReference.Name.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("group=" + pkt.readObjectRef());
            } else {
                consumer.accept("groupName=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPThreadGroupReference.ThreadGroupReference.Parent.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("group=" + pkt.readObjectRef());
            } else {
                consumer.accept("parentGroup=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadGroupReference.ThreadGroupReference.Children.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("group=" + pkt.readObjectRef());
            } else {
                int childThreads = pkt.readInt();
                for(int i=0; i < childThreads;++i) {
                    consumer.accept("childThread=" + pkt.readObjectRef());
                }
                int childGroups = pkt.readInt();
                for(int i=0; i < childGroups;++i) {
                    consumer.accept("childGroup=" + pkt.readObjectRef());
                }
            }
        }
    }

    private void dumpThreadReference(Packet pkt) {
        if (pkt.cmd == JDWPThreadReference.ThreadReference.Name.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                consumer.accept("threadName=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Suspend.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("thread=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Resume.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("thread=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Status.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                consumer.accept("threadStatus=" + threadStatusToString(pkt.readInt()) +
                        " suspendStatus=" + suspendStatusToString(pkt.readInt()));
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.ThreadGroup.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                consumer.accept("group=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Frames.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef() + " startFrame=" + pkt.readInt() +
                        " length=" + pkt.readInt());
            } else {
                int frames = pkt.readInt();
                for(int i=0; i < frames;++i) {
                    consumer.accept("frameID=" + pkt.readObjectRef() + " location=" + locationToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.FrameCount.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                consumer.accept("frameCount=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.OwnedMonitors.COMMAND ||
                pkt.cmd == JDWPThreadReference.ThreadReference.CurrentContendedMonitor.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                int owned = pkt.readInt();
                for(int i=0; i < owned;++i) {
                    consumer.accept("monitor=" + pkt.readByte() + pkt.readObjectRef());
                }
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Stop.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef() + " throwable=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Interrupt.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.SuspendCount.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                consumer.accept("suspendCount=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.OwnedMonitorsStackDepthInfo.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef());
            } else {
                int owned = pkt.readInt();
                for(int i=0; i < owned;++i) {
                    consumer.accept("monitor=" + pkt.readByte() + pkt.readObjectRef() + " stack_depth=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.ForceEarlyReturn.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("thread=" + pkt.readObjectRef() + "value=" + valueToString(pkt));
            }
        }
    }

    private String suspendStatusToString(int suspendStatus) {
        switch (suspendStatus) {
            case JDWP.SuspendStatus.SUSPEND_STATUS_SUSPENDED:
                return "suspended";
            default:
                return "invalid " + suspendStatus;
        }
    }

    private String threadStatusToString(int threadStatus) {
        switch (threadStatus) {
            case JDWP.ThreadStatus.ZOMBIE:
                return "zombie";
            case JDWP.ThreadStatus.RUNNING:
                return "running";
            case JDWP.ThreadStatus.SLEEPING:
                return "sleeping";
            case JDWP.ThreadStatus.MONITOR:
                return "monitor";
            case JDWP.ThreadStatus.WAIT:
                return "wait";
            default:
                return "invalid " + threadStatus;
        }
    }

    private static String locationToString(Packet pkt) {
        StringBuilder sb = new StringBuilder();
        var tag = pkt.readByte();
        if (tag == JDWP.TypeTag.CLASS) {
            sb.append("class");
        } else if (tag == JDWP.TypeTag.INTERFACE) {
            sb.append("interface");
        } else if (tag == JDWP.TypeTag.ARRAY) {
            sb.append("array");
        }
        sb.append(" classID=").append(pkt.readObjectRef()).
                append(" methodID=").append(pkt.readObjectRef()).
                append(" index=").append(pkt.readLong());
        return sb.toString();
    }

    private static String typeTagToString(byte val) {
        if (val == JDWP.TypeTag.CLASS) {
            return "class";
        } else if (val == JDWP.TypeTag.INTERFACE) {
            return "interface";
        } else if (val == JDWP.TypeTag.ARRAY) {
            return "array";
        }
        return "invalid " + val;

    }

    private static String tagToString(byte val) {
        switch (val) {
            case JDWP.Tag.ARRAY:
                return "array";
            case JDWP.Tag.BYTE:
                return "byte";
            case JDWP.Tag.CHAR:
                return "char";
            case JDWP.Tag.OBJECT:
                return "object";
            case JDWP.Tag.FLOAT:
                return "float";
            case JDWP.Tag.DOUBLE:
                return "double";
            case JDWP.Tag.INT:
                return "int";
            case JDWP.Tag.LONG:
                return "long";
            case JDWP.Tag.SHORT:
                return "short";
            case JDWP.Tag.VOID:
                return "void";
            case JDWP.Tag.BOOLEAN:
                return "boolean";
            case JDWP.Tag.STRING:
                return "string";
            case JDWP.Tag.THREAD:
                return "thread";
            case JDWP.Tag.THREAD_GROUP:
                return "threadGroup";
            case JDWP.Tag.CLASS_LOADER:
                return "classLoader";
            case JDWP.Tag.CLASS_OBJECT:
                return "class";
        }
        return "invalid " + val;

    }

    private static String statusToString(int status) {
        StringBuilder builder = new StringBuilder();
        int[] values = new int[] {JDWP.ClassStatus.VERIFIED, JDWP.ClassStatus.PREPARED, JDWP.ClassStatus.INITIALIZED, JDWP.ClassStatus.ERROR};
        String[] labels = new String[] {"verified", "prepared", "initialized", "error"};
        boolean first = true;
        for(int i = 0; i < values.length;++i) {
            if ((status & values[i]) == values[i]) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(labels[i]);
                first = false;
            }
        }
        return builder.toString();
    }

    private void dumpStringReference(Packet pkt) {
        if (pkt.cmd == JDWPStringReference.StringReference.Value.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("stringObject=" + pkt.readObjectRef());
            } else {
                consumer.accept("stringValue=" + pkt.readString());
            }
        }
    }

    private void dumpObjectReference(Packet pkt) {
        if (pkt.cmd == JDWPObjectReference.ObjectReference.ReferenceType.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("object=" + pkt.readObjectRef());
            } else {
                consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readClassRef());
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                int fields;
                consumer.accept("object=" + pkt.readObjectRef() + " fields=" + (fields = pkt.readInt()));
                for(int i=0; i < fields;++i) {
                    consumer.accept("fieldID=" + pkt.readFieldRef());
                }
            } else {
                int values = pkt.readInt();
                consumer.accept("values=" + values);
                for(int i=0; i < values;++i) {
                    consumer.accept("value" + i + "=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.SetValues.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("object=" + pkt.readObjectRef() + " values=" + pkt.readInt());
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.MonitorInfo.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("object=" + pkt.readObjectRef());
            } else {
                consumer.accept("owner=" + pkt.readObjectRef() + " entryCount=" + pkt.readInt());
                int waiters = pkt.readInt();
                for(int i=0; i < waiters;++i) {
                    consumer.accept("thread=" + pkt.readObjectRef());
                }
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.InvokeMethod.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("object=" + pkt.readObjectRef() + " thread=" + pkt.readObjectRef() +
                        " clazz=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    consumer.accept("arg" + i + "=" + valueToString(pkt));
                }
                consumer.accept("options=" + invokeOptionsToString(pkt.readInt()));
            } else {
                consumer.accept("returnValue=" + valueToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.DisableCollection.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("object=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.EnableCollection.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("object=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.IsCollected.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("object=" + pkt.readObjectRef());
            } else {
                consumer.accept("isCollected=" + pkt.readBoolean());
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.ReferringObjects.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("object=" + pkt.readObjectRef() + " maxReferers=" + pkt.readInt());
            } else {
                int referringObjects = pkt.readInt();
                for(int i=0; i < referringObjects;++i) {
                    consumer.accept("instance=" + pkt.readByte() + pkt.readObjectRef());
                }
            }
        }
    }

    private String invokeOptionsToString(int options) {
        StringBuilder builder = new StringBuilder();
        int[] values = new int[] {JDWP.InvokeOptions.INVOKE_SINGLE_THREADED, JDWP.InvokeOptions.INVOKE_NONVIRTUAL};
        String[] labels = new String[] {"single threaded", "non virtual"};
        boolean first = true;
        for(int i = 0; i < values.length;++i) {
            if ((options & values[i]) == values[i]) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(labels[i]);
                first = false;
            }
        }
        return builder.toString();
    }

    private void dumpField(Packet pkt) {
    }

    private void dumpMethod(Packet pkt) {
        if (pkt.cmd == JDWPMethod.Method.LineTable.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                consumer.accept("start=" + pkt.readLong() + " end=" + pkt.readLong());
                int lines = pkt.readInt();
                for(int i=0;i < lines;++i) {
                    consumer.accept("lineCodeIndex=" + pkt.readLong() + " lineNumber=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPMethod.Method.VariableTable.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                consumer.accept("argCnt=" + pkt.readInt());
                int slots = pkt.readInt();
                for (int i = 0; i < slots; ++i) {
                    consumer.accept("codeIndex=" + pkt.readLong() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " length=" + pkt.readInt() +
                            " slot=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPMethod.Method.Bytecodes.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                consumer.accept("bytes=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPMethod.Method.IsObsolete.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                consumer.accept("isObsolete=" + pkt.readBoolean());
            }
        } else if (pkt.cmd == JDWPMethod.Method.VariableTableWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                int argCnt = pkt.readInt();
                int slots = pkt.readInt();
                consumer.accept("argCnt=" + argCnt + " slots=" + slots);
                for (int i = 0; i < slots; ++i) {
                    consumer.accept("codeIndex=" + pkt.readLong() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                            " length=" + pkt.readInt() + " slot=" + pkt.readInt());
                }
            }
        }
    }

    private void dumpInterfaceType(Packet pkt) {
        if (pkt.cmd == JDWPInterfaceType.InterfaceType.InvokeMethod.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("clazz=" + pkt.readClassRef() + " thread=" + pkt.readObjectRef() +
                        " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    consumer.accept("arg" + i + "=" + valueToString(pkt));
                }
                consumer.accept("options=" + invokeOptionsToString(pkt.readInt()));
            } else {
                consumer.accept("returnValue=" + valueToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        }
    }

    private void dumpArrayType(Packet pkt) {
        if (pkt.cmd == JDWPArrayType.ArrayType.NewInstance.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("arrType=" + pkt.readObjectRef() + " length=" + pkt.readInt());
            } else {
                consumer.accept("newArray=" + pkt.readByte() + pkt.readObjectRef());
            }
        }
    }

    private void dumpClassType(Packet pkt) {
        if (pkt.cmd == JDWPClassType.ClassType.Superclass.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("clazz=" + pkt.readClassRef());
            } else {
                consumer.accept("superclass=" + pkt.readClassRef());
            }
        } else if (pkt.cmd == JDWPClassType.ClassType.SetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("clazz=" + pkt.readClassRef() + " values=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPClassType.ClassType.InvokeMethod.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("clazz=" + pkt.readClassRef() + " thread=" + pkt.readObjectRef() +
                        " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    consumer.accept("arg" + i + "=" + valueToString(pkt));
                }
                consumer.accept("options=" + invokeOptionsToString(pkt.readInt()));
            } else {
                consumer.accept("returnValue=" + valueToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPClassType.ClassType.NewInstance.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("clazz=" + pkt.readClassRef() + " thread=" + pkt.readObjectRef() +
                        " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    consumer.accept("arg" + i + "=" + valueToString(pkt));
                }
                consumer.accept("options=" + invokeOptionsToString(pkt.readInt()));
            } else {
                consumer.accept("newObject=" + pkt.readByte() + pkt.readObjectRef() + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        }
    }

    private void dumpReferenceType(Packet pkt) {
        if (pkt.cmd == JDWPReferenceType.ReferenceType.Signature.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readObjectRef());
            } else {
                consumer.accept("signature=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ClassLoader.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("classLoader=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Modifiers.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("modBits=" + modbitsToString(pkt.readInt()));
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Fields.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    consumer.accept("fieldID=" + pkt.readFieldRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " modBits=" + modbitsToString(pkt.readInt()));
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Methods.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    consumer.accept("methodID=" + pkt.readMethodRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " modBits=" + modbitsToString(pkt.readInt()));
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
                int fields = pkt.readInt();
                for(int i=0; i < fields;++i) {
                    consumer.accept("fieldID=" + pkt.readFieldRef());
                }
            } else {
                int values = pkt.readInt();
                for(int i=0; i < values;++i) {
                    consumer.accept("value=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.SourceFile.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("sourceFile=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.NestedTypes.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                int classes = pkt.readInt();
                for(int i=0; i < classes;++i) {
                    consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readClassRef());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Status.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("status=" + statusToString(pkt.readInt()));
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Interfaces.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                int interfaces = pkt.readInt();
                for(int i=0; i < interfaces;++i) {
                    consumer.accept("interfaceType=" + pkt.readClassRef());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ClassObject.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("classObject=" + pkt.readClassRef());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.SourceDebugExtension.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("extension=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.SignatureWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                consumer.accept("signature=" + pkt.readString() + " genericSignature=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.FieldsWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    consumer.accept("fieldID=" + pkt.readFieldRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                            " modBits=" + modbitsToString(pkt.readInt()));
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.MethodsWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    consumer.accept("methodID=" + pkt.readMethodRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                            " modBits=" + modbitsToString(pkt.readInt()));
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Instances.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                int instances = pkt.readInt();
                for(int i=0; i < instances;++i) {
                    consumer.accept("instance=" + pkt.readByte() + pkt.readObjectRef());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ClassFileVersion.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                consumer.accept("majorVersion=" + pkt.readInt() + " minorVersion=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ConstantPool.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                consumer.accept("count=" + pkt.readInt());
                int bytes = pkt.readInt();
                pkt.readByteArray(bytes);
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Module.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                consumer.accept("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                consumer.accept("module=" + pkt.readObjectRef());
            }
        }
    }

    private String modbitsToString(int modifier) {
        StringBuilder builder = new StringBuilder();
        int[] values = new int[] {Modifier.STATIC, Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.FINAL,
                Modifier.INTERFACE, Modifier.NATIVE, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.STRICT,
        Modifier.SYNCHRONIZED, Modifier.TRANSIENT, Modifier.VOLATILE};
        String[] labels = new String[] {"static", "public", "abstract", "final", "interface", "native", "private",
        "protected", "strict", "synchronized", "transient", "volatile"};
        boolean first = true;
        for(int i = 0; i < values.length;++i) {
            if ((modifier & values[i]) == values[i]) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(labels[i]);
                first = false;
            }
        }
        return builder.toString();
    }

    private static String valueToString(Packet pkt, Byte type) {
        StringBuilder sb = new StringBuilder();
        byte tag = type==null?pkt.readByte():type;
        switch (tag) {
            case JDWP.Tag.ARRAY:
                sb.append("[");
                sb.append(pkt.readObjectRef()).append("](array)");
                break;
            case JDWP.Tag.BYTE:
                sb.append(pkt.readByte()).append("(byte)");
                break;
            case JDWP.Tag.CHAR:
                sb.append(pkt.readChar()).append("(char)");
                break;
            case JDWP.Tag.OBJECT:
                sb.append(pkt.readObjectRef()).append("(object)");
                break;
            case JDWP.Tag.FLOAT:
                sb.append(pkt.readFloat()).append("(float)");
                break;
            case JDWP.Tag.DOUBLE:
                sb.append(pkt.readDouble()).append("(double)");
                break;
            case JDWP.Tag.INT:
                sb.append(pkt.readInt()).append("(int)");
                break;
            case JDWP.Tag.LONG:
                sb.append(pkt.readLong()).append("(long)");
                break;
            case JDWP.Tag.SHORT:
                sb.append(pkt.readShort()).append("(short)");
                break;
            case JDWP.Tag.VOID:
                sb.append("void");
                break;
            case JDWP.Tag.BOOLEAN:
                sb.append(pkt.readBoolean()).append("(boolean)");
                break;
            case JDWP.Tag.STRING:
                sb.append(pkt.readObjectRef()).append("(string)");
                break;
            case JDWP.Tag.THREAD:
                sb.append(pkt.readObjectRef()).append("(thread)");
                break;
            case JDWP.Tag.THREAD_GROUP:
                sb.append(pkt.readObjectRef()).append("(thread group)");
                break;
            case JDWP.Tag.CLASS_LOADER:
                sb.append(pkt.readObjectRef()).append("(class loader)");
                break;
            case JDWP.Tag.CLASS_OBJECT:
                sb.append(pkt.readObjectRef()).append("(class)");
                break;
        }
        return sb.toString();
    }

    private static String valueToString(Packet pkt) {
        return valueToString(pkt, null);
    }

    private void dumpVirtualMachine(Packet pkt) {
        if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.Version.COMMAND && pkt.flags == Packet.Reply) {
            String description = pkt.readString();
            int major = pkt.readInt();
            int minor = pkt.readInt();
            String vmVersion = pkt.readString();
            String vmName = pkt.readString();
            consumer.accept("description=" + description + " jdwpMajor=" + major + " jdwpMinor=" + minor + " vmVersion=" + vmVersion + " vmName=" + vmName);
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.ClassesBySignature.COMMAND) {
            if (pkt.flags == Packet.Reply) {
                int classes = pkt.readInt();
                for(int i=0; i < classes;++i) {
                    consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) +
                            " typeID=" + pkt.readClassRef() +
                            " status=" + statusToString(pkt.readInt()));
                }
            } else {
                consumer.accept("signature=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllClasses.COMMAND && pkt.flags == Packet.Reply) {
            int classes = pkt.readInt();
            for(int i=0; i < classes;++i) {
                consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readClassRef() +
                        " signature=" + pkt.readString() + " status=" + statusToString(pkt.readInt()));
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllThreads.COMMAND && pkt.flags == Packet.Reply) {
            int threads = pkt.readInt();
            for(int i=0; i < threads;++i) {
                consumer.accept("threadID=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.TopLevelThreadGroups.COMMAND && pkt.flags == Packet.Reply) {
            int groups = pkt.readInt();
            for(int i=0; i < groups;++i) {
                consumer.accept("threadGroupID=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.IDSizes.COMMAND && pkt.flags == Packet.Reply) {
            consumer.accept("fieldIDSize=" + pkt.readInt() + " methodIDSize=" + pkt.readInt() + " objectIDSize=" + pkt.readInt() + " referenceTypeIDSize=" + pkt.readInt() + " frameIDSize=" + pkt.readInt());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.Exit.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("exitCode=" + pkt.readInt());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.CreateString.COMMAND) {
            if (pkt.flags == Packet.Reply) {
                consumer.accept("stringObject=" + pkt.readObjectRef());
            } else {
                consumer.accept("utf=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.Capabilities.COMMAND && pkt.flags == Packet.Reply) {
            consumer.accept("canWatchFieldModification=" + pkt.readBoolean() + " canWatchFieldAccess=" + pkt.readBoolean() +
                    " canGetByteCodes=" + pkt.readBoolean() + " canGetSyntheticAttribute=" + pkt.readBoolean() +
                    " canGetOwnedMonitorInfo=" + pkt.readBoolean() + " canGetCurrentContendedMonitor=" + pkt.readBoolean() +
                    " canGetMonitorInfo=" + pkt.readBoolean());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.ClassPaths.COMMAND && pkt.flags == Packet.Reply) {
            consumer.accept("baseDir=" + pkt.readString());
            int classpaths = pkt.readInt();
            for(int i=0; i < classpaths;++i) {
                consumer.accept("path=" + pkt.readString());
            }
            int bootclasspaths = pkt.readInt();
            for(int i=0; i < bootclasspaths;++i) {
                consumer.accept("path=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.DisposeObjects.COMMAND && pkt.flags == Packet.NoFlags) {
            int requests = pkt.readInt();
            for(int i=0; i < requests;++i) {
                consumer.accept("object=" + pkt.readObjectRef() + " refCnt=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.CapabilitiesNew.COMMAND && pkt.flags == Packet.Reply) {
            consumer.accept("canWatchFieldModification=" + pkt.readBoolean() + " canWatchFieldAccess=" + pkt.readBoolean() +
                    " canGetByteCodes=" + pkt.readBoolean() + " canGetSyntheticAttribute=" + pkt.readBoolean() +
                    " canGetOwnedMonitorInfo=" + pkt.readBoolean() + " canGetCurrentContendedMonitor=" + pkt.readBoolean() +
                    " canGetMonitorInfo=" + pkt.readBoolean() + " canRedefineClasses="+ pkt.readBoolean() +
                    " canAddMethod=" + pkt.readBoolean() + " canUnrestrictedlyRedefineClasses=" + pkt.readBoolean() +
                    " canPopFrames=" + pkt.readBoolean() + " canUseInstanceFilters=" + pkt.readBoolean() +
                    " canGetSourceDebugExtension=" + pkt.readBoolean() + " canRequestVMDeathEvent=" + pkt.readBoolean() +
                    " canSetDefaultStratum=" + pkt.readBoolean() + " canGetInstanceInfo=" + pkt.readBoolean() +
                    " canRequestMonitorEvents=" + pkt.readBoolean() + " canGetMonitorFrameInfo=" + pkt.readBoolean() +
                    " canUseSourceNameFilters=" + pkt.readBoolean() + " canGetConstantPool=" + pkt.readBoolean() +
                    " canForceEarlyReturn=" + pkt.readBoolean());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.RedefineClasses.COMMAND && pkt.flags == Packet.NoFlags) {
            int classes = pkt.readInt();
            for(int i=0; i < classes;++i) {
                System.out.print("refType=" + pkt.readObjectRef());
                int classfile = pkt.readInt();
                pkt.readByteArray(classfile);
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.SetDefaultStratum.COMMAND && pkt.flags == Packet.NoFlags) {
            consumer.accept("stratumID=" + pkt.readString());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllClassesWithGeneric.COMMAND && pkt.flags == Packet.Reply) {
            int classes = pkt.readInt();
            for(int i=0; i < classes;++i) {
                consumer.accept("refTypeTag=" + typeTagToString(pkt.readByte()) + " typeID=" + pkt.readClassRef() +
                        " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                        " status=" + statusToString(pkt.readInt()));
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.InstanceCounts.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                int refTypesCount = pkt.readInt();
                for(int i=0; i < refTypesCount;++i) {
                    consumer.accept("refType=" + pkt.readClassRef());
                }
            } else {
                int counts = pkt.readInt();
                for(int i=0; i < counts;++i) {
                    consumer.accept("instanceCount=" + pkt.readLong());
                }
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllModules.COMMAND && pkt.flags == Packet.Reply) {
            int modules = pkt.readInt();
            for(int i=0; i < modules;++i) {
                consumer.accept("module=" + pkt.readObjectRef());
            }
        }
    }
}
