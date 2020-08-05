package com.limiao.system.io;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/*
* 非阻塞代码
* */
public class SocketNIO {

    public static void main(String[] args) throws Exception {

        LinkedList<SocketChannel> clients = new LinkedList<>();

        //在java中，Channel类似文件描述符，是一个抽象层，双向的输入输出，整合了输入流、输出流
        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); //重点  OS  NONBLOCKING!!!

        ss.setOption(StandardSocketOptions.TCP_NODELAY, false);
//        StandardSocketOptions.TCP_NODELAY
//        StandardSocketOptions.SO_KEEPALIVE
//        StandardSocketOptions.SO_LINGER
//        StandardSocketOptions.SO_RCVBUF
//        StandardSocketOptions.SO_SNDBUF
//        StandardSocketOptions.SO_REUSEADDR




        while (true) {
            //接受客户端的连接
            Thread.sleep(1000);
            // 得到一个客户端的具体文件描述符的一个数值
            SocketChannel client = ss.accept(); //不会阻塞？  操作系统方面：-1    在java面向对象方面：NULL
            //accept 调用内核了；1.没有客户端连接进来，返回值？ 在BIO的时候一直卡着，但是在NIO，不卡着，OS返回-1，java中返回null
            //如果来了客户端的连接，accept返回的是这个客户端的fd，例如OS返回5， java中返回client对象
            //NON BLOCKING 就是代码可以往下执行，只不过有不同的情况

            if (client == null) {
                System.out.println("null.....");
            } else {
                client.configureBlocking(false);//重点 非阻塞
                //socket领域有：1.服务端的listen socket<连接请求三次握手之后，往我这里扔，我去通过accept 得到连接的socket>、
                // 2.连接socket（连接建立以后数据读写使用的）

                int port = client.socket().getPort();
                System.out.println("client...port: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);  //可以在堆里   堆外

            //遍历已经连接进来的客户端能不能读写数据
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
