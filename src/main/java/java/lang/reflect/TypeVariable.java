package java.lang.reflect;

public interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement {

    /**
     * 类型的上限
     */
    Type[] getBounds();

    /**
     * 返回泛型
     */
    D getGenericDeclaration();

    /**
     * 返回名称
     */
    String getName();

    /**
     * 类型上限
     */
    AnnotatedType[] getAnnotatedBounds();
}
