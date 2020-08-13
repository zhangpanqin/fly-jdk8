
package java.util;

/**
 * @author zhangpanqin
 */
public class Stack<E> extends Vector<E> {

    public Stack() {
    }

    /**
     * 压栈
     */
    public E push(E item) {
        addElement(item);

        return item;
    }

    /**
     * 弹栈
     */
    public synchronized E pop() {
        E obj;
        int len = size();

        obj = peek();
        removeElementAt(len - 1);

        return obj;
    }


    public synchronized E peek() {
        int len = size();
        if (len == 0) {
            throw new EmptyStackException();
        }
        return elementAt(len - 1);
    }

    public boolean empty() {
        return size() == 0;
    }

    public synchronized int search(Object o) {
        int i = lastIndexOf(o);

        if (i >= 0) {
            return size() - i;
        }
        return -1;
    }

    private static final long serialVersionUID = 1224463164541339165L;
}
