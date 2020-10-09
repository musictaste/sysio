[TOC]

# 简述Netty Reactor工作架构图

==多路复用器-单线程解决并发的代码必须会，必须手撕==

图片607.jpg

上图是Netty Reactor工作架构图，是一个主从的

Boss group负责IO的accept，会用到多路复用器selector和任务队列，在EventLoop中先select，然后得到selectKeys,最后执行tasks

在processSelectedkeys会触发Worker group，而一个NioEventGroup就是一个线程，会把客户端连接的FD注册到这个线程的Selector，而不是主线程的selector中

从的EventGroup也会执行一个loop，在处理selectKeys时（也就是处理读写R/W），尤其是读操作，会从io中读出数据来，编解码，校验、过滤会涉及到pipeline

这张图中有三个线程<一个蓝色框就是一个线程>，Worker Group中Selector中的客户端连接来源于Boss Group中accept的连接


---
这张图可以简化成单线程的，即没有worker Group；在Boss group的NioEventGroup中处理R/W

注意：这是IO Threads，关于IO上读写事件的线程，不讨论IO读完之后的处理在当前线程还是多线程

# 手写代码，理解Netty模型及IO模型



