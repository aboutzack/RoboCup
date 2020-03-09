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
GYJ：跑MRL观察员别人的智能体策，学习sample<br>

2020年2月6日

CRF：理解程式中K-means算法的思想以及应用场景。<br>
GYQ: 继续阅读框架代码<br>
YBC：熟悉Kmeans和PathPlanning,通过sample里的ActionExtClear理解PF是如何进行道路清障<br>
YYX：阅读rules,熟悉框架结构<br>
CGY：修复MRL的一个bug，给台式机安装环境<br>

2020年2月7日

CRF：熟悉框架，理解K-means代码。<br>
GYQ: 继续阅读框架代码<br>
YBC：按照自己理解粗略写了sample的PF的注释，理解CSU的PF是如何进行清障<br>
CGY：看源码，联机测试，跑MRL和CSU代码<br>
GYJ：跑CSU的代码，看源码<br>

2020年2月8日

CRF：看sample代码，理解思想。<br>CXX：开始阅读sample代码，了解框架结构。<br>
CGY：排查代码跑不起来的原因，修复代码<br>
GYQ: 继续阅读框架代码<br>

2020年2月9日

GYQ: 因为笔记本内存跑不动部分地图，研究了一下如何加装内存条<br>
CRF：理解sample的框架结构，理解代码。<br>
XJ:重装了一遍双系统，新建了仓库，阅读了rules，熟悉了下框架结构，运行了下服务器。<br>
GYJ：重学git，看sample<br>

2020年2月10日

CRF：查看sample框架中的各个部分的大致功能，理清框架结构。<br>
GYQ: 看CSU代码<br>
YBC：阅读CSU的寻路和聚类算法，理解PF_ActionExtClear具体内涵<br>
GYJ：sample和csu的代码对比着看<br>
XJ：将rules和manul阅读完<br>
CGY：看CSU的kmeans和pathplanning算法<br>
CXX：继续阅读sample代码熟悉架构

2020年2月11日

GYQ：阅读CSU代码。<br>
CRF：理解sample中智能体移动、清障等基本操作的代码。<br>
CXX：熟悉CSU代码的架构。<br>
YBC：理解PF_ActionExtClear<br>
XJ：阅读PF代码并理解<br>
CGY：看CSU策略，阅读MRL和CSU的TDP，删除没用的文件<br>
GYJ：尝试调整config.sh以运行autoRun.sh自动测试脚本未成功<br>

2020年2月12日

CRF：理解sample中智能体决策的代码以及思想。<br>
YBC：整理CSU的清障思想，调参数跑图。<br>
XJ:继续阅读框架代码。<br>
GYJ：mac自带的bash版本很低，升级了bash。看CSU代码<br>
CGY：跑去年决赛地图，移植MRL的代码查看效果，确定开发方向<br>

2020年2月13日

CRF：根据配置文档梳理框架整体结构。<br>
XJ：阅读其他队伍的代码<br>
CXX：继续阅读CSU代码<br>
CGY：测试，发现FB_ActionExtMove存在在一条路上反复来回走动的问题，已找到解决办法<br>
GYJ：再测自动测试脚本未成功，阅读CSU代码<br>

2020年2月14日

CRF：结合框架和TDP理解整体思路。<br>
YBC：根据警察存在的问题对代码进行试改进，还没跑图实验<br>
CXX：熟悉MRL框架。<br>
CGY：理解MRL的at代码，尝试改进<br>
GYJ：看CSU代码，运行测试理解其策略<br>

2020年2月15日

GYQ：阅读sample代码并与CSU比对。<br>
YBC：理解MRL警察的寻路算法和清理算法，试改进CSU的警察<br>
CRF：按照框架分块阅读CSU代码。<br>
GYJ：用CSU把所有图测试了一遍，与代码结合理解策略，充分感受到pf侦查算法的低效<br>
CGY：修改at的ActionExtMove，学习aur的策略<br>

2020年2月16日

CRF：根据CSU运行结果分析优缺点，对应相应的代码进行理解。<br>
GYQ：阅读CSU代码。<br>
YBC：试修改警察的RoadDtector,成果待验<br>
GYJ：阅读理解了autoRun.sh，尝试修改config.sh以运行自动测试。<br>

2020年2月17日

GYQ：运行CSU代码分析智能体行为，对应策略代码。<br>
CRF：分析CSUSearchForFire中寻找火源的策略以及CSUBuildingDetector中建筑物体检测的相关程式。<br>
CGY：修复at的ActionExtMove的stuck判定和处理的问题、测试<br>
GYJ：继续配自动测试脚本<br>

2020年2月18日

CRF：调试寻找火源的代码，查看并对比运行效果。<br>
GYQ：加装内存，尝试跑一些大图。<br>
YBC：调试寻路代码，在一些判断上还有问题。<br>
CGY：修改search代码<br>
GYJ：划划划划划<brß>

2020年2月19日

CRF：理解center的对火警分配的程式，调试代码。<br>
XJ：调试PFdetector代码<br>
GYJ：调试自动测试脚本<br>
CGY:修改PathPlanning代码<br>

2020年2月20日

GYQ:运行CSU代码分析智能体行为，理解策略<br>
CRF：理解火警algorithm中pathplanning的智能体移动的思想，调试代码。<br>
YBC：调试代码，跑图，试解决警察两点来回转圈的问题<br>CGY:修改和测试PathPlanning代码<br>

2020年2月21日

CRF：理解火警algorithm中的kmeans聚类思想，调试代码。<br>
CGY:改进PathPlanning代码<br>
XJ：调试PF-detactor代码。<br>
YBC：划了划水。

2020年2月22日

CXX：结合大家群里的问题阅读了相应代码。<br>CRF：调试并丰富了complex中火警center对不同agent的状态情况的处理办法。<br>
CGY:改进pathplanning,写tdp,修复search代码<br>YBC:调试detector，试着理清ActionExtClear该改进的地方<br>

2020年2月23日

CRF：调试algorithm中kmeans代码，查看fb中的bug代码。<br>
CGY:处理代码冲突,写tpd<br>
XJ：调试ExtClear代码<br>

2020年2月24日

CGY：写tdp1<br>CRF：写火警部分的tdp。<br>

2020年2月25日

YBC：写警察tdp,改了ActionExtClear一个空指针bug，调试提交RoadDetector代码。<br>
CGY:还在写tdp<br>CRF：写Clustering模块的tdp。<br>

2020年2月26日

YBC：理清ActionExtClear思路与guideline思路，开始尝试修改这拉跨的清理过程。<br>CRF：配环境。<br>
CXX：审阅tdp，跑地图。
CGY:改tdp,修改at的search<br>
GYQ:审阅部分tdp，学习Graham-Scan算法<br>
XJ:审阅tdp<br>

2020年2月27日

CRF：SampleKmeans添加按照tdp添加isodata算法部分代码。<br>
CGY:看fb代码,debug,考虑通讯<br>

2020年2月28日

XJ：阅读messag部分的源代码，调试pf的mssagemanager，尝试实现警察之间的通信。<br>CRF：调试buildingDetector中的bug。<br>
CGY:A*在大图上太慢,调试代码<br>

2020年2月29日

CRF：优化samplekmeans框架。<br>
CGY:测试A*,修复bug,看buildingDetector<br>

2020年3月1日
CGY:完善fb的通讯,修改cluster之外的灭火策略<br>GYJ：重新测试地图，检验阶段代码成果，观察search策略<br>

2020年3月2日

CXX：调试地图。<br>CRF：修改kmeans中预处理模块。<br>GYJ:阅读AIT的search，观察具体实现细节<br>
CGY:写FireCluster<br>
2020年3月3日：

CRF：调试Kmeans中预处理模块。<br>
GYJ：看AIT，MRL的search，以编写search,添加debug开关常量类<br>
CGY:写FireCluster<br>

2020年3月4日:

CGY:初步完成fireCluster,下一步将完善buildingDetector策略<br>CRF：初步完成Kmeans预处理的时间保证。<br>
GYJ:初步设计search思路<br>

2020年3月5日:

XJ:改写了部分PFClear里面的randomwalk函数<br>
CGY:拆分BuildingDetector策略,看16年代码,打算移植一套worldModel<br>CRF：解决kmeans划分cluster中建筑物重复，开始研究precompute。<br>GYJ:划水<br>

2020年3月6日:

CGY:正在引入CSU_2016的AdvancedModel,开发CSUWorldHelper和CSUBuildingHelper<br>CRF：调试kmeans在merge过程中clusterEntityList无法及时更新。<br>
GYJ:正在编写at的search<br>

2020年3月7日:

CGY:引入2016年代码,写fb的target代码<br>
XJ:调试警察聚集清理障碍的问题<br>

2020年3月8日：

CRF：重写kmeans，测试kmeans运行时的各种空指针的情况。<br>
CGY:写Detector<br>
XJ:写allocator代码，尝试随机分配给警察target以解决聚清理障碍的问题，在kobe图表现良好，SydneyS1则不行

2020年3日9日

CXX：阅读项目最近的修改情况，并调试地图。
