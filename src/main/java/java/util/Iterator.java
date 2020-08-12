package java.util;

import java.util.function.Consumer;

public interface Iterator<E> {

    /**
     * 判断集合是否还有更多的元素,返回 true 标识有更多的元素
     */
    boolean hasNext();

    /**
     * 返回下一个元素
     */
    E next();

    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * 对集合中的每个元素应用 Consumer
     */
    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext()) {
            action.accept(next());
        }
    }
}
