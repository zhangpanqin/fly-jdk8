package java.lang.reflect;

/**
 * 查询权限修复符
 *
 * @author Nakul Saraiya
 * @author Kenneth Russell
 */
public class Modifier {
    public static boolean isPublic(int mod) {
        return (mod & PUBLIC) != 0;
    }
    public static boolean isPrivate(int mod) {
        return (mod & PRIVATE) != 0;
    }
    public static boolean isProtected(int mod) {
        return (mod & PROTECTED) != 0;
    }
    public static boolean isStatic(int mod) {
        return (mod & STATIC) != 0;
    }
    public static boolean isFinal(int mod) {
        return (mod & FINAL) != 0;
    }
    public static boolean isSynchronized(int mod) {
        return (mod & SYNCHRONIZED) != 0;
    }
    public static boolean isVolatile(int mod) {
        return (mod & VOLATILE) != 0;
    }
    public static boolean isTransient(int mod) {
        return (mod & TRANSIENT) != 0;
    }
    public static boolean isNative(int mod) {
        return (mod & NATIVE) != 0;
    }
    public static boolean isInterface(int mod) {
        return (mod & INTERFACE) != 0;
    }
    public static boolean isAbstract(int mod) {
        return (mod & ABSTRACT) != 0;
    }
    public static boolean isStrict(int mod) {
        return (mod & STRICT) != 0;
    }

    /**
     * The {@code int} value representing the {@code public}
     * modifier.
     */
    public static final int PUBLIC = 0x00000001;

    /**
     * The {@code int} value representing the {@code private}
     * modifier.
     */
    public static final int PRIVATE = 0x00000002;

    /**
     * The {@code int} value representing the {@code protected}
     * modifier.
     */
    public static final int PROTECTED = 0x00000004;

    /**
     * The {@code int} value representing the {@code static}
     * modifier.
     */
    public static final int STATIC = 0x00000008;

    /**
     * The {@code int} value representing the {@code final}
     * modifier.
     */
    public static final int FINAL = 0x00000010;

    /**
     * The {@code int} value representing the {@code synchronized}
     * modifier.
     */
    public static final int SYNCHRONIZED = 0x00000020;

    /**
     * The {@code int} value representing the {@code volatile}
     * modifier.
     */
    public static final int VOLATILE = 0x00000040;

    /**
     * The {@code int} value representing the {@code transient}
     * modifier.
     */
    public static final int TRANSIENT = 0x00000080;

    /**
     * The {@code int} value representing the {@code native}
     * modifier.
     */
    public static final int NATIVE = 0x00000100;

    /**
     * The {@code int} value representing the {@code interface}
     * modifier.
     */
    public static final int INTERFACE = 0x00000200;

    /**
     * The {@code int} value representing the {@code abstract}
     * modifier.
     */
    public static final int ABSTRACT = 0x00000400;

    /**
     * The {@code int} value representing the {@code strictfp}
     * modifier.
     */
    public static final int STRICT = 0x00000800;

    // Bits not (yet) exposed in the public API either because they
    // have different meanings for fields and methods and there is no
    // way to distinguish between the two in this class, or because
    // they are not Java programming language keywords
    static final int BRIDGE = 0x00000040;
    static final int VARARGS = 0x00000080;
    static final int SYNTHETIC = 0x00001000;
    static final int ANNOTATION = 0x00002000;
    static final int ENUM = 0x00004000;
    static final int MANDATED = 0x00008000;

    static boolean isSynthetic(int mod) {
        return (mod & SYNTHETIC) != 0;
    }

    static boolean isMandated(int mod) {
        return (mod & MANDATED) != 0;
    }

    // Note on the FOO_MODIFIERS fields and fooModifiers() methods:
    // the sets of modifiers are not guaranteed to be constants
    // across time and Java SE releases. Therefore, it would not be
    // appropriate to expose an external interface to this information
    // that would allow the values to be treated as Java-level
    // constants since the values could be constant folded and updates
    // to the sets of modifiers missed. Thus, the fooModifiers()
    // methods return an unchanging values for a given release, but a
    // value that can potentially change over time.

    /**
     * The Java source modifiers that can be applied to a class.
     *
     * @jls 8.1.1 Class Modifiers
     */
    private static final int CLASS_MODIFIERS =
            Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
                    Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL |
                    Modifier.STRICT;

    /**
     * The Java source modifiers that can be applied to an interface.
     *
     * @jls 9.1.1 Interface Modifiers
     */
    private static final int INTERFACE_MODIFIERS =
            Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
                    Modifier.ABSTRACT | Modifier.STATIC | Modifier.STRICT;


    /**
     * The Java source modifiers that can be applied to a constructor.
     *
     * @jls 8.8.3 Constructor Modifiers
     */
    private static final int CONSTRUCTOR_MODIFIERS =
            Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /**
     * The Java source modifiers that can be applied to a method.
     *
     * @jls8.4.3 Method Modifiers
     */
    private static final int METHOD_MODIFIERS =
            Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
                    Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL |
                    Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;

    /**
     * The Java source modifiers that can be applied to a field.
     *
     * @jls 8.3.1  Field Modifiers
     */
    private static final int FIELD_MODIFIERS =
            Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
                    Modifier.STATIC | Modifier.FINAL | Modifier.TRANSIENT |
                    Modifier.VOLATILE;

    /**
     * The Java source modifiers that can be applied to a method or constructor parameter.
     *
     * @jls 8.4.1 Formal Parameters
     */
    private static final int PARAMETER_MODIFIERS =
            Modifier.FINAL;

    /**
     *
     */
    static final int ACCESS_MODIFIERS =
            Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /**
     * Return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a class.
     *
     * @return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a class.
     * @jls 8.1.1 Class Modifiers
     * @since 1.7
     */
    public static int classModifiers() {
        return CLASS_MODIFIERS;
    }

    /**
     * Return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to an interface.
     *
     * @return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to an interface.
     * @jls 9.1.1 Interface Modifiers
     * @since 1.7
     */
    public static int interfaceModifiers() {
        return INTERFACE_MODIFIERS;
    }

    /**
     * Return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a constructor.
     *
     * @return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a constructor.
     * @jls 8.8.3 Constructor Modifiers
     * @since 1.7
     */
    public static int constructorModifiers() {
        return CONSTRUCTOR_MODIFIERS;
    }

    /**
     * Return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a method.
     *
     * @return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a method.
     * @jls 8.4.3 Method Modifiers
     * @since 1.7
     */
    public static int methodModifiers() {
        return METHOD_MODIFIERS;
    }

    /**
     * Return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a field.
     *
     * @return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a field.
     * @jls 8.3.1 Field Modifiers
     * @since 1.7
     */
    public static int fieldModifiers() {
        return FIELD_MODIFIERS;
    }

    /**
     * Return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a parameter.
     *
     * @return an {@code int} value OR-ing together the source language
     * modifiers that can be applied to a parameter.
     * @jls 8.4.1 Formal Parameters
     * @since 1.8
     */
    public static int parameterModifiers() {
        return PARAMETER_MODIFIERS;
    }
}
