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

public class JDWPEventRequest {

    static class EventRequest {
        static final int COMMAND_SET = 15;
        private EventRequest() {}  // hide constructor

        /**
         * Set an event request. When the event described by this request
         * occurs, an <a href="#JDWP_Event">event</a> is sent from the
         * target VM. If an event occurs that has not been requested then it is not sent
         * from the target VM. The two exceptions to this are the VM Start Event and
         * the VM Death Event which are automatically generated events - see
         * <a href="#JDWP_Event_Composite">Composite Command</a> for further details.
         */
        static class Set {
            static final int COMMAND = 1;
        }

        /**
         * Clear an event request. See <a href="#JDWP_EventKind">JDWP.EventKind</a>
         * for a complete list of events that can be cleared. Only the event request matching
         * the specified event kind and requestID is cleared. If there isn't a matching event
         * request the command is a no-op and does not result in an error. Automatically
         * generated events do not have a corresponding event request and may not be cleared
         * using this command.
         */
        static class Clear {
            static final int COMMAND = 2;
        }

        /**
         * Removes all set breakpoints, a no-op if there are no breakpoints set.
         */
        static class ClearAllBreakpoints {
            static final int COMMAND = 3;
        }
    }
}
