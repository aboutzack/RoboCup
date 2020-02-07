# RoboCup

这里是我们的RoboCup比赛的Git仓库，我们将通过Github精诚合作至比赛结束，重要通知会同步更新于此

## 团队成员
- AT 蔡冠宇、阳雅珣、高益基
- FB 葛雨晴、陈冉飞、陈续兴
- PF 丁佳香、越铂淳、熊璟

## 相关网站
- 2020世界赛 <https://rescuesim.robocup.org/robocup-2020/>
- 往届世界赛 <https://rescuesim.robocup.org/events/past-events/>

## 项目结构
- CSU/ 是学长学姐参赛的代码，我们将在这之上进行开发
- MRL/ 是2019年冠军队伍，目前此代码在默认地图运行报错，其他地图内存爆炸
- AIT/ 是2019年亚军队伍，此代码可以直接运行
- rcrs-adf-sample/ 是2019年的sample代码
- rcrs-server/ 是2019年server代码，也是目前我们使用的server，rcrs-server/scripts/ 已经替换成上届主办方提供的脚本，实现了许多测试等功能，可以加速我们的开发
- maps/ 是2019年国赛比赛地图
- rcrs-manual.pdf 是2019年的参赛手册，包括了启动项目的步骤和项目简述等

## 团队协作
在这期间，尤其是不进行Teamwork的时候，我们要每天在这里的日志下面记录下点东西（主要是为了保证团队活跃，减少摸鱼划水）比如今天为了这个项目做了哪些工作。


唔，哪怕你什么都没做，写日记也行。commit时备注‘更新日志’

**注意** ：

- 项目拉取后记得先按照rcrs-manual.pdf里的步骤编译项目
- 使用Git pull命令将文件夹存到本地，在本地checkout新分支，在此分支上开发，开发完毕后必须经过测试代码无bug后merge到master分支，然后push到github仓库

## 时间节点
**截至2020年1月24日，当前的任务是：**

- 熟悉项目，各小组分配任务，准备开发。

**世界赛**

- Pre-Registration……………………………….: February 22, 2020 (23:55 GMT)
- Team Description Paper (TDP) submission.: February 29, 2020 (23:55 GMT)
- Qualified teams announcement…………..…: March 10, 2020
- Camera-ready TDP submission………….….: May 31, 2020 (23:55 GMT)
- Tournament dates………………………..……: June 25-28, 2020

**中国赛**

waiting...

## 日记

2020年1月23日

CGY：新建了Git仓库，整理项目结构，编写README，确定团队协作要求<br>
GYJ：划水<br>

2020年1月24日

CGY：划水<br>
GYJ：划水<br>

2020年1月25日

YBC：划水<br>
CGY：划水<br>

2020年1月26日

CGY：熟悉代码<br>

2020年1月27日

CGY：查看server脚本文件，学习框架<br>

2020年1月29日

YBC：查看阅读server框架<br>
CGY：阅读server和client的脚本文件，学习框架<br>
GYJ：熟悉代码<br>

2020年1月30日

YBC：熟悉代码<br>
CGY：阅读脚本文件、代码、rule和manual<br>

2020年1月31日

GYJ：继续熟悉代码，尝试在mac系统上跑图，成功<br>
CGY：阅读server和client的脚本文件，学习框架<br>

2020年2月1日

DJX：在家里的台式机上安装了Ubuntu18.04，安装了RCRS服务器，获取编译ADF<br>
CGY：看CSU代码<br>
YBC：阅读rules，结合框架理解算法代码和实体的联系<br>

2020年2月2日

CRF：复习计算几何，复现凸包算法C++代码。<br>
GYQ：阅读代码<br>
CGY：将sample，CSU，框架结合起来看<br>
YBC：结合框架理解算法代码和实体的联系<br>
GYJ：复习Kmeans算法，学习canopy算法<br>

2020年2月3日

CRF：学习凸包算法中不同的点集预处理方法。<br>
GYQ：结合框架代码类图阅读框架代码<br>
CXX：熟悉代码<br>
CGY：看sample+框架+写注释<br>

2020年2月4日

CRF：学习计算几何中二维平面基本的点、线、面运算以及代码实现。<br>
GYQ：复习Kmeans算法,并学习其改进版<br>
GYJ：学习shell语言，读sample<br>
CGY：学习sample和框架，写注释。发现sample一个bug并修复<br>

2020年2月5日

YBC：将sample与CSU结合对比学习，写注释，修复一个bug<br>
GYQ：浏览赛事官网和阅读rules<br>
CGY：阅读完sample代码，对项目结构有了更清晰的了解，粗略写了AT和部分通用代码的注释<br>
CRF：结合项目中的程式学习K-means算法。<br>
CXX：复习凸包算法及k-means聚类算法。<br>

2020年2月6日

CRF：理解程式中K-means算法的思想以及应用场景。<br>
GYQ: 继续阅读框架代码<br>
YBC：熟悉Kmeans和PathPlanning,通过sample里的ActionExtClean理解PF是如何进行道路清障<br>
YYX：阅读rules,熟悉框架结构<br>
CGY：修复MRL的一个bug，给台式机安装环境<br>

2020年2月7日

CRF：熟悉框架，理解K-means代码。<br>
GYQ: 继续阅读框架代码<br>
YBC：按照自己理解粗略写了PF的注释，理解CSU的PF是如何进行清障
