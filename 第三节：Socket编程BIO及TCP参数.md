# TCP参数
[TOC]

## 常用命名

lsof -p  看某一进程的文件描述符

netstat -natp   看内核socket建立的过程

tcpdump  抓取网络通讯数据包

## 实验

### SocketIOPropertites

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

```java
package com.bjmashibing.system.io;

import java.io.*;
import java.net.Socket;

/**
 * @author: 马士兵教育
 * @create: 2020-05-17 16:18
 */
public class SocketClient {

    public static void main(String[] args) {

        try {
            Socket client = new Socket("192.168.150.11",9090);

            client.setSendBufferSize(20);
            client.setTcpNoDelay(true);
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while(true){
                String line = reader.readLine();
                if(line != null ){
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


```



### SocketClient

### 实验过程以及结果

    连接一台服务器：服务器程序
    javac SocketIOPropertities.java && java SocketIOPropertities
    
    ----------------------------------
    另一个窗口：查看网络连接
    netstat -natp
    
    ==客户端启动以后，建立了一个socket连接，但是socket没有分配使用；Recv-Q=4
    
    netstat -natp
    
    ==服务端回车：socket连接分配了，分配给了7932/java
    
    ----------------------------------
    再开一个窗口:打开TCP监控,开启监听，抓取数据包
    tcpdump -nn -i eth0 port 9090
        服务端会开启LISTEN状态，监听端口号9090
        
    jps   得到服务端程序的进程号是7932
    
    lsof -p 7932    可以看到文件描述符为5， TYPE=IPv4   NODE=TCP   NAME=*:websm(LISTEN)  是LISTEN状态
    
    ==客户端启动以后，抓到三次握手
    
    lsof -p 7932   文件描述符还是5
    
    lsof -p 7932   文件描述符多了6  TYPE=IPv4 NODE=TCP  NAME=node01:websm->node02:47513(ESTABLISHED)
    
    ==========================
    连接另一台服务器：客户端程序
    javac SocketClient.java && java SocketClient
    
    ==========================
    
    1.服务器刚启动的时候
        1）TCP会开启LISTEN状态，监听端口号9090
        
        2）服务器进程的文件描述符为5，TYPE=IPv4, NODE=TCP,NAME=*:websm(LISTEN)  是LISTEN状态
    
    2.启动客户端
        1）服务器抓到三次握手
        
        2）服务器内核建立了一个socket连接，但是socket没有分配使用;Recv-Q=4
            说明什么是连接，连接不是物理的，双方通过三次握手开辟了资源，可以为对方提供服务了，那这个连接就已经有了；它看不见摸不着，是通过资源来代表的
            
    3.服务器端回车
        1）服务器打印：client port:47513
        client read some data is :4 val:1111   内核中缓存的4个字节也读到了
        
        2）socket连接分配了，分配给了7932/java
        
        3）服务器进程多了文件描述符6
    
### 总结：

#### 知识点1：TCP的三次握手

TCP是面向连接的，可靠的传输协议

三次握手之后在内核级开辟资源，这个资源代表了连接

    三次握手：
    客户端发送syn到服务端；
    服务端发送syn+ack到客户端
    客户端再发送ack到服务器

#### 知识点2：socket

socket是一个四元组（客户端ip地址、客户端的端口号、服务端的ip地址、服务端的端口号）；

socket也是内核级的；三次握手之后，即使不调用accept，也会完成socket连接的建立，以及==数据的接受过程==

#### 面试题：服务器进程可不可以有XPORT(80)、YPORT（9090）？
有一个客户端和服务端；客户端的ip地址：AIP，随机端口号CPORT：123；服务端的IP地址：XIP，端口号：XPORT；请问服务器进程可不可以有XPORT(80)、YPORT（9090）？

可以

客户端的CPORT 连接 服务端的XPORT以后，会得到一个四元组（AIP_CPORT + XIP_XPORT）

#### 面试题2：服务器是否需要为client的连接分配一个随机端口号

不需要

**客户端和服务端建立连接以后，会开辟资源，资源里面包含了一个条目（AIP_CPORT + XIP_XPORT）;这个条目在客户端和服务端的内核里都保存了这个对应关系（AIP_CPORT + XIP_XPORT）以及内存缓存区send_buf、rece_BUf(即会开辟buffer)，**

因为这个条目（AIP_CPORT + XIP_XPORT）是唯一标识，就没有必要再拼一个随机端口号了


当然了一个客户端也可以开启很多线程连接一台服务器

==四元组让Socket变得唯一==

端口号最多有65535


#### 知识点：

当accept以后，会拿到内核socket抽象的代表FD,在java中FD被包装为java.net.Socket

#### 问题：为什么服务器启动服务会报端口号被占用？

因为如果端口被占用，服务端ip+端口号就不再是唯一的


![301](FDDD8BE0DBE347748A18BA8EA6413681)

#### 服务端、客户端连接过程以及资源分配

![302](2C7F5A77D0E54A638085E25820443A9C)

## BACK_LOG

如果Back_log=2,可以建立的socket是3个，2个是备胎

第四个连接，状态为SYN_RECV，说明三次握手（客户端发了syn,服务端没有发送syn+ack）只进行了一次握手

back_log要合理配置，如果back_log的socket长时间不处理，会连接超时

![303](71E577672622401C8D9AC90BAB1D9AEB)

## 三次握手-窗口机制，解决拥塞（腾讯面试必问）

第一次：有一个seq的序列号，win 窗口大小=14600(客户端申请窗口大小)；mss=1460(数据内容大小)

第二次：回传一个seq的序列号，ack=第一次seq+1；win=1448（服务端答复）

第三次：ack=1; win=115(最终协商的窗口大小)

数据包应该多大？  利用ifconfig  查看MTU，MTU=1500字节

数据内容大小=MTU-包头的ip(20字节)-端口号(20字节)=1500-20-20=1460

### 腾讯面试必问：TCP中什么叫拥塞？

拥塞：如果窗口被填满了，回的数据包里  确认的ack告诉没有余量了；

这是客户端就可以先阻塞自己，不发了；

等服务器内核把数据处理一些以后，服务器再补一个包，通知客户端可以发了

==拥塞控制：既能提高性能，又能防止塞爆了==

没有拥塞控制，客户端一味的发送数据，一些数据是会丢掉的

![304](BE786857217740A7B9D9470D8E13E6EA)

#### 实验验证

实验过程在视频1:19:00

## TCP-NoDelay

### 实验1：NoDelay=false

    client.setSendBufferSize(20);//发送的缓冲区为20字节
    client.setTcpNoDelay(false);//默认为起优化的状态：

**传输的数据可以超过设置的缓冲区大小**

### 实验2：NoDelay=true

==着急发送消息的时候，数据量还不大的情况下，可以不采用优化；因为这个优化，会导致吞吐量下降==

## OOBInline=false   NoDelay=false

**两个需要组合使用才能有效果**

client.setOOBInline(false);//**关闭首字节快速发送**

## CLI_KEEPALIVE()

CLI_KEEPALIVE: keepalive的心跳是否代表长连接

TCP协议中规定，如果双方建立了连接，很久都不说话，对方还活着吗？

==传输控制层要发一些确认包，作心跳，确认对方是否还活着==

==周期性做这件事，能为后期资源的把控、效率、性能有一定的帮助==


注意概念别混淆：

1.HTTP也有Keepalive

2.负载均衡里有Keepalived（高可用进程）

![305](67A0FFBC96C242799D704582FF3CC557)


# 网络IO 变化 模型

## 面试常问概念

同步

异步

阻塞

非阻塞

没有异步阻塞模型

接下来要用的指令：starce -ff -o out cmd

## 实验:BIO + 多线程

```
TestSocket
```

    /usr/java/j2sdk1.4.2_18/bin/javac TestSocket.java    用jdk1.4.2编译代码
    
    starace -ff -o out  /usr/java/j2sdk1.4.2_18/bin/java TestSocket
        -ff  抓取所有跑的线程
        -o   给出输出文件的前缀，追踪每一个线程对内核的系统调用，并每一个线程都独立输出
        
    
## 学习技巧：利用man帮助手册进行学习
    man man
    
    man tcp
    
    man ip
    
    man bash
    
    man 2 socket
    
![306](AC0DB010A14A4BA292A0F524F4BD2C3B)

## 汇总BIO的代码逻辑

计算机中有application应用程序，Kernel内核，无论哪种IO模型，只要想通过网络IO进行通信，application和kernel之间有一个固定的环节不被破坏的(只能修正)就是一套基本的系统调用（Socket）

    这套系统调用是：
    Socket返回文件描述符，例如FD3
    bind绑定，例如绑定到8090端口：bind(fd3,8090)
    listen监听FD3
    
    可以通过netstat -natp 进行查看结果，结果：0.0.0.0:8090  0.0.0.0:* LISTEN
    
    应用程序走完上面这几步，才能拿到这个Listen状态
    
    有了Listen状态，调用accept，例如accept(fd3,      
    等待客户端的连接，这时进入阻塞状态
    
    客户端连接以后，得到另一个文件描述符，例如FD5
    需要注意：有两个socket，一个是服务端的Listen状态的socket，一个客户端连接以后的socket
    
    有了FD5以后，想读，需要receive()，这时又有可能进入一个阻塞状态：等待数据传输
    
    java代码中有两处会出现阻塞：一个是等待客户端连接；一个是读取数据阻塞
    
    是如何解决这两个阻塞的呢？抛出去一个线程（进行数据的读写）
    
    主线程等待客户端连接，客户端连接以后clone一个线程
    
    clone的线程：阻塞，读取一个连接
    
![307](E30CD73B105E4822BF2BE883C738E17A)

# BIO整理:

当有一个连接进来的时候，主线程accept()接受，得到这个连接，并克隆出一个新的线程，在这个新的线程中receive和read，读取那个连接发送过来的数据；

因为等待连接的建立是阻塞的，读取数据是阻塞的，所以在并发的情况下，由一个线程等着连接进来，然后排队挨个处理每个到达的人，并把每个人发来的数据绑定到独立的线程当中，那么即便他们阻塞也不会阻塞其他的操作



