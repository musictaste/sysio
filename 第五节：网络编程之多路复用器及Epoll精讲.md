[TOC]

# NIO的优势与问题

![501](626D15B1FA0F4BFA844C9E80D3BB6B75)

==优势：通过1个或几个线程，来解决N个IO==

虽然一个线程可以解决客户端的连接、以及连接建立以后的数据读写，而不阻塞

但是accept 以及recv方法还是会触发系统调用的，

这时候如果==发生C10K问题==，即IO的连接数量已经达到1万个，单线程每循环一次，==成本是O(n)的复杂度==，即1万次recv系统调用，涉及到用户态内核态的切换，==很多调用时无意义的，是浪费的==，


这就引出多路复用器的使用

# 多路复用器

![502](52E951F53CCE453F9A1A4AF9749F88F2)

如果每一个IO是一条路，这条路上有没有数据（data）,这些路先扎在Kernel上

==NIO==：应用程序需要==全量遍历==，涉及系统调用，需要==用户态内核态切换才能实现==

==多路复用==：数据到了，谁先知道呢？kernel先知道，NIO是打一个电话询问一个IO；而多路复用是打一个电话询问所有IO，数据到没到；也就是==多条路(IO)通过一个系统调用，获得其中的IO**状态**，然后**由程序自己对有状态的IO进行读写**==

==注意：只要程序自己读写，那么你的**IO模型**就是同步的==

**区别于：读取多个IO的数据后，在多个线程里处理，那是处理的同步异步问题**

---

只关注IO，不关注从IO读写完之后的事情

同步：app自己读写

异步：kernel完成读写后写到程序的Buffer中，程序没有访问IO，程序从Buffer取数据
    只有windows的iocp是纯异步的

阻塞：BLOCKING，调用以后，肯定能拿到结果

非阻塞：NONBLOCKING；调用以后，还需要自己再访问一次去取结果

linux以及成熟的框架netty中使用是同步阻塞（BIO）、同步非阻塞（）

==同步阻塞：同步(程序自己读取，调用了方法一直等待有效返回结果)==

==同步非阻塞：程序自己读取，调用方法一瞬间，给出是否读到（自己要解决下一次啥时候再去读）==

异步：尽量不要去讨论，因为现在只讨论IO模型下，linux目前没有通用的内核级的异步的处理方案

==只有异步非阻塞，异步阻塞是没有意义上的==

---

## 多路复用器的实现：

SELECT（POSIX标准）<所有操作系统都会提供的一种多路复用器>

POLL

EPOLL

==上面的三种实现==，如果面试问到，记住是==同步模型下非阻塞的一种使用==

### SELECT:synchronous I/O multiplexing

![503](49031FB0A7794EAA9325E014129A67DA)

==SELECT是相对容易实现的，不同的操作系统内核实现方式约束力很少，是解耦性很强的一个系统调用，是所有操作系统都会提供的，都遵循POSIX组织的规范==

==EPOLL需要内核具备一定的实现；linux是EPOLL,Unix是kqueue，它们都是基于IO事件的一种通知行为，都是同步的==


man 2 select可以看到synchronous I/O multiplexing 同步IO多路复用器

受到==FD_SETSIZE==<可以交互的、可以询问的路(IO)的数量>的限制,是==1024==，就是因为有限制，所以现在select几乎不用

==POLL里面没有1024的限制==

应用程序：while循环中调用select(fds) [复杂度：O(1)]后知道所有持有的连接中哪些是可读写的，然后对可读写的连接进行recv() [时间复杂度：O(m),不是O(n)]

所以时间复杂度：O(1)+O(m)


应用程序的select()调用的是Kernel的select(fds) <fds:所有持有的文件描述符>


### 小结


==**<含金量很高的一句话>:其实，无论NIO、SELECT、POLL都是要遍历所有的IO，询问IO的状态**==

==只不过，NIO这个遍历的过程，都需要用户态内核态的切换==

==在SELECT、POLL(多路复用器-1阶段)的情况下：这个遍历的过程，触发了一次系统调用，即一次用户态内核态的切换，过程中把fds传递给内核，内核重新根据用户这次调用传过来的fds，遍历并修改状态==

**现在就能看出，多路复用器现在就比NIO执行快**

> 同时也能看出SELECT、POLL的弊端：
> 
> 1.每次都要重新传递fds（epoll_create内核开辟空间）
> 
> 2.每次内核被调用以后，针对这次调用触发一个遍历fds全量的复杂度

### 强行插入知识点

![504](8BAA6B746384404AA75A6C407C16C5BE)

Kernel是一个程序，在内存中；内存中还有app应用程序

根据计算机组成原理的知识，CPU、内存、网卡、键盘、中断（软中断、硬中断）

==具体看之前计算机组成原理的知识==

==软中断-中断向量==表：255个整数 1到255；  ==int 80==：callback函数

==硬中断-时钟中断==：CPU中有==晶振==，过来的电压，出去的电压‘哒哒哒’，1秒中会规则的震荡，每哒一次产生时钟中断，会去找中断向量表相应的回调函数，可以做一些计数器或做一些系统调用；

时钟中断可以促使我们即使只有1核CPU，也可以在内核，程序、程序之间进行交替

时钟中断的应用场景：只有1核CPU，启动了很多应用程序，应用程序的交替执行就使用了时钟中断    切片   

还有就是==IO中断==：

网卡、键盘、鼠标都是输入输出设备；

晃动鼠标的时候，会给计算机发送中断，cpu要响应这些中断，如果CPU来不及响应这个中断，鼠标会卡顿

网卡：有数据包从客户端进到网卡，这时候有几种可能性，

第一种：一个数据包低速进入，这时网卡发生了IO中断，这个中断一定会打算CPU，cpu就不去处理程序，这个程序没有进入挂起状态，未来cpu处理完中断程序继续执行；另外打断CPU，cpu把读到的数据复制到内存中；这时最早的情况

网卡中有一个==buffer==，这个buffer可以被cpu读取；另外内存中可以再开辟一个空间，叫==DMA==，内核中有网卡驱动，网卡驱动被激活会开启一个DMA

网卡触发IO中断有三个级别：

1.package：来个一个包就触发

2.buffer：攒一个buffer，放到DMA中，使用最常见的

3.轮询：数据来的太快，就会关闭中断，cpu停止靠中断来同步数据；直接通过轮训读取DMA的数据

这是由网卡的工作频率，内核动态调整；

切记：这只是网卡和Kernel之间的交互，没有应用程序参与

==最终：有中断就会有回调(callback),就可以抽象成：有event事件就会回调处理事件==

==中断 ->  回调callback==

==event事件 -> 回到处理事件==

callback回调函数有大小、复杂度

==在EPOLL之前的callback：只是完成了将网卡发来的数据，走内核网络协议栈（包含2/3/4层网络协议<链路层、网络层、传输控制层>），最终关联到FD的buffer，所以你某一个时间从APP询问内核某一个或某些FD是否可读可写，会有状态返回==

如果内核在callback处理中再加入()

如果想要看man，需要装man(帮助命令)   man-pages(帮助手册)

==yum install man man-pages==

查询的时候使用命令：man 2 select

### 答疑

1h15min 到 1h30min

涉及的知识点有zk、mmap、redis、epoll的细节


### Epoll

先看帮助文档  man epoll   一共有7类

先看2类 系统调用：epoll_create(2)、epoll_ctl(2)、epoll_wait（2）

这三个系统调用就是在完成两个弊端的修正，以及内核里底层实现了基于callback 以及文件描述符迁移的过程，


---

应用程序和Server，通过Socket、bind、listen三个系统调用到内核去，得到一个监听的文件描述符fd4 

==重点来了，为了解决select、poll的第一个弊端：每次都重新、重复传递fds，有了epoll_create==

#### epoll_create

查看帮助文档：man 2 epoll_create

==epoll_create如果成功返回一个文件描述符（epfd：epoll+fd）fd6,在内核开辟一个空间，这个空间就是fd6, 这个空间存放红黑树==

#### epoll_ctl

查看帮助文档：man 2 epoll_ctl

需要传递参数epfd,这个epfd就是epoll_create开辟的空间fd6

第二个参数op：执行的操作（EPOLL_CTL_ADD、EPOLL_CTL_MOD、EPOLL_CTL_DEL）

第三个参数fd:具体的文件描述符，可以是listen的fd，也可以是socket

第四个参数epoll_event:关注的事件，比如读、写等等

也就是epoll_ctl(fd6,ADD,fd4,accept) 相当于图中的epoll_ctl(fd6,ADD,fd4)  ->  fd4 ->accept

#### epoll_wait

不能光有epoll_create和epoll_ctl,还需要有epoll_wait

查看帮助文档：man 2 epoll_wait

调了epoll_wait以后等待有状态事件的返回 , 也就是在等一个链表

epoll_wait调到内核了，内核中会有一个链表，链表中的东西来自于红黑树向它的状态的迁移、拷贝

#### 中断延伸

Epoll在中断处理时 延伸了

客户端的数据包 到达 网卡，网卡根据中断里面的callback，将发送来的数据包放到FD4的缓冲区buffer

==延伸 要处理什么事情呢？拿着这个fd4去红黑树中去找，如果找到，将fd4拷贝到链表中了==

问题：什么时候将FD从红黑树拷贝到链表中呢？就是在  ==中断回调==的==延伸==里  做的

==就是因为中断回调的延伸处理，那么未来程序在调用epoll_wait的时候，就不需要再将曾经持有的所有文件描述符fds遍历一遍==

==EPOLL执行快的原因就是：规避了遍历==

![505](3FDF9B84CCD0441BBC45CE0E9A144711)

### EPOLL V.S  SELECT

SELECT:站在程序的角度，什么时候调用select，内核什么时候遍历fds修正状态

EPOLL:内核中，程序在红黑树中放过一些FD，那么伴随内核基于中断处理完fd的buffer，状态呀；之后，继续把有状态的fd，copy到链表中；所以，程序只要调用wait，就能及时取走有状态的FD的结果集

**epoll_wait得到的结果集，程序还需要进行处理（accept、recv），所以Epoll_wait还是同步模型**

epoll_wait拿到有状态的FDs,还需要app做read、write操作

==epoll_wait 约等于select/Poll，只不过epoll_wait不传递参数fds，也不触发内核遍历==

==epoll相比select多了epoll_create、epoll_ctl两个调用、就是为了内核开辟空间的维护，以及这个开辟空间基于系统中断延伸向链表迁移的过程==

==EPOLL和SELECT通用的是：即便你不调用我内核，我内核也会随着中断完成所有FD的状态设置==


![506](7AC068E6472F4358A7F133B36A2B9006)

### selector

上面那些都是linux、操作系统的知识，回归到java领域，select、poll以及Epoll在java领域被抽象为Selector

上面说的都是读事件

写事件:什么时候想要写了，什么时候注册写事件；因为只要你注册写事件，写事件队列不满，那么写事件可以玩命的触发，因为写事件不需要阻塞

写事件什么时候会阻塞？当写出的性能，网卡支撑不了（网卡上有太多要写出去的东西），写出队列阻塞了，那么注册写事件的时候会阻塞，并且等写出队列不阻塞了，会调用这个注册的写事件

读事件不一样，因为读事件受制于人，什么时候别人给你发，你才能读。

==所有我们上来就注册读事件，而写事件是什么时候写就什么时候注册写事件，调用完之后还要把自己移除==

# 映射到java代码

## SocketMultiplexingSingleThreadv1_1


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
