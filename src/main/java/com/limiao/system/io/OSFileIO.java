package com.limiao.system.io;

import org.junit.Test;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class OSFileIO {

    static byte[] data = "123456789\n".getBytes();
        static String path =  "/root/testfileio/out.txt";


    public static void main(String[] args) throws Exception {


        switch ( args[0]) {
            case "0" :
                testBasicFileIO();
                break;
            case "1":
                testBufferedFileIO();
                break;
            case "2" :
                testRandomAccessFileWrite();
            case "3":
//                whatByteBuffer();
            default:

        }
    }


    //最基本的file写

    public static  void testBasicFileIO() throws Exception {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while(true){
//            Thread.sleep(10);
            out.write(data);

        }

    }

    //测试buffer文件IO
    //  jvm  8kB   syscall  write(8KBbyte[])

    public static void testBufferedFileIO() throws Exception {
        File file = new File(path);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        while(true){
            Thread.sleep(10);
            out.write(data);
        }
    }



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

        buffer.flip();   //翻转：为了读写交替；翻转以后可以得到内容的大小，limit指针到了文件内容的最后

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


}
