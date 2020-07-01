package java.lang.annotation;


public interface Annotation {

    @Override
    boolean equals(Object obj);


    @Override
    int hashCode();


    @Override
    String toString();

    /**
     * 返回注解的类型
     */
    Class<? extends Annotation> annotationType();
}
