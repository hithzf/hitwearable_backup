package com.hitwearable;


/**
 * Created by hzf on 2017/5/16.
 */

import android.content.Intent;
import android.net.TrafficStats;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

/**
 * 网络操作工具类
 */
public class NetworkUtil {
    /**
     * 接收文件
     * @param targetIP 目标IP
     * @param fileReceivePort 文件接收端口
     * @param filePath 文件存储路径
     */
    public void receiveFileBySocket(final String targetIP, final int fileReceivePort, final String filePath) {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MyApplication.getContext());
        new Thread() {
            public void run() {
                try {
                    //创建socket，连接发送端
                    Socket receiveSocket = new Socket(targetIP, fileReceivePort);
                    //数据流操作
                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(receiveSocket.getInputStream()));

                    String savePath = new StringBuilder(filePath).append("/").append(dataInputStream.readUTF()).toString();
                    DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new BufferedOutputStream(new FileOutputStream(savePath))));

                    int bufferSize = 1024;
                    byte[] buf = new byte[bufferSize];
                    while (true) {
                        int read = 0;
                        if (dataInputStream != null) {
                            read = dataInputStream.read(buf);
                        }
                        if (read == -1) {
                            break;
                        }
                        dataOutputStream.write(buf, 0, read);
                    }
                    LogUtil.d("NetworkUtil", "接受完毕");
                    //更新数据库
                    Msg msg = new Msg(savePath, Msg.TYPE_RECEIVED, System.currentTimeMillis());
                    msg.save();
                    //通知更新数据
                    Intent intent=new Intent("com.hitwearable.LOCAL_BROADCAST");
                    intent.putExtra("msg", msg);
                    localBroadcastManager.sendBroadcast(intent);

                    dataOutputStream.close();
                    dataInputStream.close();
                    receiveSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }

    /**
     * 发送文件
     * @param sendPort 文件发送端口
     * @param filePath 文件所在路径
     */
    public void sendFileBySocket(final int sendPort, final String filePath){
        new Thread()
        {
            public void run()
            {
                ServerSocket serverSocket = null;
                try
                {
                    //创建socket
                    serverSocket = new ServerSocket(sendPort, 1);
                    LogUtil.d("NetworkUtil", "等待接收端连接");
                    Socket sendSocket = serverSocket.accept();
                    LogUtil.d("NetworkUtil", "接收端完成连接");
                    //数据流操作
                    File file = new File(filePath);
                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
                    DataOutputStream dataOutputStream = new DataOutputStream(sendSocket.getOutputStream());

                    File tempFile = new File(filePath);
                    dataOutputStream.writeUTF(tempFile.getName());
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    while (true)
                    {
                        int readLength = 0;
                        if (dataInputStream != null)
                        {
                            readLength = dataInputStream.read(buffer);
                        }
                        if (readLength == -1)
                        {
                            break;
                        }
                        dataOutputStream.write(buffer, 0, readLength);
                    }
                    dataOutputStream.flush();
                    //关闭流和socket
                    dataOutputStream.close();
                    dataInputStream.close();
                    sendSocket.close();
                    serverSocket.close();
                }
                catch(BindException bindException){
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 接收文本
     * @param textLocalPort 文本接收端口
     */
    public void receiveTextByDatagram(final int textLocalPort){
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MyApplication.getContext());
        new Thread()
        {
            public void run()
            {
                    try
                    {
                        DatagramSocket ds = new DatagramSocket(textLocalPort);
                        byte[] buf = new byte[1024];
                        DatagramPacket dp = new DatagramPacket(buf,buf.length);
                        ds.receive(dp);

                        //更新数据库
                        Msg msg = new Msg(new String(dp.getData(), 0, dp.getLength(), "GBK"), Msg.TYPE_RECEIVED, System.currentTimeMillis(), Msg.CATAGORY_TEXT);
                        msg.save();
                        //通知更新数据
                        Intent intent=new Intent("com.hitwearable.LOCAL_BROADCAST");
                        intent.putExtra("msg", msg);
                        localBroadcastManager.sendBroadcast(intent);
                        ds.close();
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
            }
        }.start();
    }

    /**
     * 发送文本
     * @param messageSend
     * @param targetIP
     * @param textTargetPort
     */
    public void sendTextByDatagram(final String messageSend, final String targetIP, final int textTargetPort){
        new Thread() {
            @Override
            public void run() {
                try {
                    DatagramSocket ds = new DatagramSocket();
                    DatagramPacket dp = new DatagramPacket(messageSend.getBytes(), messageSend.getBytes().length,
                            InetAddress.getByName(targetIP), textTargetPort);
                    ds.send(dp);

                    ds.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
