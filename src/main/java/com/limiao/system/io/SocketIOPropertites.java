package com.limiao.system.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;

/**
 * @author: 马士兵教育
 * @create: 2020-05-17 05:34
 * BIO  多线程的方式
 *
 *
 */
public class SocketIOPropertites {



    //server socket listen property:
    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final boolean REUSE_ADDR = false;

    //当服务起来以后，很多连接过来，线程不够，排队的数量；超过的全部拒绝
    private static final int BACK_LOG = 2;

    //client socket listen property on server endpoint:
    //keepalive的心跳是否代表长连接
    private static final boolean CLI_KEEPALIVE = false;

    //是否优先发一个字符过去试探一下
    private static final boolean CLI_OOB = false;

    //receive-Q和send-Q跟REC_BUF的关系
    private static final int CLI_REC_BUF = 20;

    //是否重用地址
    private static final boolean CLI_REUSE_ADDR = false;
    private static final int CLI_SEND_BUF = 20;

    //断开连接的速度
    private static final boolean CLI_LINGER = true;
    private static final int CLI_LINGER_N = 0;

    //客户端读取数据的时候，超时时间，超过设置时间抛异常，做其他处理
    private static final int CLI_TIMEOUT = 0;

    //TCP的一个优化算法，当发送数据比较少的时候，可以利用缓冲的概念；是否开启对于业务数据量的大小，业务规模、业务特征是有关系的
    //默认为false，不优化；负负得正，优化：当发送的数据量小的时候，积攒一下，再发送
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
            server.bind(new InetSocketAddress( 9090), BACK_LOG);
            server.setReceiveBufferSize(RECEIVE_BUFFER);
            server.setReuseAddress(REUSE_ADDR);
            server.setSoTimeout(SO_TIMEOUT);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server up use 9090!");
        while (true) {
            try {
                //分水岭；上面只是得到了Server；
                // 在Socket.accept之前，TCP的三次握手能不能走通和TCP什么关系？内核是否开辟资源？
                // 有没有队列在客户端没有接受连接的情况下，客户端就能发数据了；接受数据之后，把数据处理了？

                System.in.read(); // 分水岭

                Socket client = server.accept(); // 阻塞的，没有返回-1的可能性，一直卡着不动  通过查看out.txt文件可以看到accept(4,
                System.out.println("client port: " + client.getPort());

                client.setKeepAlive(CLI_KEEPALIVE);
                client.setOOBInline(CLI_OOB);
                client.setReceiveBufferSize(CLI_REC_BUF);
                client.setReuseAddress(CLI_REUSE_ADDR);
                client.setSendBufferSize(CLI_SEND_BUF);
                client.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(CLI_NO_DELAY);

                new Thread(
                        () -> {
                            while (true) {
                                try {
                                    InputStream in = client.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                    char[] data = new char[1024];
                                    int num = reader.read(data);

                                    if (num > 0) {
                                        System.out.println("client read some data is :" + num + " val :" + new String(data, 0, num));
                                    } else if (num == 0) {
                                        System.out.println("client readed nothing!");
                                        continue;
                                    } else {
                                        System.out.println("client readed -1...");
                                        client.close();
                                        break;
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                ).start();

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
