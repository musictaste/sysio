[TOC]

# C10K问题

文章：http://www.kegel.com/c10k.html

单机服务器连接1万个客户端的问题

单机能力强的人才符合软件工程学中的职位；需要掌握：JVM、设计模式、调优
    

四元组的概念：

    客户端，单机单进程，循环55000次，每次创建两个客户端，相同的端口号，不同的IP地址访问同一个Server，对一台服务器发起11万的连接
    突破端口号65535的限制

    本机IP地址
        网络和Internet-以太网-更改适配器选项-已启用
        1.计算机自带网卡的IP：192.168.1.4（课程中为192.168.150.1）
        2.通过VMware虚拟机创建的网络IP：192.168.10.1（课程中为：192.168.110.100）
            通过VMware-编辑-虚拟网络编辑器，可以看到类型是NAT模式，子网地址是：192.168.10.0，
            点击“NAT设置”，看到网关：192.168.10.2
            （课程中IP:192.168.150.0，网关：192.168.150.2）

## 实验

本实验模拟的是windows有一个java进程，玩命在windows调资源，开辟连接，==是一个客户端想建立更多的连接数==

而不是真正的有这么多客户端，每个客户端建立一个连接，模拟互联网的并发

### 代码
```
使用的服务器代码：SocketIOPropertites

C10Kclient
```


```java
package com.bjmashibing.system.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author: 马士兵教育
 * @create: 2020-05-17 05:34
 * BIO  多线程的方式
 */
public class SocketIOPropertites {


    //server socket listen property:
    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final boolean REUSE_ADDR = false;
    private static final int BACK_LOG = 2;
    //client socket listen property on server endpoint:
    private static final boolean CLI_KEEPALIVE = false;
    private static final boolean CLI_OOB = false;
    private static final int CLI_REC_BUF = 20;
    private static final boolean CLI_REUSE_ADDR = false;
    private static final int CLI_SEND_BUF = 20;
    private static final boolean CLI_LINGER = true;
    private static final int CLI_LINGER_N = 0;
    private static final int CLI_TIMEOUT = 0;
    private static final boolean CLI_NO_DELAY = false;
/*

    StandardSocketOptions.TCP_NODELAY
    StandardSocketOptions.SO_KEEPALIVE
    StandardSocketOptions.SO_LINGER
    StandardSocketOptions.SO_RCVBUF
    StandardSocketOptions.SO_SNDBUF
    StandardSocketOptions.SO_REUSEADDR

 */


    public static void main(String[] args) {

        ServerSocket server = null;
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(9090), BACK_LOG);
            server.setReceiveBufferSize(RECEIVE_BUFFER);
            server.setReuseAddress(REUSE_ADDR);
            server.setSoTimeout(SO_TIMEOUT);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server up use 9090!");
        try {
            while (true) {

                // System.in.read();  //分水岭：

                Socket client = server.accept();  //阻塞的，没有 -1  一直卡着不动  accept(4,
                System.out.println("client port: " + client.getPort());

                client.setKeepAlive(CLI_KEEPALIVE);
                client.setOOBInline(CLI_OOB);
                client.setReceiveBufferSize(CLI_REC_BUF);
                client.setReuseAddress(CLI_REUSE_ADDR);
                client.setSendBufferSize(CLI_SEND_BUF);
                client.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(CLI_NO_DELAY);

                //client.read   //阻塞   没有  -1 0
                new Thread(
                        () -> {
                            try {
                                InputStream in = client.getInputStream();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                char[] data = new char[1024];
                                while (true) {

                                    int num = reader.read(data);

                                    if (num > 0) {
                                        System.out.println("client read some data is :" + num + " val :" + new String(data, 0, num));
                                    } else if (num == 0) {
                                        System.out.println("client readed nothing!");
                                        continue;
                                    } else {
                                        System.out.println("client readed -1...");
                                        System.in.read();
                                        client.close();
                                        break;
                                    }
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                ).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}

```

```
package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * @author: 马士兵教育
 * @create: 2020-06-06 15:12
 */
public class C10Kclient {

    public static void main(String[] args) {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        InetSocketAddress serverAddr = new InetSocketAddress("192.168.150.11", 9090);

        //端口号的问题：65535
        //  windows
        for (int i = 10000; i < 65000; i++) {
            try {
                SocketChannel client1 = SocketChannel.open();

                SocketChannel client2 = SocketChannel.open();

                /*
                linux中你看到的连接就是：
                client...port: 10508
                client...port: 10508
                 */

                client1.bind(new InetSocketAddress("192.168.150.1", i));
                //  192.168.150.1：10000   192.168.150.11：9090
                client1.connect(serverAddr);
                clients.add(client1);

                client2.bind(new InetSocketAddress("192.168.110.100", i));
                //  192.168.110.100：10000  192.168.150.11：9090
                client2.connect(serverAddr);
                clients.add(client2);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        System.out.println("clients "+ clients.size());

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

```



### 实验过程

    1.抓包观察：
    tcpdump -nn -i eth0 port 9090
    
    2.linux服务器：
    javac SocketIOPropertites.java && java SocketIOPropertites
    
    3.启动客户端程序：C10Kclient -> run
    
    结果：
    通过抓包发现192.168.150.1：10000和192.168.110.100：10000
    同一个端口号，不同ip确实尝试连接Server，但是server只打印了一次端口号
    
    这是一个小bug：
    
    192.168.150.1：10000（本机网卡）三次握手成功了
    
    192.168.110.100：10000（虚拟网卡）进行了前两次握手，第三次没有进行；反复进行前两次握手

    但是在服务端 ping 客户端的192.168.110.100（虚拟网卡）是通的
    

![402](FE4AE42BEE2648BDB1EB0B08FEE60882)
    
### 分析（整理的有问题，看下面总结的内容）
![403](CB98571CA199416A86ED988EF81F5A70)

    服务端查看路由表：route -n   图片403
    
    面向这台服务器的linux操作系统，有一块网卡，网卡的地址是192.168.150.11，网络号是192.168.150.0，意思是通过linux是可以直接访问192.168.150.0这个网络的
    
    通过这个路由表，发现这个linux跟192.168.110.100不是直连关系，因为没有出现跟192.168.110.100的条目，
    而是走了默认的网关：192.168.150.2，也就是说192.168.110.100的数据包可以进到这台服务器
    
    返回的时候呢，以192.168.110.100为目标地址也能返回，返回的时候只能走默认网关192.168.150.2这个条目，那么将会以192.168.150.2为下一跳
    
    而192.168.150.2是VMware虚拟网络中Vmnet8对应的网关，这个网关是什么呢？是windows上的一个进程，这个进程完成了一个NAT地址转换（这个知识点在高并发的redis中已经讲过了）
    
    其实到这块应该明白了，windows客户端数据包进入linux主机，只不过回去的包走了192.168.150.2,也就是说把原地址192.168.150.11换成了192.168.150.2
    
**具体过程是**：

    第一次握手：原地址192.168.110.100   目标地址：192.168.150.11
    第二次：原地址192.168.150.2  目标地址：192.168.110.100
        本来原地址应该为192.168.150.11,但是因为经过了NAT地址转换，原地址就由192.168.150.11变成了192.168.150.2；
        而windows并不认192.168.150.2，所以就把第二次握手的数据包给丢弃了，所以这个连接就没有建立上

### 如何解决
    linux服务器增加一个路由器条目就可以,让 192.168.110.100走window网卡的地址 （192.168.150.1）
    
    route add -host 192.168.110.100 gw 192.168.150.1

    这时候重新运行客户端和服务端，发现正常了，Server打印了两次同一个端口号，说明连接都建立了
    
    另外通过命令：netstat -natp 查看发现四元组的连接都建立了
    这时也突破了端口号65535的限制
    
    这时呢发现连接也不是很快
    
### 总结：

![404](3920B21BD6874E8BA5FA731C8ED3D61D)

    拓扑结构
    linux服务器IP地址：192.168.150.11
    
    windows 两块网卡：192.168.150.1、192.168.110.100（物理网络）
    
    VMware:可以模拟虚拟机，也可以模拟虚拟网络（网络号：192.168.150.0），网关Nat(192.168.150.2)

    Vmware Nat Server服务（进程）持有192.168.150.2这个地址
    
    windows程序可以通过网卡192.168.150.1将数据包传递到Linux服务器
    
    
    四元组：
    绑定192.168.150.1作为源 + 随机端口号10000 访问 192.168.150.11 + 9090
    
    192.168.110.100 + 随机端口号10000 访问 192.168.150.11 + 9090
    
    
    第一次握手：
    192.168.150.1 + 10000 或192.168.110.100 + 10000 的数据包会通过window程序，通过虚拟网络，发到linux服务器
    
    第二次握手：
    基于192.168.150.1这个地址绑定的数据包可以完成三次握手
    
    192.168.110.100作为目标地址的话，会触发windows路由条目，走到下一跳（192.168.150.2），IP 192.168.110.100经过NAT地址转换(window系统中发生)，转换成IP 192.168.150.2，并且端口号随机分配为19090
    这样的数据包到达windows以后，windows发现程序中没有 “目的：192.168.110.100 + 10000 192.168.150.2 19090”的socket，所以这个数据包就被丢弃了
    
### 为什么连接会慢
![405](B3FA3FE840774E949FC9DE16ACCCAA8B)

一个客户端经过三次握手，通过kernel连上Server main（服务端的主线程）
服务端的主线程的accept以后，抛出一个Thread（线程）用于处理数据的读写

accept是一个系统调用；accept会触发clone的系统调用得到Thread

==慢的原因就是：克隆一个线程的过程消耗了时间；并且线程多了话，线程切换也需要时间==

==优化：先准备好一个线程池，来一个连接就分配一个线程，不需要accept再clone创建出一个新的线程；这就是池化思想==

## BIO的弊端：阻塞，BLOCKING

两个阻塞：accept是阻塞的， 读写的线程的读写操作也是阻塞的

==阻塞是Kernel造成的==

==因为阻塞，才抛线程==

**因为阻塞，所以需要新建一个线程用于读写数据，才能满足多个客户端可以连接服务器**

# NIO

NIO： jdk new IO;   OS  NONBlocking

在java中，SocketChannel类似文件描述符，是一个抽象层，双向的输入输出，整合了输入流、输出流

设置为NIO：

    ServerSocketChannel.configureBlocking(false)
    SocketChannel.configureBlocking(false);
    

```java
package com.bjmashibing.system.io;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class SocketNIO {

    //  what   why  how
    public static void main(String[] args) throws Exception {

        LinkedList<SocketChannel> clients = new LinkedList<>();



        ServerSocketChannel ss = ServerSocketChannel.open();  //服务端开启监听：接受客户端
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); //重点  OS  NONBLOCKING!!!  //只让接受客户端  不阻塞

//        ss.setOption(StandardSocketOptions.TCP_NODELAY, false);
//        StandardSocketOptions.TCP_NODELAY
//        StandardSocketOptions.SO_KEEPALIVE
//        StandardSocketOptions.SO_LINGER
//        StandardSocketOptions.SO_RCVBUF
//        StandardSocketOptions.SO_SNDBUF
//        StandardSocketOptions.SO_REUSEADDR




        while (true) {
            //接受客户端的连接
            Thread.sleep(1000);
            SocketChannel client = ss.accept(); //不会阻塞？  -1 NULL
            //accept  调用内核了：1，没有客户端连接进来，返回值？在BIO 的时候一直卡着，但是在NIO ，不卡着，返回-1，NULL
            //如果来客户端的连接，accept 返回的是这个客户端的fd  5，client  object
            //NONBLOCKING 就是代码能往下走了，只不过有不同的情况

            if (client == null) {
                System.out.println("null.....");
            } else {
                client.configureBlocking(false); 
                //重点  socket（服务端的listen socket<连接请求三次握手后，往我这里扔，我去通过accept 得到  连接的socket>，连接socket<连接后的数据读写使用的> ）
                int port = client.socket().getPort();
                System.out.println("client...port: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);  //可以在堆里   堆外

            //遍历已经链接进来的客户端能不能读写数据
            for (SocketChannel c : clients) {   //串行化！！！！  多线程！！
                int num = c.read(buffer);  // >0  -1  0   //不会阻塞
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);

                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }


            }
        }
    }

}


```

## 实验

    **javac SocketNIO.java  &&  strace -ff -o out java SocketNIO
        编译、追踪java线程   启动java程序
        运行结果：每隔1s打印“null........”,说明接收客户端连接没有被阻塞
        
    
    cd testsocket  到当前程序所在的目录下
    
    ll  查看out文件
    
    ** vi out.2804   查看out文件中文件最大的那个文件
        可以看到accept(4,0x7fdf2001c300,[16])  = -1 EAGAIN[Resource temporarily unavailable]  
        说明accept执行了，返回了-1
        
    set nu  设置行号
    

实验2；

    nc localhost 9090 
        nc:net client   有一台客户端连接这台服务器
    
    nc localhost 9090 
        再连接一个客户端
        
    两个连接随意发送一些内容
    
    查看Server的打印信息，可以看到发送的内容接受到了
        说明客户端的读写是非阻塞的
    
## 图解

![406](93F0A058CF584523AF41017E0776B98C)

## IO模型性能压测

### 实验1：

    启动服务端
    javac SocketNIO.java  && java SocketNIO
    
    启动客户端，运行C10Kclient
    
运行结果：可以看到每隔1s，打印一句客户端连接的信息
        
    
### 实验2：SocketNIO去掉Thread.sleep(1000);

    启动服务端
    javac SocketNIO.java  && java SocketNIO
    
    启动客户端，运行C10Kclient

#### 运行结果
在连接端口到达12000的时候，连接速度变慢，最后在端口号=12044时会报错：Exception in thread "main" j==ava.io.IOException:Too many open files，超出文件描述符的数量==
        
#### 原因分析：
因为在while(true)中，要对LinkedList<SocketChannel> clients进行遍历，遍历的时候，要调用read();==read方法要进行系统调用，涉及到内核态和用户态的切换==
    
#### 解决Too many open file

命令：ulimit -a  可以看到open files= 1024  （open files：一个进程可以打开多少个文件描述符）

==为什么Server可以接收端口号到1024？因为端口号是成对的，也就是实际是4088个连接==  

==原因是使用root用户启动了程序，如果切换成普通用户open files确实只能是1024==

这个理论是对的，只不过要看用户，而且公司里生产环境肯定是非root用户启动程序

如何解决超出文件描述符的数量？

==ulimit -SHn 500000  设置open files可以接收50万==

ulimit -n 查看设置的参数，open files=50万

### 实验3:
    
    已经设置了open files =50万（ulimit -SHn 500000）
    
    启动服务端
    javac SocketNIO.java  && java SocketNIO
    
    启动客户端，运行C10Kclient
    
    jps   可以看到SocketNIO的进程号=3004
    
    lsof -p 3004  查看文件描述符
    
    clear
    
    netstat -natp  查看连接情况

运行结果：可以建立11万的连接

本实验模拟的是windows有一个java进程，玩命在windows调资源，开辟连接，==是一个客户端想建立更多的连接数==

而不是真正的有这么多客户端，每个客户端建立一个连接，模拟互联网的并发


本实验是线性的，不是并发的

![407](BEA79EEF5AEA4232BD9D719D6438294E)

### 实验4：多路复用器

```
package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class SocketMultiplexingSingleThreadv1 {

    //马老师的坦克 一 二期
    private ServerSocketChannel server = null;
    private Selector selector = null;   //linux 多路复用器（select poll    epoll kqueue） nginx  event{}
    int port = 9090;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));


            //如果在epoll模型下，open--》  epoll_create -> fd3
            selector = Selector.open();  //  select  poll  *epoll  优先选择：epoll  但是可以 -D修正

            //server 约等于 listen状态的 fd4
            /*
            register
            如果：
            select，poll：jvm里开辟一个数组 fd4 放进去
            epoll：  epoll_ctl(fd3,ADD,fd4,EPOLLIN
             */
            server.register(selector, SelectionKey.OP_ACCEPT);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。。。");
        try {
            while (true) {  //死循环

                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"   size");


                //1,调用多路复用器(select,poll  or  epoll  (epoll_wait))
                /*
                select()是啥意思：
                1，select，poll  其实  内核的select（fd4）  poll(fd4)
                2，epoll：  其实 内核的 epoll_wait()
                *, 参数可以带时间：没有时间，0  ：  阻塞，有时间设置一个超时
                selector.wakeup()  结果返回0

                懒加载：
                其实再触碰到selector.select()调用的时候触发了epoll_ctl的调用

                 */
                while (selector.select() > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();  //返回的有状态的fd集合
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    //so，管你啥多路复用器，你呀只能给我状态，我还得一个一个的去处理他们的R/W。同步好辛苦！！！！！！！！
                    //  NIO  自己对着每一个fd调用系统调用，浪费资源，那么你看，这里是不是调用了一次select方法，知道具体的那些可以R/W了？
                    //幕兰，是不是很省力？
                    //我前边可以强调过，socket：  listen   通信 R/W
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove(); //set  不移除会重复循环处理
                        if (key.isAcceptable()) {
                            //看代码的时候，这里是重点，如果要去接受一个新的连接
                            //语义上，accept接受连接且返回新连接的FD对吧？
                            //那新的FD怎么办？
                            //select，poll，因为他们内核没有空间，那么在jvm中保存和前边的fd4那个listen的一起
                            //epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);  //连read 还有 write都处理了
                            //在当前线程，这个方法可能会阻塞  ，如果阻塞了十年，其他的IO早就没电了。。。
                            //所以，为什么提出了 IO THREADS
                            //redis  是不是用了epoll，redis是不是有个io threads的概念 ，redis是不是单线程的
                            //tomcat 8,9  异步的处理方式  IO  和   处理上  解耦
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept(); //来啦，目的是调用accept接受客户端  fd7
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);  //前边讲过了

            // 0.0  我类个去
            //你看，调用了register
            /*
            select，poll：jvm里开辟一个数组 fd7 放进去
            epoll：  epoll_ctl(fd3,ADD,fd7,EPOLLIN
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
        service.start();
    }
}

```


    启动服务端
    javac SocketMultiplexingSingleThreadv1.java 
    java -Xmx4G  SocketMultiplexingSingleThreadv1
    
    启动客户端，运行C10Kclient
    
运行结果：客户端建立的速度明显快很多

并且建立了109954个连接，其中因为windows占用了一些端口，提示Address already in use:bind

### 答疑

#### 1.使用ulimit设置，与修改内核参数/proc/sys/fs/file-max有什么区别？

我本机的虚拟机的内存是4G，通过命令cat /proc/sys/fs/file-max 得到的结果是385915
    
linux内核根据物理内存，在内核级别估算出的；1G内存一般对应10万个文件描述符
    
==/proc/sys/fs/file-max是OS kenerl能够开辟的文件描述符的数量==
    
==ulimit是控制不同用户它的进程能够申请的文件描述符的上限==

补充：在 /etc/security/limits.conf有一个属性nproc  (max number of processes 打开的最大进程数)，如果java线程创建不出来了，有可能是这个设置影响的