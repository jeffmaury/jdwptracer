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

public class JDWPThreadGroupReference {

    static class ThreadGroupReference {
        static final int COMMAND_SET = 12;
        private ThreadGroupReference() {}  // hide constructor

        /**
         * Returns the thread group name.
         */
        static class Name  {
            static final int COMMAND = 1;
        }

        /**
         * Returns the thread group, if any, which contains a given thread group.
         */
        static class Parent  {
            static final int COMMAND = 2;
        }

        /**
         * Returns the live threads and active thread groups directly contained
         * in this thread group. Threads and thread groups in child
         * thread groups are not included.
         * A thread is alive if it has been started and has not yet been stopped.
         * See <a href=../../../api/java/lang/ThreadGroup.html>java.lang.ThreadGroup </a>
         * for information about active ThreadGrouanswer.
         */
        static class Children  {
            static final int COMMAND = 3;
        }
    }
}
