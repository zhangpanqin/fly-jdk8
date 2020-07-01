package java.lang.annotation;

public enum RetentionPolicy {
    /**
     * 编译器编译的时候去掉标记的注解
     */
    SOURCE,

    /**
     * 编译器保留,但是 VM 不必保留
     */
    CLASS,

    /**
     * 编译器保留,并且 VM 也保留,通过反射可以拿到次注解信息
     */
    RUNTIME
}
