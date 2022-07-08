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

public class JDWPReferenceType {
    static class ReferenceType {
        static final int COMMAND_SET = 2;
        private ReferenceType() {}  // hide constructor

        /**
         * Returns the JNI signature of a reference type.
         * JNI signature formats are described in the
         * <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/jni/index.html">Java Native Inteface Specification</a>
         * <p>
         * For primitive classes
         * the returned signature is the signature of the corresponding primitive
         * type; for example, "I" is returned as the signature of the class
         * represented by java.lang.Integer.TYPE.
         */
        static class Signature {
            static final int COMMAND = 1;
        }

        /**
         * Returns the instance of java.lang.ClassLoader which loaded
         * a given reference type. If the reference type was loaded by the
         * system class loader, the returned object ID is null.
         */
        static class ClassLoader {
            static final int COMMAND = 2;
        }

        /**
         * Returns the modifiers (also known as access flags) for a reference type.
         * The returned bit mask contains information on the declaration
         * of the reference type. If the reference type is an array or
         * a primitive class (for example, java.lang.Integer.TYPE), the
         * value of the returned bit mask is undefined.
         */
        static class Modifiers {
            static final int COMMAND = 3;
        }

        /**
         * Returns information for each field in a reference type.
         * Inherited fields are not included.
         * The field list will include any synthetic fields created
         * by the compiler.
         * Fields are returned in the order they occur in the class file.
         */
        static class Fields {
            static final int COMMAND = 4;
        }

        /**
         * Returns information for each method in a reference type.
         * Inherited methods are not included. The list of methods will
         * include constructors (identified with the name "&lt;init&gt;"),
         * the initialization method (identified with the name "&lt;clinit&gt;")
         * if present, and any synthetic methods created by the compiler.
         * Methods are returned in the order they occur in the class file.
         */
        static class Methods {
            static final int COMMAND = 5;
        }

        /**
         * Returns the value of one or more static fields of the
         * reference type. Each field must be member of the reference type
         * or one of its superclasses, superinterfaces, or implemented interfaces.
         * Access control is not enforced; for example, the values of private
         * fields can be obtained.
         */
        static class GetValues {
            static final int COMMAND = 6;
        }

        /**
         * Returns the name of source file in which a reference type was
         * declared.
         */
        static class SourceFile {
            static final int COMMAND = 7;
        }

        /**
         * Returns the classes and interfaces directly nested within this type.
         * Types further nested within those types are not included.
         */
        static class NestedTypes {
            static final int COMMAND = 8;
        }

        /**
         * Returns the current status of the reference type. The status
         * indicates the extent to which the reference type has been
         * initialized, as described in section 2.1.6 of
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * If the class is linked the PREPARED and VERIFIED bits in the returned status bits
         * will be set. If the class is initialized the INITIALIZED bit in the returned
         * status bits will be set. If an error occured during initialization then the
         * ERROR bit in the returned status bits will be set.
         * The returned status bits are undefined for array types and for
         * primitive classes (such as java.lang.Integer.TYPE).
         */
        static class Status {
            static final int COMMAND = 9;
        }

        /**
         * Returns the interfaces declared as implemented by this class.
         * Interfaces indirectly implemented (extended by the implemented
         * interface or implemented by a superclass) are not included.
         */
        static class Interfaces {
            static final int COMMAND = 10;
        }

        /**
         * Returns the class object corresponding to this type.
         */
        static class ClassObject {
            static final int COMMAND = 11;
        }

        /**
         * Returns the value of the SourceDebugExtension attribute.
         * Since JDWP version 1.4. Requires canGetSourceDebugExtension capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class SourceDebugExtension {
            static final int COMMAND = 12;
        }

        /**
         * Returns the JNI signature of a reference type along with the
         * generic signature if there is one.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         * <p>
         */
        static class SignatureWithGeneric {
            static final int COMMAND = 13;
        }

        /**
         * Returns information, including the generic signature if any,
         * for each field in a reference type.
         * Inherited fields are not included.
         * The field list will include any synthetic fields created
         * by the compiler.
         * Fields are returned in the order they occur in the class file.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class FieldsWithGeneric {
            static final int COMMAND = 14;
        }

        /**
         * Returns information, including the generic signature if any,
         * for each method in a reference type.
         * Inherited methods are not included. The list of methods will
         * include constructors (identified with the name "&lt;init&gt;"),
         * the initialization method (identified with the name "&lt;clinit&gt;")
         * if present, and any synthetic methods created by the compiler.
         * Methods are returned in the order they occur in the class file.
         * Generic signatures are described in the signature attribute
         * section in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * Since JDWP version 1.5.
         */
        static class MethodsWithGeneric {
            static final int COMMAND = 15;
        }

        /**
         * Returns instances of this reference type.
         * Only instances that are reachable for the purposes of
         * garbage collection are returned.
         * <p>Since JDWP version 1.6. Requires canGetInstanceInfo capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         */
        static class Instances {
            static final int COMMAND = 16;
        }

        /**
         * Returns the class file major and minor version numbers, as defined in the class
         * file format of the Java Virtual Machine specification.
         * <p>Since JDWP version 1.6.
         */
        static class ClassFileVersion {
            static final int COMMAND = 17;
        }

        /**
         * Return the raw bytes of the constant pool in the format of the
         * constant_pool item of the Class File Format in
         * <cite>The Java&trade; Virtual Machine Specification</cite>.
         * <p>Since JDWP version 1.6. Requires canGetConstantPool capability - see
         * <a href="#JDWP_VirtualMachine_CapabilitiesNew">CapabilitiesNew</a>.
         *
         */
        static class ConstantPool {
            static final int COMMAND = 18;
        }

        /**
         * Returns the module that this reference type belongs to.
         * <p>Since JDWP version 9.
         */
        static class Module {
            static final int COMMAND = 19;
        }
    }
}
