[TOC]

==Netty是由java的selector开发的==

# 之前知识的复习

## 实验1：什么情况下服务端会出现CLOSE_WAIT

    代码中readHandler方法中的 client.close();删除，即当客户端关闭连接时(发送-1)不做任何处理

    javac SocketMultiplexingSingleThreadv1.java && strace -ff -o poll java -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider SocketMultiplexingSingleThreadv1
        追踪java程序， 使用poll
        
    netstap -natp
    
    nc localhost 9090  新开一个窗口，客户端建立连接，然后随意发送数据到服务端
        通过netstap -natp可以看到有一个服务端的进程（端口号9090）
        一个服务端到客户单的连接（端口号37989）
        一个端口号为37989的nc进程

    客户端ctrl+C 停掉连接    
        通过netstap -natp可以看到nc进程（端口号37989）状态是FIN_WAIT2
        服务端到客户端的连接 状态是CLOSE_WAIT
    
![601](12C1B1EC97A64C859BB3966F4AFCCB8E)

那么什么情况下会出现CLOSE_WAIT？

    之前讲过三次握手，四次分手（在redis课程）
    客户端给服务端发送FIN(断开连接的包)，服务端先进入close_wait的状态
    服务端给客户端发送一个ACK（关于FIN的）
    如果服务端没有发送FIN（断开连接的包），客户端会进入FIN_WAIT2的状态
    
    只做了2次分手
    
![603](4E134966104E4B65BF3C43054FD07275)

    
## 实验2，什么情况下会出现TIME_WAIT,会有什么影响？

    代码中readHandler方法中的 添加client.close();，即当客户端关闭连接时(发送-1)将连接关闭
    
    javac SocketMultiplexingSingleThreadv1.java && strace -ff -o epoll java SocketMultiplexingSingleThreadv1
        追踪java程序， 使用epoll
        使用epoll，可以不加-D的参数，因为默认会采用最好的方式
        
    nc localhost 9090 
        新开一个窗口，客户端建立连接，然后随意发送数据到服务端
    
    netstap -natp查看连接情况
        看到有一个服务端的连接（端口号9090）
        一个服务端到客户单的连接（端口号37990）
        一个端口号为37989的nc进程
    
    客户端Ctrl+C  关闭连接
    
    netstap -natp查看连接情况
        看到nc进程（端口号37989）状态是TIME_WAIT
        服务端到客户端的连接 没有了
    

![602](CE0E5C4F919344D7BA92801E1F3F603B)

经过四次分手以后，服务端进入close状态（服务端到客户端的连接没有了），而NC进程（客户端到服务端的链接）状态是TIME_WAIT; 

一段时间以后（==2MSL，2倍的报文时间，30秒*2=60秒==），NC进程（客户端到服务端的连接）也会消失

==注意，面试时要说清楚==

==这个TIME_WAIT状态取决于客户端还是服务端先发起关闭请求，谁先发起关闭请求，谁就是TIME_WAIT状态==

网络通信是没有物理连接的，服务端和客户端都是资源的开辟

为什么TIME_WAIT状态要多保留一会呢？因为有可能最后的ACK没有到达对方，自己多保留一会资源

==另外这个TIME_WAIT消耗不消耗资源？ 消耗资源==
    通过lsof -op 服务端进程号，没有文件描述符
    通过netstap -natp 可以看到nc连接状态为TIME_WAIT
    
==也就是虽然程序里已经结束了，但是内核中还会再保留一会；所以TIME_WAIT是消耗资源的，消耗的是SOCKET四元组的规则==

> 在TIME_WAIT没有结束前，内核中的socket的四元组被占用，相同的对端不能使用这个资源建立新的连接
> 
> 浪费的是名额！（客户端到服务器）
> 
> 站着茅坑不拉屎
> 
> 这个不是DDOS攻击；是当前客户端不能再连接服务器，其他客户端还是可以连接服务器的

![603](7F35B1A6A53B4525BF153FEC00872112)


## 总结：

1.什么情况下会造成TIME_WAIT？就是当服务端或客户端任意一方发起断开连接时，在连接关闭后，该端会进入TIME_WAIT状态，并保留2MSL(2倍报文的时间)

2.TIME_WAIT造成的影响：消耗资源，内核中的四元组被占用，相同的对端不能再使用这个资源建立新的链接

3.解决方案: net.ipv4.tcp_reuse=1 （代表资源可以重复使用）  加上 快速关闭连接的参数（后面会降）

    通过sysctl -a | grep reuse进行查看
    
# 同一套代码： java-NIO-selector：poll和epoll的不同底层实现

![604](CA6C11ADE87B4E70BC9DACE67679397B)


## OS:POLL

    OS:POLL  jdk  native 用户空间 保存了fd

    socket(PF_INET, SOCK_STREAM, IPPROTO_IP) = 4
    fcntl(4, F_SETFL, O_RDWR|O_NONBLOCK)    = 0 //server.configureBlocking(false);设置非阻塞
    bind(4, {sa_family=AF_INET, sin_port=htons(9090)  绑定端口号
    listen(4, 50)  监听连接
    
     poll([{fd=5, events=POLLIN}, {fd=4, events=POLLIN}], 2, -1) = 1 ([{fd=4, revents=POLLIN}])      //  while (selector.select() > 0) 
        可以看到是数组形式
        {fd=5, events=POLLIN}不用管，是一个pipeline管道
        ([{fd=4, revents=POLLIN}])代表一个客户端想建立连接了
        
         
    accept(4,    = 7  //得到新的客户端
    
    fcntl(7, F_SETFL, O_RDWR|O_NONBLOCK) //新的客户端连接设置非阻塞
    
    poll([{fd=5, events=POLLIN}, {fd=4, events=POLLIN}, {fd=7, events=POLLIN}], 3, -1)  = 1 ([{fd=7,revents=POLLIN}])
         //再次执行  while (selector.select() > 0) 
        返回 1（一个fd有事件）
        返回-1（非阻塞下，没有事件）
        
        poll( fds, fds的数量，timeyout) -1代表永久阻塞
       
    
## OS:EPOLL

    socket(PF_INET, SOCK_STREAM, IPPROTO_IP) = 4
    fcntl(4, F_SETFL, O_RDWR|O_NONBLOCK)    = 0
    bind(4, {sa_family=AF_INET, sin_port=htons(9090)
    listen(4, 50)  //这些都是一样的
    
    epoll_create(256)                       = 7 (得到epfd)
    epoll_ctl(7, EPOLL_CTL_ADD, 4,
    epoll_wait(7, {{EPOLLIN, {u32=4, u64=2216749036554158084}}}, 4096, -1) = 1   //一个客户端想建立连接
    //  while (selector.select() > 0) {
    
    accept(4  =8 //client的fd
    
    fcntl(8, F_SETFL, O_RDWR|O_NONBLOCK)     //非阻塞
    epoll_ctl(7, EPOLL_CTL_ADD, 8, {EPOLLIN,   
    
    epoll_wait(7,

## 闲篇

学习了Netty，那么对于IO框架就非常熟了，那么再学中间件（dubbo、zk、ES、MQ）就容易多了

搞清楚了Netty，就可以去tomcat官网看性能描述的一些关键词就能想到对应的知识了，而不是靠猜来理解

Kafka是自己封装了epoll

现在IT的知识量呈现一种形式：每3年进行一次增量递增

## 碎片知识

### 实验3：server.register会立马触发epoll_ctl吗？ 不会

    1.代码修改
    server.register(selector, SelectionKey.OP_ACCEPT);
    System.out.println("register listern over ~~~");在register后面增加打印语句
    
    2.删除日志
    rm -fr *poll*
    
    javac SocketMultiplexingSingleThreadv1.java && strace -ff -o epoll java SocketMultiplexingSingleThreadv1
    
![605](19F3841F98DC4F89ADD2824776A45A67)

server.register不会立马触发epoll_ctl

原因：懒加载，其实在触碰到selector.select()调用的时候才会触发了epoll_ctl的调用

但是呢，在epoll_wait之前呢，肯定会触发epoll_ctl

### 多路复用器-写事件


```
代码：SocketMultiplexingSingleThreadv2  -  53行：key.isWritable()
```


写事件<--  send-queue 只要是空的，就一定会给你返回可以写的事件，有事件就会回调我们的写方法

通过netstap -natp 可以看到Send-Q (也就是Send-queue)

你真的要明白：你想什么时候写？不是依赖send-queue是不是有空间（多路复用器能不能写是参考send-queue有没有空间）
1，你准备好要写什么了，这是第一步
2，第二步你才关心send-queue是否有空间
3，so，读 read 一开始就要注册，但是write依赖以上关系(第一、二步)，什么时候用什么时候注册
4，如果一开始就注册了write的事件，进入死循环，一直调起！！！

#### 实验1：只有一个线程


```
SocketMultiplexingSingleThreadv1_1
```
    
    1.代码
    key.isReadable()  没有key.cancel();
        只处理了read,并注册 关心这个key的write事件
        关心OP_WRITE 其实就是关心send-queue是不是有空间
    
    key.isWritable() 没有key.cancel();
        拿到buffer，并写出去
    
    2.运行    
    服务端可以在windows上运行
    
    nc 192.168.150.1 9090   linux连接
    
结果：一个线程中是没有问题的，客户端可以发出去内容，并接收发出去的内容
    
多线程情况下，如果其中一个线程执行耗时（例如读写耗时，或者业务逻辑复杂），会阻塞、延长执行周期，并让后面的连接等很久
  
#### 实验2：主线程使用多路复用器，其他线程处理业务逻辑

```
SocketMultiplexingSingleThreadv2
```

出现一个想法：把耗时的方法（读、写）放到另一个线程中，当前线程只做连接建立，以及处理读写事件

这时候，读写方法的阻塞   会影响 selector的执行吗？
    
    1.代码
    key.isReadable()  没有key.cancel();
    key.isWritable() 没有key.cancel();

    2.
    windows启动服务端
    
    nc 192.168.150.1 9090
    
    客户端发送abc
    
    3.实验结果：
        read handler.....打印多次
        write handler被疯狂的调用

    4.分析：
        1).首次执行selector.select(50)，selector连接建立，执行acceptHandler方法
        2).用户发来数据，再次执行selector.select(50)，readHandler还是阻塞的吗？
        
        不阻塞
        因为有另一个线程去具体处理了，所以很快就有返回值了；
        但是现在呢具体的读方法中的线程还没有处理完，有一个时延，reading空间还没有读完，
        
        这时候selector.select()又执行了，
        所以会重复触发key.isReadable()
        
        所以即便以抛出了线程去读取，但是在时差里，这个key的read事件会被重复触发
    
    5.解决: readHandler(key)之前key.cancel();
        } else if (key.isReadable()) {
            key.cancel();  //现在多路复用器里把key  cancel了
            System.out.println("in.....");
            key.interestOps(key.interestOps() | ~SelectionKey.OP_READ);
        
            readHandler(key);//还是阻塞的嘛？不阻塞了； 即便以抛出了线程去读取，但是在时差里，这个key的read事件会被重复触发
        }    
    
    
    6.为什么write handler...被疯狂调起
        因为readHandler方法中注册了SelectionKey.OP_WRITE  写事件
        
        如果一开始就注册了write的事件，并且send-queue是空的；就会进入死循环，一直调起！！！
        
    7.解决：writeHandler(key)之前key.cancel();

![606](626D5D29BAAD41F391F0B87960587715)

通过图再来理解一下

主线程中使用了多路复用器selector，现在有多个连接FD1、FD2

selector.select()返回：有状态的fd集合

连接FD1 read的时候，会调用readHandler();在readHandler方法中会再起一个线程处理数据，并往selector中注册一个OP_WRITE写事件<client.register(key.selector(),SelectionKey.OP_WRITE,buffer);>

这样就造成了selector可以调用writeHandler()<writeHandler中也会再起一个线程>

只要send-queue有空间，那么writeHandler()就会被重复调起

在主线程里不能阻塞执行，不能是线性的，所以事件会被重复触发，解决方案是key.cancel()

key.cancel()会触发系统调用epoll_ctl(n,del,m) ==不是删除内核中的文件描述符，而是剔除Epoll开辟出的空间--红黑树中的FD==

register会触发系统调用 epoll_ctl(n,add,m)

   
#### 如何才能使用多线程，还少触发系统调用

上一个版本，主线程中使用了多路复用器，在其他线程中处理业务逻辑；

为了规避事件重复调起，需要频繁的register(key)、key.cancel()

register会触发系统调用 epoll_ctl

cancel会触发系统调用:epoll_ctl

那么如何才能使用多线程，还少触发系统调用?

---

我们为啥提出这个模型？

考虑资源利用，充分利用cpu核数

考虑如果有一个fd执行耗时，在一个线性里会阻塞后续FD的处理

> 

当有N个fd有R/W(读写)处理的时候：

将N个FD 分组，每一组一个selector，将一个selector压到一个线程上

> 

最好的线程数量是：cpu的核数 或 cpu核数*2

其实单看一个线程：里面有一个selector，有一部分FD，且他们是线性的

多个线程，他们在自己的cpu上执行，代表会有多个selector在并行，且线程内是线性的，最终是并行的fd被处理

但是，你得明白，还是一个selector中的fd要放到不同的线程并行，还需要canel调用嘛？  不需要了！！！

在一个线程中调用select(),调用select()后要做selectedKeys的处理，selectedKeys处理之后再做select()

![608](227FBE9F224E47E7B5F4B268E74481C1)

![607](242FA0D0BC25409392D7F17AB6B27F74)

参考netty的架构图，一个NIOEventGroup中有很多NioEventLoop


上边的逻辑其实就是分治，我的程序如果有100W个连接，如果有4个线程（selector），每个线程处理 25W个连接

那么，可不可以拿出一个线程的selector就只关注accpet ，然后把接受的客户端的FD，分配给其他线程的selector

那是不是就见过下面这张图

![609](6830DD771FD0411594E1D4188C7F123F)

# 答疑

## 1.writeHandler为什么会被重复调起，消息发完了应该就不会再调起了？

注意多路复用器，关注的是读写状态，而不是读写操作

你真的要明白：你想什么时候写？不是依赖send-queue是不是有空间（多路复用器能不能写是参考send-queue有没有空间）
    
    