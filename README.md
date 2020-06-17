## 引用

| 引用类型 | 例子                     | 用途               | 生存时间                   |
| -------- | ------------------------ | ------------------ | -------------------------- |
| 强引用   | Object obj=new Object(); | 对象的一般状态     | GC ROOT 可达，永远不会回收 |
| 软引用   | SoftReference            | 对象缓存           | OOM 之前回收               |
| 弱引用   | WeakReference            | 对象缓存           | GC 之后，访问不到          |
| 虚引用   | PhantomReference         | 跟踪对象的垃圾回收 | 未知                       |

当前对象没有强引用，只有 软、弱、虚的时候，才会生效。



## 异常

<img src="http://oss.mflyyou.cn/blog/20200616202642.png?author=zhangpanqin" alt="RuntimeException" style="zoom: 50%;" />