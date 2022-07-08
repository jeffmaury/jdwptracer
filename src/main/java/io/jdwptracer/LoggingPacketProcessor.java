package io.jdwptracer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoggingPacketProcessor implements PacketProcessor {
    private final VMInfo vm = new VMInfo();
    private final Map<Integer, Packet> requests = new HashMap<>();

    @Override
    public void process(byte[] packet) {
        try {
            Packet p = Packet.fromByteArray(packet, vm);
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
            System.out.println("Can't parse packet");
        }

    }

    private static void dump(Packet pkt) {
        Map<Integer, Class> cmdSet = JDWP.COMMANDS.get(Integer.valueOf(pkt.cmdSet));
        Class command = cmdSet != null ? cmdSet.get(Integer.valueOf(pkt.cmd)) : null;
        System.out.println("JDWP Packet id=" + pkt.id + " cmdSet=" + (command != null ? command.getDeclaringClass().getSimpleName() : pkt.cmdSet) + " cmd=" +
                (command != null ? command.getSimpleName() : pkt.cmd) + " flags=" + pkt.flags + " errorCode=" + pkt.errorCode);
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

    private static void dumpEvent(Packet pkt) {
        if (pkt.cmd == JDWPEvent.Event.Composite.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("suspendPolicy=" + suspendPolicyToString(pkt.readByte()));
                int events = pkt.readInt();
                for(int i=0; i < events;++i) {
                    byte eventKind = pkt.readByte();
                    switch (eventKind) {
                        case JDWP.EventKind.VM_START:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef());
                            break;
                        case JDWP.EventKind.SINGLE_STEP:
                        case JDWP.EventKind.BREAKPOINT:
                        case JDWP.EventKind.METHOD_ENTRY:
                        case JDWP.EventKind.METHOD_EXIT:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt));
                            break;
                        case JDWP.EventKind.METHOD_EXIT_WITH_RETURN_VALUE:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) + " value=" + valueToString(pkt));
                            break;
                        case JDWP.EventKind.MONITOR_CONTENDED_ENTER:
                        case JDWP.EventKind.MONITOR_CONTENDED_ENTERED:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef() + " location=" + locationToString(pkt));
                            break;
                        case JDWP.EventKind.MONITOR_WAIT:
                        case JDWP.EventKind.MONITOR_WAITED:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef() + " location=" + locationToString(pkt) +
                                    " timed_out=" + pkt.readBoolean());
                            break;
                        case JDWP.EventKind.EXCEPTION:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef() +
                                    " catchLocation=" + locationToString(pkt));
                            break;
                        case JDWP.EventKind.THREAD_START:
                        case JDWP.EventKind.THREAD_DEATH:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef());
                            break;
                        case JDWP.EventKind.CLASS_PREPARE:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef() +
                                    " signature=" + pkt.readString() + " status=" + pkt.readInt());
                            break;
                        case JDWP.EventKind.CLASS_UNLOAD:
                            System.out.println("requestID=" + pkt.readInt() + " signature=" + pkt.readString());
                            break;
                        case JDWP.EventKind.FIELD_ACCESS:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) + " refTypeTag=" + pkt.readByte() +
                                    " typeID=" + pkt.readClassRef() + " fieldID+" + pkt.readFieldRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef());
                            break;
                        case JDWP.EventKind.FIELD_MODIFICATION:
                            System.out.println("requestID=" + pkt.readInt() + " thread=" + pkt.readObjectRef() +
                                    " location=" + locationToString(pkt) + " refTypeTag=" + pkt.readByte() +
                                    " typeID=" + pkt.readClassRef() + " fieldID+" + pkt.readFieldRef() +
                                    " object=" + pkt.readByte() + pkt.readObjectRef() + " valueToBe=" + valueToString(pkt));
                            break;
                        case JDWP.EventKind.VM_DEATH:
                            System.out.println("requestID=" + pkt.readInt());
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

    private static void dumpModuleReference(Packet pkt) {
        if (pkt.cmd == JDWPModuleReference.ModuleReference.Name.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("module=" + pkt.readObjectRef());
            } else {
                System.out.println("name=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPModuleReference.ModuleReference.ClassLoader.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("module=" + pkt.readObjectRef());
            } else {
                System.out.println("classLoader=" + pkt.readObjectRef());
            }
        }
    }

    private static void dumpClassObjectReference(Packet pkt) {
        if (pkt.cmd == JDWPClassObjectReference.ClassObjectReference.ReflectedType.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("classObject=" + pkt.readObjectRef());
            } else {
                System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef());
            }
        }
    }

    private static void dumpStackFrame(Packet pkt) {
        if (pkt.cmd == JDWPStackFrame.StackFrame.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef());
                int slots = pkt.readInt();
                for(int i=0; i < slots;++i) {
                    System.out.println("slot=" + pkt.readInt() + " sigByte=" + pkt.readByte());
                }
            } else {
                int values = pkt.readInt();
                for(int i=0; i < values;++i) {
                    System.out.println("slotValue=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPStackFrame.StackFrame.SetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef());
                int slotsValues = pkt.readInt();
                for(int i=0; i < slotsValues;++i) {
                    System.out.println("slot=" + pkt.readInt() + " slotValue=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPStackFrame.StackFrame.ThisObject.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef());
            } else {
                System.out.println("objectThis=" + pkt.readByte() + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPStackFrame.StackFrame.PopFrames.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + " frame=" + pkt.readObjectRef());
            }
        }
    }

    private static void dumpEventRequest(Packet pkt) {
        if (pkt.cmd == JDWPEventRequest.EventRequest.Set.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("eventKind=" + eventKindToString(pkt.readByte()) + " suspendPolicy=" + suspendPolicyToString(pkt.readByte()));
                int modifiers = pkt.readInt();
                for(int i=0; i < modifiers;++i) {
                    byte modKind = pkt.readByte();
                    System.out.print("modKind=" + dumpModKind(modKind));
                    switch (modKind) {
                        case JDWP.ModKind.Count:
                            System.out.println(" count=" + pkt.readInt());
                            break;
                        case JDWP.ModKind.Conditional:
                            System.out.println(" exprID=" + pkt.readInt());
                            break;
                        case JDWP.ModKind.ThreadOnly:
                            System.out.println(" thread=" + pkt.readObjectRef());
                            break;
                        case JDWP.ModKind.ClassOnly:
                            System.out.println(" clazz=" + pkt.readClassRef());
                            break;
                        case JDWP.ModKind.ClassMatch:
                        case JDWP.ModKind.ClassExclude:
                            System.out.println(" classPattern=" + pkt.readString());
                            break;
                        case JDWP.ModKind.LocationOnly:
                            System.out.println(" loc=" + locationToString(pkt));
                            break;
                        case JDWP.ModKind.ExceptionOnly:
                            System.out.println(" exceptionOrNull=" + pkt.readClassRef() + " caught=" + pkt.readBoolean() +
                                    " uncaught=" + pkt.readBoolean());
                            break;
                        case JDWP.ModKind.FieldOnly:
                            System.out.println(" declaring=" + pkt.readClassRef() + " fieldID=" + pkt.readFieldRef());
                            break;
                        case JDWP.ModKind.Step:
                            System.out.println(" thread=" + pkt.readObjectRef() + " size=" + stepSizeToString(pkt.readInt()) +
                                    " depth=" + stepDepthToString(pkt.readInt()));
                            break;
                        case JDWP.ModKind.InstanceOnly:
                            System.out.println(" instance=" + pkt.readObjectRef());
                            break;
                        case JDWP.ModKind.SourceNameMatch:
                            System.out.println(" sourceNamePattern=" + pkt.readString());
                            break;
                    }
                }
            } else {
                System.out.println("requestID=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPEventRequest.EventRequest.Clear.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("eventKind=" + pkt.readByte() + " requestID=" + pkt.readInt());
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

    private static void dumpClassLoaderReference(Packet pkt) {
        if (pkt.cmd == JDWPClassLoaderReference.ClassLoaderReference.VisibleClasses.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("classLoaderObject=" + pkt.readObjectRef());
            } else {
                int classes = pkt.readInt();
                for(int i=0; i < classes;++i) {
                    System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readObjectRef());
                }
            }
        }
    }

    private static void dumpArrayReference(Packet pkt) {
        if (pkt.cmd == JDWPArrayReference.ArrayReference.Length.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("arrayObject=" + pkt.readObjectRef());
            } else {
                System.out.println("arrayLength=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPArrayReference.ArrayReference.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("arrayObject=" + pkt.readObjectRef() + " firstIndex=" + pkt.readInt() +
                        " length=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPArrayReference.ArrayReference.SetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("arrayObject=" + pkt.readObjectRef() + " firstIndex=" + pkt.readInt() +
                        " values=" + pkt.readInt());
            }
        }
    }

    private static void dumpThreadGroupReference(Packet pkt) {
        if (pkt.cmd == JDWPThreadGroupReference.ThreadGroupReference.Name.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("group=" + pkt.readObjectRef());
            } else {
                System.out.println("groupName=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPThreadGroupReference.ThreadGroupReference.Parent.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("group=" + pkt.readObjectRef());
            } else {
                System.out.println("parentGroup=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadGroupReference.ThreadGroupReference.Children.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("group=" + pkt.readObjectRef());
            } else {
                int childThreads = pkt.readInt();
                for(int i=0; i < childThreads;++i) {
                    System.out.println("childThread=" + pkt.readObjectRef());
                }
                int childGroups = pkt.readInt();
                for(int i=0; i < childGroups;++i) {
                    System.out.println("childGroup=" + pkt.readObjectRef());
                }
            }
        }
    }

    private static void dumpThreadReference(Packet pkt) {
        if (pkt.cmd == JDWPThreadReference.ThreadReference.Name.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                System.out.println("threedName=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Suspend.COMMAND && pkt.flags == Packet.NoFlags) {
            System.out.println("thread=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Resume.COMMAND && pkt.flags == Packet.NoFlags) {
            System.out.println("thread=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Status.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                System.out.println("threadStatus=" + pkt.readInt() + " suspendStatus=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.ThreadGroup.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                System.out.println("group=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Frames.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + " startFrame=" + pkt.readInt() +
                        " length=" + pkt.readInt());
            } else {
                int frames = pkt.readInt();
                for(int i=0; i < frames;++i) {
                    System.out.println("frameID=" + pkt.readObjectRef() + " location=" + locationToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.FrameCount.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                System.out.println("frameCount=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.OwnedMonitors.COMMAND ||
                pkt.cmd == JDWPThreadReference.ThreadReference.CurrentContendedMonitor.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                int owned = pkt.readInt();
                for(int i=0; i < owned;++i) {
                    System.out.println("monitor=" + pkt.readByte() + pkt.readObjectRef());
                }
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Stop.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + " throwable=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.Interrupt.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.SuspendCount.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                System.out.println("suspendCount=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.OwnedMonitorsStackDepthInfo.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef());
            } else {
                int owned = pkt.readInt();
                for(int i=0; i < owned;++i) {
                    System.out.println("monitor=" + pkt.readByte() + pkt.readObjectRef() + " stack_depth=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPThreadReference.ThreadReference.ForceEarlyReturn.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("thread=" + pkt.readObjectRef() + "value=" + valueToString(pkt));
            }
        }
    }

    private static String locationToString(Packet pkt) {
        StringBuilder sb = new StringBuilder();
        sb.append(pkt.readByte()).append(" classID=").append(pkt.readObjectRef()).
                append(" methodID=").append(pkt.readObjectRef()).
                append(" index=").append(pkt.readLong());
        return sb.toString();
    }

    private static void dumpStringReference(Packet pkt) {
        if (pkt.cmd == JDWPStringReference.StringReference.Value.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("stringObject=" + pkt.readObjectRef());
            } else {
                System.out.println("stringValue=" + pkt.readString());
            }
        }
    }

    private static void dumpObjectReference(Packet pkt) {
        if (pkt.cmd == JDWPObjectReference.ObjectReference.ReferenceType.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("object=" + pkt.readObjectRef());
            } else {
                System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef());
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("object=" + pkt.readObjectRef());
                int fields = pkt.readInt();
                for(int i=0; i < fields;++i) {
                    System.out.println("fieldID=" + pkt.readFieldRef());
                }
            } else {
                int values = pkt.readInt();
                for(int i=0; i < values;++i) {
                    System.out.println("value" + i + "=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.SetValues.COMMAND && pkt.flags == Packet.NoFlags) {
            System.out.println("object=" + pkt.readObjectRef() + " values=" + pkt.readInt());
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.MonitorInfo.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("object=" + pkt.readObjectRef());
            } else {
                System.out.println("owner=" + pkt.readObjectRef() + " entryCount=" + pkt.readInt());
                int waiters = pkt.readInt();
                for(int i=0; i < waiters;++i) {
                    System.out.println("thread=" + pkt.readObjectRef());
                }
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.InvokeMethod.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("object=" + pkt.readObjectRef() + " thread=" + pkt.readObjectRef() +
                        " clazz=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    System.out.println("arg" + i + "=" + valueToString(pkt));
                }
                System.out.println("options=" + pkt.readInt());
            } else {
                System.out.println("returnValue=" + valueToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.DisableCollection.COMMAND && pkt.flags == Packet.NoFlags) {
            System.out.println("object=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.EnableCollection.COMMAND && pkt.flags == Packet.NoFlags) {
            System.out.println("object=" + pkt.readObjectRef());
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.IsCollected.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("object=" + pkt.readObjectRef());
            } else {
                System.out.println("isCollected=" + pkt.readBoolean());
            }
        } else if (pkt.cmd == JDWPObjectReference.ObjectReference.ReferringObjects.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("object=" + pkt.readObjectRef() + " maxReferers=" + pkt.readInt());
            } else {
                int referringObjects = pkt.readInt();
                for(int i=0; i < referringObjects;++i) {
                    System.out.println("instance=" + pkt.readByte() + pkt.readObjectRef());
                }
            }
        }
    }

    private static void dumpField(Packet pkt) {
    }

    private static void dumpMethod(Packet pkt) {
        if (pkt.cmd == JDWPMethod.Method.LineTable.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                System.out.println("start=" + pkt.readLong() + " end=" + pkt.readLong());
                int lines = pkt.readInt();
                for(int i=0;i < lines;++i) {
                    System.out.println("lineCodeIndex=" + pkt.readLong() + " lineNumber=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPMethod.Method.VariableTable.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                System.out.println("argCnt=" + pkt.readInt());
                int slots = pkt.readInt();
                for (int i = 0; i < slots; ++i) {
                    System.out.println("codeIndex=" + pkt.readLong() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " length=" + pkt.readInt() +
                            " slot=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPMethod.Method.Bytecodes.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                System.out.println("bytes=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPMethod.Method.IsObsolete.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                System.out.println("isObsolete=" + pkt.readBoolean());
            }
        } else if (pkt.cmd == JDWPMethod.Method.VariableTableWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " methodID=" + pkt.readMethodRef());
            } else {
                System.out.println("argCnt=" + pkt.readInt());
                int slots = pkt.readInt();
                for (int i = 0; i < slots; ++i) {
                    System.out.println("codeIndex=" + pkt.readLong() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                            " length=" + pkt.readInt() + " slot=" + pkt.readInt());
                }
            }
        }
    }

    private static void dumpInterfaceType(Packet pkt) {
        if (pkt.cmd == JDWPInterfaceType.InterfaceType.InvokeMethod.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("clazz=" + pkt.readClassRef() + " thread=" + pkt.readObjectRef() +
                        " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    System.out.println("arg" + i + "=" + valueToString(pkt));
                }
                System.out.println("options=" + pkt.readInt());
            } else {
                System.out.println("returnValue=" + valueToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        }
    }

    private static void dumpArrayType(Packet pkt) {
        if (pkt.cmd == JDWPArrayType.ArrayType.NewInstance.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("arrType=" + pkt.readObjectRef() + " length=" + pkt.readInt());
            } else {
                System.out.println("newArray=" + pkt.readByte() + pkt.readObjectRef());
            }
        }
    }

    private static void dumpClassType(Packet pkt) {
        if (pkt.cmd == JDWPClassType.ClassType.Superclass.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("clazz=" + pkt.readClassRef());
            } else {
                System.out.println("superclass=" + pkt.readClassRef());
            }
        } else if (pkt.cmd == JDWPClassType.ClassType.SetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("clazz=" + pkt.readClassRef() + " values=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPClassType.ClassType.InvokeMethod.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("clazz=" + pkt.readClassRef() + " thread=" + pkt.readObjectRef() +
                        " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    System.out.println("arg" + i + "=" + valueToString(pkt));
                }
                System.out.println("options=" + pkt.readInt());
            } else {
                System.out.println("returnValue=" + valueToString(pkt) + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPClassType.ClassType.NewInstance.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("clazz=" + pkt.readClassRef() + " thread=" + pkt.readObjectRef() +
                        " methodID=" + pkt.readMethodRef());
                int arguments = pkt.readInt();
                for(int i=0; i < arguments;++i) {
                    System.out.println("arg" + i + "=" + valueToString(pkt));
                }
                System.out.println("options=" + pkt.readInt());
            } else {
                System.out.println("newObject=" + pkt.readByte() + pkt.readObjectRef() + " exception=" + pkt.readByte() + pkt.readObjectRef());
            }
        }
    }

    private static void dumpReferenceType(Packet pkt) {
        if (pkt.cmd == JDWPReferenceType.ReferenceType.Signature.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readObjectRef());
            } else {
                System.out.println("signature=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ClassLoader.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("classLoader=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Modifiers.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("modBits=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Fields.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    System.out.println("fieldID=" + pkt.readFieldRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " modBits=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Methods.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    System.out.println("methodID=" + pkt.readMethodRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " modBits=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.GetValues.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
                int fields = pkt.readInt();
                for(int i=0; i < fields;++i) {
                    System.out.println("fieldID=" + pkt.readFieldRef());
                }
            } else {
                int values = pkt.readInt();
                for(int i=0; i < values;++i) {
                    System.out.println("value=" + valueToString(pkt));
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.SourceFile.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("sourceFile=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.NestedTypes.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                int classes = pkt.readInt();
                for(int i=0; i < classes;++i) {
                    System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Status.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("status=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Interfaces.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                int interfaces = pkt.readInt();
                for(int i=0; i < interfaces;++i) {
                    System.out.println("interfaceType=" + pkt.readClassRef());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ClassObject.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("classObject=" + pkt.readClassRef());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.SourceDebugExtension.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("extension=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.SignatureWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                System.out.println("signature=" + pkt.readString() + " genericSignature=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.FieldsWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    System.out.println("fieldID=" + pkt.readFieldRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                            " modBits=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.MethodsWithGeneric.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef());
            } else {
                int declared = pkt.readInt();
                for(int i=0; i < declared;++i) {
                    System.out.println("methodID=" + pkt.readMethodRef() + " name=" + pkt.readString() +
                            " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                            " modBits=" + pkt.readInt());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Instances.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                int instances = pkt.readInt();
                for(int i=0; i < instances;++i) {
                    System.out.println("instance=" + pkt.readByte() + pkt.readObjectRef());
                }
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ClassFileVersion.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                System.out.println("majorVersion=" + pkt.readInt() + " minorVersion=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.ConstantPool.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                System.out.println("count=" + pkt.readInt());
                int bytes = pkt.readInt();
                pkt.readByteArray(bytes);
            }
        } else if (pkt.cmd == JDWPReferenceType.ReferenceType.Module.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                System.out.println("refType=" + pkt.readClassRef() + " maxInstances=" + pkt.readInt());
            } else {
                System.out.println("module=" + pkt.readObjectRef());
            }
        }
    }

    private static String valueToString(Packet pkt) {
        StringBuilder sb = new StringBuilder();
        byte tag = pkt.readByte();
        switch (tag) {
            case JDWP.Tag.ARRAY:
                sb.append('[');
                sb.append(pkt.readObjectRef()).append(']');
                break;
            case JDWP.Tag.BYTE:
                sb.append('B').append(pkt.readByte());
                break;
            case JDWP.Tag.CHAR:
                sb.append('C').append(pkt.readChar());
                break;
            case JDWP.Tag.OBJECT:
                sb.append('L').append(pkt.readObjectRef());
                break;
            case JDWP.Tag.FLOAT:
                sb.append('F').append(pkt.readFloat());
                break;
            case JDWP.Tag.DOUBLE:
                sb.append('D').append(pkt.readDouble());
                break;
            case JDWP.Tag.INT:
                sb.append('I').append(pkt.readInt());
                break;
            case JDWP.Tag.LONG:
                sb.append('J').append(pkt.readLong());
                break;
            case JDWP.Tag.SHORT:
                sb.append('S').append(pkt.readShort());
                break;
            case JDWP.Tag.VOID:
                sb.append('V');
                break;
            case JDWP.Tag.BOOLEAN:
                sb.append('Z').append(pkt.readBoolean());
                break;
            case JDWP.Tag.STRING:
                sb.append('s').append(pkt.readObjectRef());
                break;
            case JDWP.Tag.THREAD:
                sb.append('t').append(pkt.readObjectRef());
                break;
            case JDWP.Tag.THREAD_GROUP:
                sb.append('g').append(pkt.readObjectRef());
                break;
            case JDWP.Tag.CLASS_LOADER:
                sb.append('l').append(pkt.readObjectRef());
                break;
            case JDWP.Tag.CLASS_OBJECT:
                sb.append('c').append(pkt.readObjectRef());
                break;
        }
        return sb.toString();
    }

    private static void dumpVirtualMachine(Packet pkt) {
        if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.Version.COMMAND && pkt.flags == Packet.Reply) {
            String description = pkt.readString();
            int major = pkt.readInt();
            int minor = pkt.readInt();
            String vmVersion = pkt.readString();
            String vmName = pkt.readString();
            System.out.println("description=" + description + " jdwpMajor=" + major + " jdwpMinor=" + minor + " vmVersion=" + vmVersion + " vmName=" + vmName);
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.ClassesBySignature.COMMAND) {
            if (pkt.flags == Packet.Reply) {
                int classes = pkt.readInt();
                for(int i=0; i < classes;++i) {
                    System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef() + " status=" + pkt.readInt());
                }
            } else {
                System.out.println("signature=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllClasses.COMMAND && pkt.flags == Packet.Reply) {
            int classes = pkt.readInt();
            for(int i=0; i < classes;++i) {
                System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef() + " signature=" + pkt.readString() + " status=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllThreads.COMMAND && pkt.flags == Packet.Reply) {
            int threads = pkt.readInt();
            for(int i=0; i < threads;++i) {
                System.out.println("threadID=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.TopLevelThreadGroups.COMMAND && pkt.flags == Packet.Reply) {
            int groups = pkt.readInt();
            for(int i=0; i < groups;++i) {
                System.out.println("threadGroupID=" + pkt.readObjectRef());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.IDSizes.COMMAND && pkt.flags == Packet.Reply) {
            System.out.println("fieldIDSize=" + pkt.readInt() + " methodIDSize=" + pkt.readInt() + " objectIDSize=" + pkt.readInt() + " referenceTypeIDSize=" + pkt.readInt() + " frameIDSize=" + pkt.readInt());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.Exit.COMMAND && pkt.flags == Packet.NoFlags) {
            System.out.println("exitCode=" + pkt.readInt());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.CreateString.COMMAND) {
            if (pkt.flags == Packet.Reply) {
                System.out.println("stringObject=" + pkt.readObjectRef());
            } else {
                System.out.println("utf=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.Capabilities.COMMAND && pkt.flags == Packet.Reply) {
            System.out.println("canWatchFieldModification=" + pkt.readBoolean() + " canWatchFieldAccess=" + pkt.readBoolean() +
                    " canGetByteCodes=" + pkt.readBoolean() + " canGetSyntheticAttribute=" + pkt.readBoolean() +
                    " canGetOwnedMonitorInfo=" + pkt.readBoolean() + " canGetCurrentContendedMonitor=" + pkt.readBoolean() +
                    " canGetMonitorInfo=" + pkt.readBoolean());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.ClassPaths.COMMAND && pkt.flags == Packet.Reply) {
            System.out.println("baseDir=" + pkt.readString());
            int classpaths = pkt.readInt();
            for(int i=0; i < classpaths;++i) {
                System.out.println("path=" + pkt.readString());
            }
            int bootclasspaths = pkt.readInt();
            for(int i=0; i < bootclasspaths;++i) {
                System.out.println("path=" + pkt.readString());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.DisposeObjects.COMMAND && pkt.flags == Packet.NoFlags) {
            int requests = pkt.readInt();
            for(int i=0; i < requests;++i) {
                System.out.println("object=" + pkt.readObjectRef() + " refCnt=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.CapabilitiesNew.COMMAND && pkt.flags == Packet.Reply) {
            System.out.println("canWatchFieldModification=" + pkt.readBoolean() + " canWatchFieldAccess=" + pkt.readBoolean() +
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
            System.out.println("stratumID=" + pkt.readString());
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllClassesWithGeneric.COMMAND && pkt.flags == Packet.Reply) {
            int classes = pkt.readInt();
            for(int i=0; i < classes;++i) {
                System.out.println("refTypeTag=" + pkt.readByte() + " typeID=" + pkt.readClassRef() +
                        " signature=" + pkt.readString() + " genericSignature=" + pkt.readString() +
                        " status=" + pkt.readInt());
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.InstanceCounts.COMMAND) {
            if (pkt.flags == Packet.NoFlags) {
                int refTypesCount = pkt.readInt();
                for(int i=0; i < refTypesCount;++i) {
                    System.out.println("refType=" + pkt.readClassRef());
                }
            } else {
                int counts = pkt.readInt();
                for(int i=0; i < counts;++i) {
                    System.out.println("instanceCount=" + pkt.readLong());
                }
            }
        } else if (pkt.cmd == JDWPVirtualMachine.VirtualMachine.AllModules.COMMAND && pkt.flags == Packet.Reply) {
            int modules = pkt.readInt();
            for(int i=0; i < modules;++i) {
                System.out.println("module=" + pkt.readObjectRef());
            }
        }
    }
}
