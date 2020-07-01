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
     * Returns an array of AnnotatedType objects that represent the use of
     * types to denote the upper bounds of the type parameter represented by
     * this TypeVariable. The order of the objects in the array corresponds to
     * the order of the bounds in the declaration of the type parameter.
     * <p>
     * Returns an array of length 0 if the type parameter declares no bounds.
     *
     * @return an array of objects representing the upper bounds of the type variable
     * @since 1.8
     */
    AnnotatedType[] getAnnotatedBounds();
}
