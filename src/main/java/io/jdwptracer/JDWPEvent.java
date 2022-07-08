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

public class JDWPEvent {
    static class Event {
        static final int COMMAND_SET = 64;
        private Event() {}  // hide constructor

        /**
         * Several events may occur at a given time in the target VM.
         * For example, there may be more than one breakpoint request
         * for a given location
         * or you might single step to the same location as a
         * breakpoint request.  These events are delivered
         * together as a composite event.  For uniformity, a
         * composite event is always used
         * to deliver events, even if there is only one event to report.
         * <P>
         * The events that are grouped in a composite event are restricted in the
         * following ways:
         * <P>
         * <UL>
         * <LI>Only with other thread start events for the same thread:
         *     <UL>
         *     <LI>Thread Start Event
         *     </UL>
         * <LI>Only with other thread death events for the same thread:
         *     <UL>
         *     <LI>Thread Death Event
         *     </UL>
         * <LI>Only with other class prepare events for the same class:
         *     <UL>
         *     <LI>Class Prepare Event
         *     </UL>
         * <LI>Only with other class unload events for the same class:
         *     <UL>
         *     <LI>Class Unload Event
         *     </UL>
         * <LI>Only with other access watchpoint events for the same field access:
         *     <UL>
         *     <LI>Access Watchpoint Event
         *     </UL>
         * <LI>Only with other modification watchpoint events for the same field
         * modification:
         *     <UL>
         *     <LI>Modification Watchpoint Event
         *     </UL>
         * <LI>Only with other Monitor contended enter events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Contended Enter Event
         *     </UL>
         * <LI>Only with other Monitor contended entered events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Contended Entered Event
         *     </UL>
         * <LI>Only with other Monitor wait events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Wait Event
         *     </UL>
         * <LI>Only with other Monitor waited events for the same monitor object:
         *     <UL>
         *     <LI>Monitor Waited Event
         *     </UL>
         * <LI>Only with other ExceptionEvents for the same exception occurrance:
         *     <UL>
         *     <LI>ExceptionEvent
         *     </UL>
         * <LI>Only with other members of this group, at the same location
         * and in the same thread:
         *     <UL>
         *     <LI>Breakpoint Event
         *     <LI>Step Event
         *     <LI>Method Entry Event
         *     <LI>Method Exit Event
         *     </UL>
         * </UL>
         * <P>
         * The VM Start Event and VM Death Event are automatically generated events.
         * This means they do not need to be requested using the
         * <a href="#JDWP_EventRequest_Set">EventRequest.Set</a> command.
         * The VM Start event signals the completion of VM initialization. The VM Death
         * event signals the termination of the VM.
         * If there is a debugger connected at the time when an automatically generated
         * event occurs it is sent from the target VM. Automatically generated events may
         * also be requested using the EventRequest.Set command and thus multiple events
         * of the same event kind will be sent from the target VM when an event occurs.
         * Automatically generated events are sent with the requestID field
         * in the Event Data set to 0. The value of the suspendPolicy field in the
         * Event Data depends on the event. For the automatically generated VM Start
         * Event the value of suspendPolicy is not defined and is therefore implementation
         * or configuration specific. In the Sun implementation, for example, the
         * suspendPolicy is specified as an option to the JDWP agent at launch-time.
         * The automatically generated VM Death Event will have the suspendPolicy set to
         * NONE.
         */
        static class Composite {
            static final int COMMAND = 100;
        }
    }
}
