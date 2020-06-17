package java.test.lang.ref0;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 张攀钦
 * @date 2020-06-16-15:02
 */
public class ReferenceTest {
    private List<RefObj> refObj = new ArrayList<>();

    private SoftReference<RefObj> ref = new SoftReference<RefObj>(createRefObj(4096 * 256));//1m

    public void add() {
        refObj.add(createRefObj(4096));
    }

    private RefObj createRefObj(int dataSize) {
        RefObj refObj = new RefObj();
        byte[] data = new byte[dataSize];
        for (int i = 0; i < dataSize; i++) {
            data[i] = Byte.MAX_VALUE;
        }
        refObj.setData(data);
        return refObj;
    }

    public void validRef() {
        System.out.println(ref.get());
    }

    private class RefObj {
        private byte[] data;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    public static void main(String[] args) {
        ReferenceTest referenceTest = new ReferenceTest();
        for (int i = 0; i < 1200; i++) {
            //不停新增堆大小
            referenceTest.add();
            //新增后查看SoftReference中的对象是否被回收
            referenceTest.validRef();
        }
    }
}