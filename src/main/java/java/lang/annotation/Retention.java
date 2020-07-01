package java.lang.annotation;


/**
 * 标识注解在什么时候生效
 * RetentionPolicy.RUNTIME 标识运行时可以获得
 * RetentionPolicy.CLASS class 上存在,但是运行时拿不到
 * RetentionPolicy.SOURCE 源码上有,编译之后没有了
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {
    RetentionPolicy value();
}
