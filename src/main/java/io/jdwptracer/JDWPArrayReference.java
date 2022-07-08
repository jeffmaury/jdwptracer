package io.jdwptracer;

public class JDWPArrayReference  {
    static class ArrayReference {
        static final int COMMAND_SET = 13;
        private ArrayReference() {}  // hide constructor

        /**
         * Returns the number of components in a given array.
         */
        static class Length {
            static final int COMMAND = 1;
        }

        /**
         * Returns a range of array components. The specified range must
         * be within the bounds of the array.
         */
        static class GetValues {
            static final int COMMAND = 2;
        }

        /**
         * Sets a range of array components. The specified range must
         * be within the bounds of the array.
         * For primitive values, each value's type must match the
         * array component type exactly. For object values, there must be a
         * widening reference conversion from the value's type to the
         * array component type and the array component type must be loaded.
         */
        static class SetValues {
            static final int COMMAND = 3;
        }
    }
}
