package io.jdwptracer;

public class JDWPArrayType {
    static class ArrayType {
        static final int COMMAND_SET = 4;
        private ArrayType() {}  // hide constructor

        /**
         * Creates a new array object of this type with a given length.
         */
        static class NewInstance {
            static final int COMMAND = 1;
        }
    }
}
