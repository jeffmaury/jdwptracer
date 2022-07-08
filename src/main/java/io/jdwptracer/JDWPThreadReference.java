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

public class JDWPThreadReference {



    static class ThreadReference {
        static final int COMMAND_SET = 11;
        private ThreadReference() {}  // hide constructor

        /**
         * Returns the thread name.
         */
        static class Name  {
            static final int COMMAND = 1;
        }

        /**
         * Suspends the thread.
         * <p>
         * Unlike java.lang.Thread.suspend(), suspends of both
         * the virtual machine and individual threads are counted. Before
         * a thread will run again, it must be resumed the same number
         * of times it has been suspended.
         * <p>
         * Suspending single threads with command has the same
         * dangers java.lang.Thread.suspend(). If the suspended
         * thread holds a monitor needed by another running thread,
         * deadlock is possible in the target VM (at least until the
         * suspended thread is resumed again).
         * <p>
         * The suspended thread is guaranteed to remain suspended until
         * resumed through one of the JDI resume methods mentioned above;
         * the application in the target VM cannot resume the suspended thread
         * through {@link Thread#resume}.
         * <p>
         * Note that this doesn't change the status of the thread (see the
         * <a href="#JDWP_ThreadReference_Status">ThreadStatus</a> command.)
         * For example, if it was
         * Running, it will still appear running to other threads.
         */
        static class Suspend  {
            static final int COMMAND = 2;
        }

        /**
         * Resumes the execution of a given thread. If this thread was
         * not previously suspended by the front-end,
         * calling this command has no effect.
         * Otherwise, the count of pending suspends on this thread is
         * decremented. If it is decremented to 0, the thread will
         * continue to execute.
         */
        static class Resume  {
            static final int COMMAND = 3;
        }

        /**
         * Returns the current status of a thread. The thread status
         * reply indicates the thread status the last time it was running.
         * the suspend status provides information on the thread's
         * suspension, if any.
         */
        static class Status  {
            static final int COMMAND = 4;
        }

        /**
         * Returns the thread group that contains a given thread.
         */
        static class ThreadGroup  {
            static final int COMMAND = 5;
        }

        /**
         * Returns the current call stack of a suspended thread.
         * The sequence of frames starts with
         * the currently executing frame, followed by its caller,
         * and so on. The thread must be suspended, and the returned
         * frameID is valid only while the thread is suspended.
         */
        static class Frames  {
            static final int COMMAND = 6;
        }

        /**
         * Returns the count of frames on this thread's stack.
         * The thread must be suspended, and the returned
         * count is valid only while the thread is suspended.
         * Returns JDWP.Error.errorThreadNotSuspended if not suspended.
         */
        static class FrameCount  {
            static final int COMMAND = 7;
        }

        /**
         * Returns the objects whose monitors have been entered by this thread.
         * The thread must be suspended, and the returned information is
         * relevant only while the thread is suspended.
         * Requires canGetOwnedMonitorInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class OwnedMonitors  {
            static final int COMMAND = 8;
        }

        /**
         * Returns the object, if any, for which this thread is waiting. The
         * thread may be waiting to enter a monitor, or it may be waiting, via
         * the java.lang.Object.wait method, for another thread to invoke the
         * notify method.
         * The thread must be suspended, and the returned information is
         * relevant only while the thread is suspended.
         * Requires canGetCurrentContendedMonitor capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class CurrentContendedMonitor  {
            static final int COMMAND = 9;
        }

        /**
         * Stops the thread with an asynchronous exception, as if done by
         * java.lang.Thread.stop
         */
        static class Stop  {
            static final int COMMAND = 10;
        }

        /**
         * Interrupt the thread, as if done by java.lang.Thread.interrupt
         */
        static class Interrupt  {
            static final int COMMAND = 11;
        }

        /**
         * Get the suspend count for this thread. The suspend count is the
         * number of times the thread has been suspended through the
         * thread-level or VM-level suspend commands without a corresponding resume
         */
        static class SuspendCount  {
            static final int COMMAND = 12;
        }

        /**
         * Returns monitor objects owned by the thread, along with stack depth at which
         * the monitor was acquired. Returns stack depth of -1  if
         * the implementation cannot determine the stack depth
         * (e.g., for monitors acquired by JNI MonitorEnter).
         * The thread must be suspended, and the returned information is
         * relevant only while the thread is suspended.
         * Requires canGetMonitorFrameInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         * <p>Since JDWP version 1.6.
         */
        static class OwnedMonitorsStackDepthInfo  {
            static final int COMMAND = 13;
        }

        /**
         * Force a method to return before it reaches a return
         * statement.
         * <p>
         * The method which will return early is referred to as the
         * called method. The called method is the current method (as
         * defined by the Frames section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>)
         * for the specified thread at the time this command
         * is received.
         * <p>
         * The specified thread must be suspended.
         * The return occurs when execution of Java programming
         * language code is resumed on this thread. Between sending this
         * command and resumption of thread execution, the
         * state of the stack is undefined.
         * <p>
         * No further instructions are executed in the called
         * method. Specifically, finally blocks are not executed. Note:
         * this can cause inconsistent states in the application.
         * <p>
         * A lock acquired by calling the called method (if it is a
         * synchronized method) and locks acquired by entering
         * synchronized blocks within the called method are
         * released. Note: this does not apply to JNI locks or
         * java.util.concurrent.locks locks.
         * <p>
         * Events, such as MethodExit, are generated as they would be in
         * a normal return.
         * <p>
         * The called method must be a non-native Java programming
         * language method. Forcing return on a thread with only one
         * frame on the stack causes the thread to exit when resumed.
         * <p>
         * For void methods, the value must be a void value.
         * For methods that return primitive values, the value's type must
         * match the return type exactly.  For object values, there must be a
         * widening reference conversion from the value's type to the
         * return type type and the return type must be loaded.
         * <p>
         * Since JDWP version 1.6. Requires canForceEarlyReturn capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class ForceEarlyReturn  {
            static final int COMMAND = 14;
        }
    }
}
