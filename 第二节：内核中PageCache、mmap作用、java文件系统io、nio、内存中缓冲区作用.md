[TOC]

# 推荐书《深入理解linux内核》《深入理解计算机系统》

这两本书能夯实基础，应对大多数的面试

# 面试题：epoll 怎么知道数据到达了，可以通知用户读取了？ ->中断


# PageCache

> 知识点：
> 
> 计算机用页的分配方式，让程序动态的慢慢使用内存
> 
> CPU的mmu单元：线性地址和物理地址的映射管理
> 
> 缺页：cpu执行的时候会报缺页异常，类似一个软中断，cpu跑到内核，建立页映射关系，再漂移回来，cpu继续执行指令
> 
> 
> 缺页异常，会产生软中断，cpu从用户态切换到内核态，内核先去物理内存分配一个页，然后将线性地址指向这个物理地址，table中就记录了线性地址和物理地址的映射；然后CPU再从内核态切换到用户态，程序继续执行


> 知识点1：
> 
> 操作系统内核中会维护page
> 
> page:是硬件设备(物理内存，真正存数据)和进程(应用程序)的一个抽象层
> 
> 数据真正在磁盘上，内核中也有这个数据，内核中的数据提供给进程去使用

程序加载（或一个进程的加载）也是需要page的

    pcstat /bin/bash        pcstat:pageCache + stat;询问操作系统内核，bash文件有没有加载(填充)page页

![201](8792DA0FCD344B99AF4E3CE3581F2768)

## 应用程序，内核、物理内存通过pagecache进行数据流转

应用程序和内核：通过文件描述符的偏移量seek

物理内存和内核：通过pagecache

![202](EF3A4F1DCCA94AD3AD5CBCEB92280EF2)

## page cache使用多大内存，是否淘汰，是否延时，是否丢数据

### 实验
    sysctl -a | grep dirty       sysctl -a:显示centos的控制项
    
    vi /etc/sysctl.conf     下面中罗列了要修改的值
    
    sysctl -p    将配置应用
    
    pcstat out.txt
        利用pcstat查看一个文件从0到1的过程，它的缓冲率以及超过内存以后是否会淘汰一些page
    
    ll -h && pcstat out.txt
    
    
    **再开一个进程执行java程序
    
    ./mysh 0
    
    //实验过程太长，没有记录
    

```
sysctl -a | grep dirty 的结果：

vm.dirty_background_ratio = 0
vm.dirty_background_bytes = 1048576
vm.dirty_ratio = 0
vm.dirty_bytes = 1048576
vm.dirty_writeback_centisecs = 5000
vm.dirty_expire_centisecs = 30000


修改后的值：

vm.dirty_background_ratio = 90  
vm.dirty_ratio = 90  
vm.dirty_writeback_centisecs = 5000
vm.dirty_expire_centisecs = 30000

```

    //可用内存使用90%后，脏页从内存向磁盘写
    vm.dirty_background_ratio = 90  
    
    //程序使用内核可用内存占比90%，程序不再往内核内存中写数据;
    //dirty_background_ratio设置的值应小于dirty_ratio设置的值
    vm.dirty_ratio = 90  
    
    vm.dirty_writeback_centisecs = 5000  //50秒
    vm.dirty_expire_centisecs = 30000   //300秒


> 关联的知识点：
> 
> **理解redis 做持久化，尤其是AUF日志文件以及mysql调优的时候Binlog中有三个级别可以调：每秒写一次、随内核、每操作都写一次**
> 
> **就是因为内核不会立马写到磁盘**
> 



```shell
mysh文件内容

rm-fr *out*
/usr/java/jdk1.8*/bin/javac OSFileIO.java
strace -ff -o out /usr/java/jdk1.8*/bin/java OSFileIO $1

```


```java
OSFileIO.java

File ifile = new File("/ooxx.txt");
out = new outputstream(ifile); //伴随out变量，在操作系统中会得到一个fd，并且这个fd还能看到
out.write("123");

```



1.OSFileIO -> testBasicFileIO 的运行结果

一个程序访问一个文件的时候，只要内存够，cache就一直在内存里缓存

在内存使用率还没有到达阈值的情况下，虚拟机关机（拔电源的那种），发现文件的大小为0，没有将数据写入磁盘

![203](1EFBBC0A1F8746C3804204CC14EB6962)

2.
![204](E9A1FCB604C54724ABB4048216E93556)


### 面试题：Buffered IO和普通IO谁快？

Buffered IO更快；

因为JVM中Buffered IO是8KB，8KB数组，8KB满了以后调一次内核的syscall.write(也就是做一次==系统调用==)将8KB的数组内容

而普通IO，每次write的时候，就会做一次系统调用

Buffered IO和普通IO，write的时候，内核切换的次数不一样

### 结论：page cache 优点：优化IO性能；缺点：丢失数据

## ByteBuffer


```
@Test
public  void whatByteBuffer(){
    //分配在堆上或堆外，不影响下面api的结果
//        ByteBuffer buffer = ByteBuffer.allocate(1024);//直接分配在堆上
    ByteBuffer buffer = ByteBuffer.allocateDirect(1024);//分配在堆外

    //四个指针
    System.out.println("postition: " + buffer.position());
    System.out.println("limit: " +  buffer.limit());
    System.out.println("capacity: " + buffer.capacity());
    System.out.println("mark: " + buffer);

    buffer.put("123".getBytes());

    System.out.println("-------------put:123......");
    System.out.println("mark: " + buffer);

    buffer.flip();   //翻转：为了读写交替

    System.out.println("-------------flip......");
    System.out.println("mark: " + buffer);

    buffer.get();

    System.out.println("-------------get......");
    System.out.println("mark: " + buffer);

    buffer.compact();

    System.out.println("-------------compact......");
    System.out.println("mark: " + buffer);

    buffer.clear();

    System.out.println("-------------clear......");
    System.out.println("mark: " + buffer);

}

运行结果：

postition: 0
limit: 1024
capacity: 1024
mark: java.nio.DirectByteBuffer[pos=0 lim=1024 cap=1024]
-------------put:123......
mark: java.nio.DirectByteBuffer[pos=3 lim=1024 cap=1024]
-------------flip......
mark: java.nio.DirectByteBuffer[pos=0 lim=3 cap=1024]
-------------get......
mark: java.nio.DirectByteBuffer[pos=1 lim=3 cap=1024]
-------------compact......
mark: java.nio.DirectByteBuffer[pos=2 lim=1024 cap=1024]
-------------clear......
mark: java.nio.DirectByteBuffer[pos=0 lim=1024 cap=1024]

```

## 测试文件NIO


```
//测试文件NIO
    public static void testRandomAccessFileWrite() throws  Exception {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");//读写权限

        //**普通写
        raf.write("hello mashibing\n".getBytes());
        raf.write("hello seanzhou\n".getBytes());
        System.out.println("write------------");
        System.in.read();//目前内容在pagecache中，不在磁盘


        //**随机写
        //seek ,文件描述符FD中的seek指针偏移量
        //大数据中HDFS也有seek，支撑起了分布式并行计算；
        // 因为每一个程序打开相同的文件，它们各自seek到自己的数据段里，去并行拿到自己的n份数据，并进行计算
        raf.seek(4);
        raf.write("ooxx".getBytes());
        //内容变成
        //hellooxxshibing
        //hello seanzhou

        System.out.println("seek---------");
        System.in.read();

        //**堆外映射写
        //只有文件通道(FileChannel)有map()方法；文件是块设备，可以自由寻址，
        // socket或serverSocket是没有map方法的，因为是字符设备，不能自由漂移
        FileChannel rafchannel = raf.getChannel();
        //map方法通过 系统调用mmap 得到 堆外的 和文件映射的 ByteBuffer
        // byte  not  objtect
        MappedByteBuffer map = rafchannel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);

        //这就是IO模型，再学完socket，简历上可以写“精通IO模型”
        map.put("@@@".getBytes());  //不是系统调用  但是数据会到达 内核的pagecache
            //曾经我们是需要out.write()  这样的系统调用，才能让程序的data 进入内核的pagecache
            //曾经必须有用户态内核态切换
            //mmap的内存映射，依然是内核的pagecache体系所约束的！！！
            //换言之，丢数据
            //也就是说使用JDK的api是无法绕过内核的pagecache，哪怕是unsafe方法
            //你可以去github上找一些 其他C程序员写的jni扩展库，使用linux内核的Direct IO
            //直接IO是忽略linux的pagecache
            //忽略linux的pagecache不是真的忽略，而是把pagecache  交给了程序自己开辟一个字节数组当作pagecache，动用代码逻辑来维护一致性/dirty。。。一系列复杂问题
            //使用linux内核的Direct IO,不像内核的参数修改是全局的，可以做到细粒度的控制；
            //DB数据库会使用linux内核的Direct IO

        System.out.println("map--put--------");
        System.in.read();
        //文件内容：
        //@@@looxxshibing
        //hello seanzhou
        //文件大小变成了4096
        //文件描述符的结果：图片205.png

//        map.force(); //  类似flush

        raf.seek(0);

        ByteBuffer buffer = ByteBuffer.allocate(8192);
//        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        int read = rafchannel.read(buffer);   //相当于buffer.put()，往buffer中put内容
        System.out.println(buffer);
        buffer.flip();
        System.out.println(buffer);
        //打印结果：
        //java.nio.HeapByteBuffer[pos=4096 lim=8192 cap=8192]
        //java.nio.HeapByteBuffer[pos=0 lim=4096 cap=8192]
        //@@@looxxshibing
        //heool seanzhou

        for (int i = 0; i < buffer.limit(); i++) {
            Thread.sleep(200);
            System.out.print(((char)buffer.get(i)));
        }
    }
```
![205](A4FD6A5A8FF24F1D930CB50D9EDA79BF)

## 知识点1:

java是由C或C++开发的一个普通程序，级别是linux系统下的一个进程

## 知识点2：
![202](EF3A4F1DCCA94AD3AD5CBCEB92280EF2)
txt是java的程序代码；txt文件、data、heap都是操作系统一个进程该有的内容；

java程序会向操作系统申请一个1G的空间，这个就是JVM中的heap;  (JVM的heap是java进程heap的一部分)

通过ByteBuffer.allocate() 【堆上分配==on Heap==】将字节数组分配到JVM的heap上

通过ByteBuffer.allocateDirect() 【堆外分配==off heap==】 申请的空间在java进程的Heap上;

堆上分配、堆外分配通过channel.read() 、 channel.write() 访问内核的pagecache； 需要经过 ==系统调用==

==堆外内存 比 堆内内存 性能稍高一些==

那么为什么还要有堆内内存？这就涉及到应用场景了

如果现在有一个Object对象是你自己的，是你能控制的，那么可以直接分配堆外内存

不是自己能控制的，目前没有很好的举例

堆内分配要先转化成堆外分配，才能到达内核的pagecache

FileChannel.map() 通过 mmap系统调用 得到 MappedByteBuffer（也是字节数组）；逻辑地址是映射到内核的pagecache

MappedByteBuffer调用put(),不会产生系统调用

==MappedByteBuffer相比堆内分配和堆外分配，性能要高太多了==

# 总结：

jvm的堆在java进程(linux系统的进程)的堆里

堆内：说的是在jvm的heap里的字节数组

堆外：说的是在jvm的heap之外，在java进程之内

mapped映射：是mmap调用的一个进程和内核共享的内存区域，且这个内存区域是pagecache到磁盘文件的映射

性能：on heap < off heap  < mapped(只限文件)

场景使用：

netty既可以on heap,又可以off heap;off heap更多一些

kafka log：mmap

# 结论：

==操作系统OS没有绝对的数据可靠性；==

==为什么设计pagecache？是为了 减少硬件IO的频繁调用；为了提速；优先使用内存；==

==即使想要可靠性，调成最慢的方式，但是单点故障问题会让你的性能调优，变得一毛钱收益都没有==

所以推荐：主从复制、主备HA

kafka/ES 有一个副本的概念【通过socket(也是IO)得到副本】（同步、异步）


![206](FC7FA995E82C48FBAA740D89FC1C8518)