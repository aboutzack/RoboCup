
以下文件若无特别说明根目录皆为 rcrs-server/scripts/remote-control
涉及用户分为三种，LOCAL_USER、REMOTE_USER和本地用户，其中REMOTE_USER为跑图机器，LOCAL_USER为运行脚本的机器，本地用户一般为笔记本， LOCAL_USER根据具体情境可以和本地用户相同也可以和REMOTE_USER相同

涉及n组机器，一组4台，一台为服务端运行Kernel以cn1命名 , 三台为客户端分别运行三种智能体分别以FB， PF,  AT的顺序以cn2 cn3 cn4命名（n 为组序）， 以下皆以c11等作为代表进行说明
组别配置懒得写了，反正yyx会配好的：）

我们的测试服务器目前只涉及c1一组
# 本地用户配置
以c1组作为示例
以下配置在本地主机做一次就好
终端运行以下代码
```
echo "118.31.126.8	c11" >> /etc/hosts
echo "47.114.161.17	c12" >> /etc/hosts
echo "47.114.172.127 c13" >> /etc/hosts
echo "47.114.113.99	c14" >> /etc/hosts
```
或者直接在 hosts文件中写入



# 本地用户上传文件到REMOTE_USER
## 上传或者更新rcrs-server、code、maps目录中的文件内容
上传或者更新rcrs-server、code、maps目录中的文件内容，分别在“uploadKernel.sh”、"uploadCodes"、“uploadMaps”中修改`/home/yyx/`为你的本地目录
并运行相应脚本
## 上传其它内容到所有REMOTE_USER
`./syscAll.sh  localDir remoteDir`
localdir 指你本地用户要上传自的目录或者文件名
remotedir指要上传到的目录或者文件名
此处的目录和文件都是以本地用户或者REMOTE_USER的home目录作为源目录

## 上传其它内容到c11
`./SyscKernel.sh localDir remoteDir`
参数意义与上一条相同
## 上传其它内容到c12 c13 c14
`./SyscClients.sh localDir remoteDir`
参数意义与上一条相同


# 自动跑全图时

## 脚本配置

### config.sh
1~10行左右
LOCAL_USER为远程用户名，REMOTE_USER 为本地用户名
将文件中的`yyx`替换为本地用户名即可

40~50行左右
TEAM_SHORTHANDS是要跑的队伍的简称，注意需要和队伍代码的根目录名称需要完全相同
TEAM_NAMES数组记录队伍全程，具体名称随意但是要跑的队伍一定要有全程配置，否则会导致报错
### autoRun.sh
MAPS 中是需要跑的地图（们）的名称

## 具体运行
### 首次运行前需要
请确认代码完成上传并且完成配置之后进入这一步
请确认rcrs-server、code、maps文件夹都被正确命名并位于REMOTE_USER的home目录之下
在服务器主机上home目录下打开终端给rcrs-server中的所有文件都赋予权限（为了避免，不如直接赋予777吧）
```
sudo chmod 777 -R rcrs-server
```
### 具体运行
自动运行脚本启动
```
./autoRun.sh
```
以`cluster 1 FREE 1 Time`为成功运行的标志
如果出现`cluster 1 FREE 0 Time`说明运行失败，大概率是lock文件没有被移除，具体解决方法与结束运行相同（见下一条）
**注意：
把c11作为LOCAL_USER 但是在本地使用ssh调用./autoRun时请注意，不能关闭笔记本上调用ssh的终端，否则会导致运行停止。
所以更为推荐的做法是在c11上打开左上角的Applications Places->System Tools->XTerm
在其中运行autoRun.sh(因为服务器的默认终端目前是乱码，只有XTerm能正常显示)
**
### 结束运行
先ctrl+C终止进程
再运行停止脚本
```
./cancelRun.sh 1
```
1 为组别序号，遗漏会导致错误停止

成功停止后仍然会出现“kernel is still waiting”的语句（大概是这句？），属于正常现象，最好把终端关掉再打开新的终端跑下一次



### 删除日志
在硬盘不够的时候并且服务器没有跑图的时候删除日志
(其实不删也问题不大)
```
./deleteLogs.sh
```
### 运行中间文件和结果文件的解释
运行中间文件都会存储在REMOTE_USER的home目录之下
”rsl_last_run.stat“记录已经跑完了或者正在跑的图和相应的队伍名称
”rsl_run.lock“锁文件，用于保证一个服务器一次只运行一张地图一个队伍
”Score.txt“记录分数的文件