[TOC]
# linux基本使用
## 梳理课程内容

kernel

**VFS:虚拟文件系统**，就是一棵树，树上的不同节点可以映射到不同的物理地址，每个不同的物理地址可以是不同的具体的文件系统

**FD:文件描述符**，**指针seek**，偏移指向，类似于java的迭代器director

inode：可以理解为id

**pagecache：页缓存，默认为4k**，

知识点1：**两个程序打开同一个文件的话，不是两个程序都把这个文件都加载一次**，其实是**访问的虚拟文件系统VFS**，如果虚拟文件系统发现**已经加载过这个文件了**，你读的东西内存的pagecache已经命中了，那么**虚拟文件系统就会返回你要的pagecache数据**

dirty:

知识点2：程序和硬件之间隔了一个内核，**内核通过pagecache来维护你曾经想读的数据在内存里，如果对数据进行了修改，会标记为dirty；标记为dirty以后，会有一个flush的过程，书写到磁盘当中去；**


**这个dirty标识是 内核对上层所有打开文件的一个统一管理**，并不针对某一个文件的；会有一些调优参数


## 验证环节

df命令：

如图：**文件系统的所有文件都在sda3（3分区），只有boot在sda1(1分区)，1分区下的boot覆盖3分区下原有的boot**

![101](02F6E9E197C74FB4985319E10147CEC6)

**根目录root来源自sda3(3分区)**，计算机引导的时候，内核进入内存，内核会初始化虚拟文件系统VFS，这时这个文件系统是空的，只有一个根，然后把根映射3分区，3分区里面有很多的目录（有root、dev、etc目录）；

**第二步，加载了一下1分区；覆盖了3分区的挂载的boot目录**

==实际在计算机通电启动时，BIOS会从1分区加载kernel镜像==



    unmount /root 将root目录的内容卸载
    
    mount /dev/sda1 /boot 将1分区的内容挂载到3分区的boot目录下


==目录树==结构趋向于稳定，有一个映射的过程

如果现在usr目录数据过多，可以再加一块硬盘，用于存储数据，然后将这块硬盘挂载到usr目录下，这样实现了usr目录的扩容，这就是**光驱、硬盘的挂载**

==冯诺依曼提出的计算机组成：计算器 + 控制器（CPU）+ 主存储器(主存)+ 输入输出设备(I/O)==

文件系统有一个理念：==一切皆文件==，这样就跟输入输出设备(IO)挂钩了

## 文件类型：

    -：普通文件（可执行文件、图片、文本） REG
    d:目录
    l:连接(软连接、硬连接)
    b:块设备(可以自由漂移，不受约束；硬盘)
    c:字符设备(不能自由漂移；键盘、网卡)  CHR
    s:socket
    p:pipeline管道
    [eventpoll]:
    ...

### 硬连接：

**在内存中的描述路径(path)是不一样的，但是是指向同一个物理文件**

**相当于两个变量指向了一个对象，通过任意一个变量对对象进行修改，另一个变量都能反映出来**

    ln /root/msb.txt  /root/xxoo.txt
    
    stat msb.txt  可以看到Inode
    
    stat xxoo.txt  可以看到Inode是一样的
    
    vi xxoo.txt    往里面增加内容
    
    vi msb.txt     msb.txt中的内容也变了
   
    rm -f msb.txt  xxoo.txt还在，但是xxoo前面的引用数量变为1（1就是硬连接被引用的数量）
    
    
### 软连接

在内存中的描述路径(path)是不一样的，但是是指向同一个物理文件

类似于windows的快捷方式

    ln -s /root/xxoo.txt /root/msb.txt  ll时可以到msb.txt .> /root/xxoo.txt 多了一个指向
    
    stat msb.txt
    
    stat xxoo.txt  Inode不同
    
    vi xxoo.txt    往里面增加内容
    
    vi msb.txt     msb.txt中的内容也变了
    
    rm -f xxoo.txt  msb.txt后面的引用变红
    

### 块设备实验

    dd if=/dev/zero of=mydisk.img bs=1048576 count=100 输出一个100M的文件，被0填充
    
        dd:拷贝数据，生成一个文件;
        if：input file;
        /dev/zero 是一个无限大的空，但不占用太多的磁盘空间
        /dev/null 是一个黑洞
        of：out pull
        mydisk.img  磁盘文件
        bs: block size
        1048576: 1M
        
    ll -h 查看结果
    
    losetup /dev/loop0 mydisk.img    将mydisk挂载到loop0(是一个设备，不是物理的，是一个虚拟的映射关系)
    
        
    
    mke2fs /dev/loop0    格式化为ext2格式
    
    df    不显示这个虚拟硬盘
    
    cd /mnt
    
    ll
    
    cd ooxx    是一个空目录
    
    ll
    
    pwd 
    
    mount -t ext2 /dev/loop0  /mnt/ooxx  将loop0挂载到ooxx
    
    df
    
    cd /mnt/ooxx
    
    ll
    
    whereis bash   bash当前的解释程序，你敲字符跟linux交互的那个程序
    
    mkdir bin
    
    ll
    
    cp /bin/bash bin
    
    ll
    
    cd bin
    
    ldd bash   分析bash的动态连接库
    
    cd ..
    
    mkdir lib64
    
    cp /lib64/{libtinfo.so.t,libdl.so.2,libc.so.6,ld-linux-x86-64.so.2} ./lib64/
    
        花括号不允许出现任何的空白符
        
    chroot ./    启动程序
    
    echo $$   得到程序号4278,ls vi  cat 都没有找到命令
    
    echo "hello mashibing" > /abc.txt   打印字符串，利用重定向写到了本目录下的abc文件中
    
    exit          退出程序
    
    echo $$    外面程序的id：4117
    
    cd /
    
    ll   没有abc文件
    
    cd /mnt/ooxx 
    
    ll  发现多了一个abc.txt文件
    
    df
    
    #生成一个mydisk.img的空文件，把它挂在到/dev/loop0, 进行了格式化；
    #再mount到/mnt/ooxx，拷贝了一些bin和bash
    
    unmount /mnt/ooxx
    
    cd /mnt/ooxx 
    
    ll   发现是空的，但是mydisk.img还是存在的
    
**docker中是先有img镜像文件，img中要先放你的程序**，

**docker是一个容器，是一个虚幻的东西，是一个云空间，里面跑的进程，没有操作系统内核什么的，所有的docker复用的是你物理机的内核**
        
先有镜像(img)，才有容器的概念

==镜像需要先挂载，然后读取进程里面的执行文件，然后再准备命名空间，把文件跑到命名空间中，程序才能跑起来，这就是源于文件系统的支撑==
    
        
### 验证不同进程访问同一个文件,FD记录seek指针和偏移量

    lsof -p $$   
        losf显示进程打开了哪些文件
        -p 告诉是哪个进程
        $$ 指当前bash进程
        
    //可以看到FD信息；0u(程序的标准输入system.in)、1u（程序的标准输出system.out）、2u（报错输出system.error）、
    //255u、txt(文本域，程序启动所执行的代码段)、mem（memory,分配的内存空间，做了一些动态链接库的挂载）
        
    
        每个程序都有0u、1u、2u文件描述符;/dev/pts/2(是一个虚拟终端，就是通过TCP连接的SSH的标签页，这个TCP通信)
        
        TYPE:代表哪一类，REG(普通文件)、CHR(字符设备) 
        DEVICE:设备:136(设备号),1(那一块)
        SIZE/OFF:偏移量
    
    vi ooxx.txt
    
    exec 8< ooxx.txt   利用8读取ooxx文件
    
    cd /proc/$$/fd      
    
    ll    可以看到0,1,2,255,8(指向了/root/ooxx.txt文件)
    
    lsof -op $$    -op(o是偏移量) 可以看到FD中有8r（r:读取）,可以看到Node-10227564(Inode)；OFFSET(指针)
    
    stat ~/ooxx.txt  可以看到Inode=10227564
    
    read a 0<& 8     读取的内容放到变量a中，0<& 8(标准输入流,从8读取)；
        read有一个特征：只读到换行符就不读了，所以它不会造成把整个文件都读完
        
    echo $a   查看a读到的文件内容四个字节（最后一个是换行符）：aaa_
    
    lsof -op $$  现在再看ooxx文件的偏移量由ot0变成了ot4
    
    **再开一个窗口（连接程序，这个程序也有一个自己的bash）
    cd /proc/$$/fd
    
    pwd  查看该窗口的进程号是4322，之前的窗口的进程号是4117
    
    exec 6< ~/ooxx.txt  //用一个6的文件描述符代表了ooxx这个文件
    
    lsof -op $$     ooxx文件的文件描述符是6r，并且偏移量是ot0
    // 这时就证明了刚开始提到的：两个程序可以打开一个文件，但是FD里会各自维护各自的指针，这份文件的数据在内存中只有一份
    
    read a <& 6
    
    echo $a   结果是aaa
    
    read a <& 6
    
    echao &a   结果是第二行数据
    
    lsof -op $$    当前ooxx文件的偏移量变为了ot16
    

    
==文件描述符：就是描述了你打开文件的一些信息，例如文件类型，指针、偏移量、Inode、文件==

**通过lsof指令可以查看进程打开了哪些文件**

==内核为每一个进程各自维护了一套数据，这套数据包含了进程的FD（文件描述符），FD里维护了一些关于FD的信息，有偏移量、指针位置、类型、文件名、所处设备的位置、Inode==

#### dd命令:备份、恢复、对拷、创建镜像

    dd if=/dev/sda3 of=~/abcd.img     将3分区（物理分区）的整个分区做了一个压缩文件的备份
        如果前面是物理硬盘，后面是一个文件，这个是备份
        如果前面是一个文件，后面是物理硬盘，这是恢复
        如果两个都是硬盘，是对拷
        如果前面是zero，后面是文件，是在创建一个空的磁盘镜像
        
    
    dd if=/dev/zero of=~/abcd.img    用0填充了一个空文件
    
    dd if=/dev/sda3 of=/dev/sdb1  两个分区对拷
    
#### swap

如果内存中有很多个程序，超过内存的大小(1G)

某个程序执行一定是在内存中的，

这时候可以借用磁盘Swap，先将程序的数据保存到磁盘上

涉及到程序交换

### socket

    echo $$
    
    cd /proc/4398/fd
    
    ll
    
    exec 8<> /dev/tcp/www.baidu.com/80   80端口，看似是一个目录，最终就是文件（一切皆文件），生成一个socket
    
    ll  多了一个文件描述符 8
    
    lsof -op $$ 
        多了TYPE=IPv4, Node=TCP 的

### 总结

> 死记硬背：
> 
> 任何程序都有0(标准输入)、1（标准输出）、2（报错输出）
> 
> /proc:内核映射目录，映射内核的一些变量、属性
> 
> /proc/$$ : 
> 
> $$:当前bash的pid($BASHPID也可以得到pid),是一个特殊的环境变量
> 
> /proc/$$/fd : 当前程序的所有文件描述符
> 
> 用命令lsof -op $$  ：查看当前程序的所有文件描述符的细节
    
    cd /proc      进入进程所在的路径；
    
    ls
    
    cd $$
    
    pwd
    
    ls
    
    cd fd
    
    ll
### Pipeline

#### 重定向

**重定向：不是命令，是一个机制；可以选择让你的IO指向别的地方去**

    File ifile = new File("/ooxx.txt");
    out = new outputstream(ifile); //伴随out变量，在操作系统中会得到一个fd，并且这个fd还能看到
    out.write("123");

重定向机制的实现：

流的方向无非就是输入、输出(I/O),换到操作符上就是 <(输入)  >(输出)

##### 输出重定向：

    ls ./ 1> ~/ls.out  bash程序的1(原先输入到屏幕)，修改为输出到ls.out
    
    ll  多了一个ls.out文件
    
    vi ls.out
    
##### 输入重定向：

    cat ooxx.txt   cat涉及到两个流：输入流（读取文件）、输出流（将内容显示到屏幕）
    
    cat 0< ooxx.txt 1> cat.out    将输出修改为:写到cat.out文件
    
    ll
    
    vi cat.out
    
    
##### read指令对换行符极其敏感

    read a    这是按下换行符，屏幕换行，输入一些内容，再次按下换行符，退出程序; 
    //read指令对换行符极其敏感;键盘是输入，输出是变量a
    
    echo $a
    
    read a 0< cat.out   将变量a的输入修改为从文件读取内容
    
    vi cat.out
    
    echo $a      因为read对换行符极其敏感，所以只读入了aaa_

#### 文件操作符的基本语法
    
**输入输出操作符，左边为文件描述符，并且文件描述符和操作符之间不能有空格**

    ls ./ /oosdfs     打印当前目录以及ooxx目录（ooxx目录并不存在）
        这是走了文件操作符1（标准输出）、2（报错输出）
        
    ls ./ /oosdfs 1> ls01.out   将./的内容写到ls01文件中
    
    
    ls ./ /oosdfs 1>ls01.out 2>ls02.out  这样屏幕就没有输入内容了
    
    ls ./ /oosdfs 1>ls03.out 2>ls03.out  追加到一个文件
    
    ls ./ /oosdfs 2>& 1 1>ls04.out   如果操作符要写文件操作符，一定要加&，该命令会报错
    
    ls ./ /oosdfs  1>ls04.out  2>& 1  1指向文件，2指向1，也指向了文件
    
#### pipeline 管道  |


##### 两个命令：head  tail

head 读文件头，默认为10行

tail 读文件尾

    head -2 test.txt      头两行
    
    tail -2 test.txt      尾两行
    
==head -8 test.txt | tail -1     显示第8行（先读头8行，交给管道，再读最后一行）==

==管道的基本特征：前面的输出作为后面的输入==


##### 进程父子关系

    echo $$      查看进程id：4398
    
    /bin/bash     再启动一个bash
    
    echo $$       另一个bash的进程id：4463
    
    pstre        bash(4398)——bash(4463)
    
    ps -fe | grep 4398
    
    exit   退出了子进程4463，回到父进程4398
    
    echo $$       进程id:4398
    
    
##### 变量，export

    x=100
    
    echo $x      得到值100
    
    /bin/bash    再起一个子进程
    
    echo $$      
    
    echo $x      子进程取不到父进程中的变量
    
    exit
    
    export x     让x具备导出能力
    
    /bin/bash
    
    echo $$
    
    echo $x       子进程读取到父进程的变量
    
    
==这也就是为什么linux安装系统要修改etc/profile(vi /ect/profile)时，要定义环境变量时要加关键词export==

##### 管道与指令块

指令块：在花括号里放很多指令，这些指令是在同一个进程中执行

    { echo "123"; echo "abd"; }            分号结束
    
    echo $$
    
    a=1
    
    echo $a
    
    { a=9 ; echo "dfadf"; } | cat 
    
    echo $a           值为1
    
    
==大家都知道：管道可以衔接输入和输出；但是如果只知道这个，在工作中会踩坑，而且会死的很惨==

==管道的机制是：我写了一个文本字符串，回车；大家都知道**bash是脚本解释执行**，会解释这个文本段；解释的时候，发现管道，会在左边启动一个子进程，右边再启动一个子进程；并让两个进程的输入输出通过管道对接起来，所以“dfadf”能打印出来==

a=9是子进程的赋值，父进程还是1

##### 管道与父子进程

    echo $$ | cat         4398是父进程号
    
    echo $BASHPID | cat    4496是新的子进程号
    
这块的本质区别也是一个坑，==$$的优先级高过了管道，$BASHPID低于管道的级别；==

换句话说，bash是个解释执行的程序，当执行echo $$ | cat 的时候，bash没有看到管道，先看到了echo $$，因为echo $$的优先级高于管道，所以先把$$扩展成4398（这叫参数扩展），再看到了管道


执行echo $BASHPID | cat时，因为管道的优先级高于$BASHPID，管道的左边起了一个子进程4496，看到了$BASHPID,将$BASHPID替换为4496被输出

##### 验证pipeline

    { echo $BASHPID; read x; } | { cat; echo $BASHPID; read y;}        
    //输出：4512；用管道衔接了两个代码块; read x 和read y 会将进程阻塞
        4512是管道左边的子进程，父进程是4398
    
    然后双夹再开一个进程访问这台机器
    
    ps -fe | grep 4398     发现进程4398有两个子进程：4512、4513

    cd /proc/4512/fd
    
    ll    看到文件操作符1指向了pipe[39968]
    
    cd /proc/4513/fd
    
    ll   看到文件操作符0指向了pipe[39968]

    lsof -op 4512      看到文件操作符1，是一个FIFO(先进先出)的pipe , node=39968
    
    lsof -op 4513      看到文件操作符0，也是一个FIFO的pipe
    
## pagecache

pagecache是Kernel的折中方案

application应用程序有缓存区Buffer

### 面试题：为什么我们喜欢用Buffer的IO呢，为什么Buffer的IO要快呢？

==应用程序要访问内核，内核也是一个应用程序，应用程序跟内核交互的时候，夹杂着一个system.call(系统调用)的东西==


### system.call怎么实现的？   

Ox80(int:cpu的指令；Ox80=128  1000 0000)

**Ox80是一个值，这个值要放到cpu的寄存器里，和寄存器中的中断描述符(向量表)做匹配的，**
    
**中断向量表有255个（因为一个字节最大可以表示255）**，128是call back方法
    
int Ox80一旦被cpu读到以后，会走call back，保护现场，恢复线程，从用户态切换到内核态
    
**内核中的pagecache一次读4K，压榨机器的内存**

硬盘中也有一个缓冲区

**原先呢，硬盘数据 -> cpu的寄存器 -> kernel的pageCache**

**有了协处理器DMA（direct memory access直接内存访问）,数据可以不走cpu，实现硬盘数据直接到Kernal的pageCache**

![102](4B206DD2FBBE4B74874B96D6905FA106)

### 关机

**虚拟机--电源-- 关闭客户机**：==相当于轻按电源键，电源会产生一个中断，告诉cpu要关机，调内核关机的过程，会有pagecache回写磁盘的过程，会保存所有的数据，是正常关机==。


==虚拟机--电源--关机：相当于把主机的电源线拔掉，来不及把内存中的脏页回写磁盘==

### 实验

1.现在有一个mysh的脚本:删除了一些文件，然后将OSFileIO文件跑起来

    rm -fr *out*
    /usr/java/jdk1.8*/bin/javac OSFileIO.java
    strace -ff -o out /usr/java/jdk1.8*/bin/java OSFileIO $1  启动需要一个参数

2.OSFileIO文件：不停往out文件中写“123456789”
3. 执行脚本：  ./mysh 0
4. 执行一会，然后直接"关机"（断电的那种）
5. 然后重启虚拟机，重新连接Xshell，查看out文件的大小，发现out文件的大小还是0


```
OSFileIO ??
```

### 线程切换，挂起，中断过程

多个应用程序，如果一个应用程序running状态，调用了 系统调用，要从磁盘中读数据，

**这时cpu会调用int Ox80 系统中断，应用程序保护现场（保护现场：cpu 一二三级缓存的数据都保存到应用程序的缓冲栈）cpu的态也从用户态切换到内核态，开始执行kernal相应的系统调用，**

**你要读的数据，kernal要先分析pagecache中有没有这部分数据，pagecache没有触发缺页，走DMA协处理器，将数据从磁盘读取到pagecache中；这时呢，应用程序就会进入一种态：==挂起状态==，这时该应用程序就不在内核的进程调度中了(内核的进程调度是调用活跃的进程）**

DMA协处理器拷贝完数据，会有一个中断通知到cpu，告诉cpu曾经有一个应用程序在挂起的时候关注过这个数据；中断过来以后，会把应用程序的状态变为：==可运行状态==，未来在某个时间片进程再调度回来，这时应用程序再执行的时候，恢复现场，数据已经有了，就能够继续执行了


![103](C1651D9D3CF84D6683BE135628AD55B5)
