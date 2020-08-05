package com.limiao.system.io;

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

            //发送的缓冲区为20字节
            client.setSendBufferSize(20);
            client.setTcpNoDelay(true); //false:负负得正，优化数据
            client.setOOBInline(false);//关闭首字节快速发送
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while(true){
                String line = reader.readLine();
                if(line != null ){
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                        //注意：没有flush
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
