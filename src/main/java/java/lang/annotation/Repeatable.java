package java.lang.annotation;

/**
 * @Repeatable(Ans.class)
 * @interface An {
 *      String value();
 * }
 *
 * @interface Ans {
 *      An[] value();
 * }
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Repeatable {
    Class<? extends Annotation> value();
}
