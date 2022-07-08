/*
 * Copyright (C) 2018 JetBrains s.r.o.
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License v2 with Classpath Exception.
 * The text of the license is available in the file LICENSE.TXT.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See LICENSE.TXT for more details.
 *
 * You may contact JetBrains s.r.o. at Na Hřebenech II 1718/10, 140 00 Prague,
 * Czech Republic or at legal@jetbrains.com.
 *
 * Copyright (C) 2022 IBM Corporation
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License v2 with Classpath Exception.
 * The text of the license is available in the file LICENSE.TXT.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See LICENSE.TXT for more details.
 */

package io.jdwptracer;

public class JDWPVirtualMachine {
    static class VirtualMachine {
        static final int COMMAND_SET = 1;
        private VirtualMachine() {}  // hide constructor

        /**
         * Returns the JDWP version implemented by the target VM.
         * The version string format is implementation dependent.
         */
        static class Version  {
            static final int COMMAND = 1;
        }

        /**
         * Returns reference types for all the classes loaded by the target VM
         * which match the given signature.
         * Multple reference types will be returned if two or more class
         * loaders have loaded a class of the same name.
         * The search is confined to loaded classes only; no attempt is made
         * to load a class of the given signature.
         */
        static class ClassesBySignature  {
            static final int COMMAND = 2;
        }

        /**
         * Returns reference types for all classes currently loaded by the
         * target VM.
         */
        static class AllClasses  {
            static final int COMMAND = 3;
        }

        /**
         * Returns all threads currently running in the target VM .
         * The returned list contains threads created through
         * java.lang.Thread, all native threads attached to
         * the target VM through JNI, and system threads created
         * by the target VM. Threads that have not yet been started
         * and threads that have completed their execution are not
         * included in the returned list.
         */
        static class AllThreads  {
            static final int COMMAND = 4;
        }

        /**
         * Returns all thread groups that do not have a parent. This command
         * may be used as the first step in building a tree (or trees) of the
         * existing thread grouanswer.
         */
        static class TopLevelThreadGroups  {
            static final int COMMAND = 5;
        }

        /**
         * Invalidates this virtual machine mirror.
         * The communication channel to the target VM is closed, and
         * the target VM prepares to accept another subsequent connection
         * from this debugger or another debugger, including the
         * following tasks:
         * <ul>
         * <li>All event requests are cancelled.
         * <li>All threads suspended by the thread-level
         * <a href="#JDWP_ThreadReference_Resume">resume</a> command
         * or the VM-level
         * <a href="#JDWP_VirtualMachine_Resume">resume</a> command
         * are resumed as many times as necessary for them to run.
         * <li>Garbage collection is re-enabled in all cases where it was
         * <a href="#JDWP_ObjectReference_DisableCollection">disabled</a>
         * </ul>
         * Any current method invocations executing in the target VM
         * are continued after the disconnection. Upon completion of any such
         * method invocation, the invoking thread continues from the
         * location where it was originally stopped.
         * <p>
         * Resources originating in
         * this VirtualMachine (ObjectReferences, ReferenceTypes, etc.)
         * will become invalid.
         */
        static class Dispose  {
            static final int COMMAND = 6;
        }

        /**
         * Returns the sizes of variably-sized data types in the target VM.
         * The returned values indicate the number of bytes used by the
         * identifiers in command and reply packets.
         */
        static class IDSizes  {
            static final int COMMAND = 7;
        }

        /**
         * Suspends the execution of the application running in the target VM. All Java threads
         * currently running will be suspended.
         *
         * <p>Unlike java.lang.Thread.suspend, suspends of both the virtual machine and individual
         * threads are counted. Before a thread will run again, it must be resumed through the <a
         * href="#JDWP_VirtualMachine_Resume">VM-level resume</a> command or the <a
         * href="#JDWP_ThreadReference_Resume">thread-level resume</a> command the same number of times
         * it has been suspended.
         */
        static class Suspend {
            static final int COMMAND = 8;
        }

        /**
         * Resumes execution of the application after the suspend
         * command or an event has stopped it.
         * Suspensions of the Virtual Machine and individual threads are
         * counted. If a particular thread is suspended n times, it must
         * resumed n times before it will continue.
         */
        static class Resume  {
            static final int COMMAND = 9;
        }

        /**
         * Terminates the target VM with the given exit code.
         * On some platforms, the exit code might be truncated, for
         * example, to the low order 8 bits.
         * All ids previously returned from the target VM become invalid.
         * Threads running in the VM are abruptly terminated.
         * A thread death exception is not thrown and
         * finally blocks are not run.
         */
        static class Exit  {
            static final int COMMAND = 10;
        }

        /**
         * Creates a new string object in the target VM and returns
         * its id.
         */
        static class CreateString  {
            static final int COMMAND = 11;
        }

        /**
         * Retrieve this VM's capabilities. The capabilities are returned
         * as booleans, each indicating the presence or absence of a
         * capability. The commands associated with each capability will
         * return the NOT_IMPLEMENTED error if the cabability is not
         * available.
         */
        static class Capabilities  {
            static final int COMMAND = 12;
        }

        /**
         * Retrieve the classpath and bootclasspath of the target VM.
         * If the classpath is not defined, returns an empty list. If the
         * bootclasspath is not defined returns an empty list.
         */
        static class ClassPaths  {
            static final int COMMAND = 13;
        }

        /**
         * Releases a list of object IDs. For each object in the list, the
         * following applies.
         * The count of references held by the back-end (the reference
         * count) will be decremented by refCnt.
         * If thereafter the reference count is less than
         * or equal to zero, the ID is freed.
         * Any back-end resources associated with the freed ID may
         * be freed, and if garbage collection was
         * disabled for the object, it will be re-enabled.
         * The sender of this command
         * promises that no further commands will be sent
         * referencing a freed ID.
         * <p>
         * Use of this command is not required. If it is not sent,
         * resources associated with each ID will be freed by the back-end
         * at some time after the corresponding object is garbage collected.
         * It is most useful to use this command to reduce the load on the
         * back-end if a very large number of
         * objects has been retrieved from the back-end (a large array,
         * for example) but may not be garbage collected any time soon.
         * <p>
         * IDs may be re-used by the back-end after they
         * have been freed with this command.
         * This description assumes reference counting,
         * a back-end may use any implementation which operates
         * equivalently.
         */
        static class DisposeObjects  {
            static final int COMMAND = 14;
        }

        /**
         * Tells the target VM to stop sending events. Events are not discarded;
         * they are held until a subsequent ReleaseEvents command is sent.
         * This command is useful to control the number of events sent
         * to the debugger VM in situations where very large numbers of events
         * are generated.
         * While events are held by the debugger back-end, application
         * execution may be frozen by the debugger back-end to prevent
         * buffer overflows on the back end.
         * Responses to commands are never held and are not affected by this
         * command. If events are already being held, this command is
         * ignored.
         */
        static class HoldEvents  {
            static final int COMMAND = 15;
        }

        /**
         * Tells the target VM to continue sending events. This command is
         * used to restore normal activity after a HoldEvents command. If
         * there is no current HoldEvents command in effect, this command is
         * ignored.
         */
        static class ReleaseEvents  {
            static final int COMMAND = 16;
        }

        /**
         * Retrieve all of this VM's capabilities. The capabilities are returned
         * as booleans, each indicating the presence or absence of a
         * capability. The commands associated with each capability will
         * return the NOT_IMPLEMENTED error if the cabability is not
         * available.
         * Since JDWP version 1.4.
         */
        static class CapabilitiesNew  {
            static final int COMMAND = 17;
        }

        /**
         * Installs new class definitions.
         * If there are active stack frames in methods of the redefined classes in the
         * target VM then those active frames continue to run the bytecodes of the
         * original method. These methods are considered obsolete - see
         * <a href="#JDWP_Method_IsObsolete">IsObsolete</a>. The methods in the
         * redefined classes will be used for new invokes in the target VM.
         * The original method ID refers to the redefined method.
         * All breakpoints in the redefined classes are cleared.
         * If resetting of stack frames is desired, the
         * <a href="#JDWP_StackFrame_PopFrames">PopFrames</a> command can be used
         * to pop frames with obsolete methods.
         * <p>
         * Requires canRedefineClasses capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         * In addition to the canRedefineClasses capability, the target VM must
         * have the canAddMethod capability to add methods when redefining classes,
         * or the canUnrestrictedlyRedefineClasses to redefine classes in arbitrary
         * ways.
         */
        static class RedefineClasses  {
            static final int COMMAND = 18;
        }

        /**
         * Set the default stratum. Requires canSetDefaultStratum capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class SetDefaultStratum  {
            static final int COMMAND = 19;
        }

        /**
         * Returns reference types for all classes currently loaded by the
         * target VM.
         * Both the JNI signature and the generic signature are
         * returned for each class.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class AllClassesWithGeneric  {
            static final int COMMAND = 20;
        }

        /**
         * Returns the number of instances of each reference type in the input list.
         * Only instances that are reachable for the purposes of
         * garbage collection are counted.  If a reference type is invalid,
         * eg. it has been unloaded, zero is returned for its instance count.
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class InstanceCounts  {
            static final int COMMAND = 21;
        }

        /**
         * Returns all modules in the target VM.
         * <p>Since JDWP version 9.
         */
        static class AllModules  {
            static final int COMMAND = 22;
        }
    }
}
