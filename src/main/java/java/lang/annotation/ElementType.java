package java.lang.annotation;

/**
 * 表名注解使用的位置
 */
public enum ElementType {
    /**
     * Class, interface (including annotation type), or enum declaration
     * 声明注解可以用在 类,接口,注解,枚举
     */
    TYPE,

    /**
     * 注解用于成员变量上 (includes enum constants)
     */
    FIELD,

    /**
     * 声明注解用于,方法
     */
    METHOD,

    /**
     * 声明的注解可以用在,方法的形参上
     */
    PARAMETER,

    /**
     * 声明的注解可以用在,构造函数
     */
    CONSTRUCTOR,

    /**
     * 局部变量
     */
    LOCAL_VARIABLE,

    /**
     * 注解上
     */
    ANNOTATION_TYPE,

    /**
     * Package declaration
     */
    PACKAGE,

    /**
     * Type parameter declaration
     *
     * @since 1.8
     */
    TYPE_PARAMETER,

    /**
     * Use of a type
     *
     * @since 1.8
     */
    TYPE_USE
}
