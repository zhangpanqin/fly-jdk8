package java.lang.reflect;


public interface Type {

    /**
     * 返回类型的信息
     */
    default String getTypeName() {
        return toString();
    }
}
