package com.example.engine

import java.util.Locale

/**
 * Standard syllabus knowledge base and catalog resolver for academic textbooks,
 * problem sets, and graduate entrance exam (考研) reference materials.
 * Bridges user inputs (e.g. "王道数据结构 (学到第3章)") to exact canonical books and chapters.
 */
data class AcademicChapter(
    val chapterNumber: Int,
    val title: String,
    val keyPoints: String = "",
    val estimatedMinutes: Int = 45
)

data class BookKnowledgeEntry(
    val canonicalTitle: String,
    val subjectCategory: String,
    val examWeightScore: String,
    val difficultyTier: String,
    val totalChapters: Int,
    val chapters: List<AcademicChapter>,
    val pedagogicalAdvice: String,
    val targetType: String = "BOOK"
)

object AcademicBookCatalog {

    /**
     * Resolves raw or cleaned book title into canonical academic book knowledge.
     */
    fun resolveBook(rawTitle: String): BookKnowledgeEntry {
        val clean = BookLifecycleEngine.cleanBookTitle(rawTitle).trim()
        val lower = clean.lowercase(Locale.ROOT)

        return when {
            // --- 408 计算机考研 ---
            lower.contains("数据结构") -> BookKnowledgeEntry(
                canonicalTitle = "《王道考研·计算机数据结构复习指导》",
                subjectCategory = "计算机408统考专业基础",
                examWeightScore = "408统考分值 45分 (核心拉分项)",
                difficultyTier = "高难必考",
                totalChapters = 8,
                chapters = listOf(
                    AcademicChapter(1, "第1章 绪论与算法时空复杂度分析", "大O表示法、渐进时间复杂度推导、空间复杂度"),
                    AcademicChapter(2, "第2章 线性表", "顺序表增删查、单链表、双向链表、循环链表、静态链表"),
                    AcademicChapter(3, "第3章 栈、队列和数组", "栈的括号匹配、表达式求值、循环队列、特殊矩阵对称压缩存储"),
                    AcademicChapter(4, "第4章 串与模式匹配", "BF暴力匹配算法、KMP模式匹配与next/nextval数组计算"),
                    AcademicChapter(5, "第5章 树与二叉树", "二叉树性质、前中后序与层次遍历、线索二叉树、哈夫曼树、并查集"),
                    AcademicChapter(6, "第6章 图", "邻接矩阵与邻接表、BFS/DFS遍历、Prim/Kruskal最小生成树、Dijkstra/Floyd最短路径、拓扑排序与关键路径"),
                    AcademicChapter(7, "第7章 查找", "顺序与折半查找、二叉排序树BST、平衡二叉树AVL、红黑树特性、B/B+树、散列表哈希冲突解决"),
                    AcademicChapter(8, "第8章 内部排序", "直接插入、折半插入、希尔、冒泡、快速排序、简单选择、堆排序、二路归并、基数排序与稳定性分析")
                ),
                pedagogicalAdvice = "重点攻坚树与二叉树遍历、图的最短路径算法手写，必须在纸上规范写出C/C++核心算法代码并推导时空复杂度。"
            )

            lower.contains("组成原理") || lower.contains("计组") || lower.contains("唐朔飞") -> BookKnowledgeEntry(
                canonicalTitle = "《王道考研·计算机组成原理复习指导》",
                subjectCategory = "计算机408统考专业基础",
                examWeightScore = "408统考分值 45分",
                difficultyTier = "极高难度",
                totalChapters = 7,
                chapters = listOf(
                    AcademicChapter(1, "第1章 计算机系统概述", "计算机软硬件系统结构、性能指标与MIPS/CPI计算"),
                    AcademicChapter(2, "第2章 数据的表示和运算", "定点数补码原码移码运算、溢出判断、IEEE754浮点数格式与运算、ALU"),
                    AcademicChapter(3, "第3章 存储系统", "SRAM与DRAM、ROM、主存容量扩展、双端口与多模块交叉存储、Cache高速缓存与命中率计算、虚拟存储器与页表"),
                    AcademicChapter(4, "第4章 指令系统", "指令寻址方式、CISC与RISC架构、指令格式设计"),
                    AcademicChapter(5, "第5章 中央处理器 (CPU)", "CPU基本结构、指令周期与数据通路、硬布线与微程序控制器、指令流水线冒险与吞吐率"),
                    AcademicChapter(6, "第6章 总线系统", "总线仲裁方式、总线定时与带宽"),
                    AcademicChapter(7, "第7章 输入/输出 (I/O) 系统", "I/O接口编址、程序查询、程序中断处理过程与屏蔽字、DMA传输方式")
                ),
                pedagogicalAdvice = "数据通路大题和Cache-主存-虚存地址映射为必考重灾区，务必通过王道综合大题理清各个寄存器间信号传递时序。"
            )

            lower.contains("操作系统") || (lower.contains("os") && !lower.contains("post")) || lower.contains("汤小丹") -> BookKnowledgeEntry(
                canonicalTitle = "《王道考研·操作系统复习指导》",
                subjectCategory = "计算机408统考专业基础",
                examWeightScore = "408统考分值 35分",
                difficultyTier = "高难重点",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "第1章 计算机操作系统概述", "操作系统的基本特征与运行机制、中断与异常、系统调用"),
                    AcademicChapter(2, "第2章 进程与线程管理", "进程状态转换、PCB、线程实现、经典调度算法、信号量机制解决同步互斥、经典同步问题(PV操作)、死锁预防与银行家算法"),
                    AcademicChapter(3, "第3章 内存管理", "连续分配与非连续分配、基本分页与分段、段页式、虚拟内存请求分页、页面置换算法(FIFO/LRU/Clock)、抖动与工作集"),
                    AcademicChapter(4, "第4章 文件管理", "文件逻辑与物理结构、目录结构、文件共享与保护、磁盘结构与磁盘调度算法(FCFS/SSTF/SCAN)"),
                    AcademicChapter(5, "第5章 输入输出 (I/O) 管理", "I/O软件层次结构、SPOOLing假脱机技术、缓冲区管理")
                ),
                pedagogicalAdvice = "PV操作信号量算法大题是全科目核心命脉，每日坚持手写一道经典同步互斥真题(生产者消费者/读者写者/哲学家就餐)。"
            )

            lower.contains("计算机网络") || lower.contains("计网") || lower.contains("谢希仁") -> BookKnowledgeEntry(
                canonicalTitle = "《王道考研·计算机网络复习指导》",
                subjectCategory = "计算机408统考专业基础",
                examWeightScore = "408统考分值 25分",
                difficultyTier = "细致规范",
                totalChapters = 6,
                chapters = listOf(
                    AcademicChapter(1, "第1章 计算机网络体系结构", "OSI七层模型与TCP/IP体系、协议服务与接口、时延带宽性能指标"),
                    AcademicChapter(2, "第2章 物理层", "通信基础、香农定理与奈奎斯特准则、编码与调制、物理层传输介质"),
                    AcademicChapter(3, "第3章 数据链路层", "组帧、差错控制CRC、停止-等待/后退N帧/选择重传ARQ、CSMA/CD与以太网交换机自学习"),
                    AcademicChapter(4, "第4章 网络层", "IPv4与IPv6数据报格式、子网划分CIDR与NAT、ARP/ICMP/DHCP、路由算法与协议(RIP/OSPF/BGP)、IP组播"),
                    AcademicChapter(5, "第5章 传输层", "UDP与TCP协议、TCP三次握手与四次挥手、可靠传输、滑动窗口流量控制、拥塞控制(慢开始/拥塞避免/快重传/快恢复)"),
                    AcademicChapter(6, "第6章 应用层", "DNS域名解析、FTP、电子邮件SMTP/POP3、万维网HTTP协议与状态码")
                ),
                pedagogicalAdvice = "TCP拥塞控制状态机转换与子网划分CIDR路由聚合是每年必考大题，需要构建端到端分组传输时序全景图。"
            )

            // --- 考研数学 (武忠祥 / 张宇 / 李永乐) ---
            lower.contains("高数") || (lower.contains("数学") && !lower.contains("线代") && !lower.contains("概率")) || lower.contains("武忠祥") || lower.contains("张宇") || lower.contains("汤家凤") -> BookKnowledgeEntry(
                canonicalTitle = if (lower.contains("张宇")) "《张宇考研高等数学30讲/18讲》" else "《武忠祥考研高等数学核心辅导讲义》",
                subjectCategory = "考研数学 (数一/数二/数三)",
                examWeightScore = "考研数学分值 150分 (决定性骨架科目)",
                difficultyTier = "极高难度",
                totalChapters = 10,
                chapters = listOf(
                    AcademicChapter(1, "第1讲 函数、极限与连续性", "七种未定式计算、泰勒展开式核心公式、极限存在准则、间断点分类"),
                    AcademicChapter(2, "第2讲 一元函数微分学概念与计算", "导数与微分定义、高阶导数莱布尼茨公式、参数方程与反函数求导"),
                    AcademicChapter(3, "第3讲 一元函数微分学应用与中值定理", "罗尔/拉格朗日/柯西中值定理构造辅助函数、不等式证明、单调极值凹凸拐点与渐近线"),
                    AcademicChapter(4, "第4讲 不定积分计算方法与技巧", "第一/第二换元积分法、分部积分法表格法、有理函数积分"),
                    AcademicChapter(5, "第5讲 定积分与反常积分", "定积分性质与几何意义、变上限积分函数求导、反常积分敛散性审敛法"),
                    AcademicChapter(6, "第6讲 定积分的几何与物理应用", "平面图形面积、旋转体体积(切片法/柱壳法)、弧长、做功水压力"),
                    AcademicChapter(7, "第7讲 微分方程", "一阶可分离变量与齐次方程、一阶线性方程、可降阶方程、二阶常系数齐次/非齐次线性方程"),
                    AcademicChapter(8, "第8讲 多元函数微分学", "多元极限连续偏导全微分关系、链式求导法则、隐函数求导、极值与拉格朗日乘数法"),
                    AcademicChapter(9, "第9讲 二重积分", "直角坐标与极坐标二重积分计算、积分次序交换、对称性与奇偶性化简"),
                    AcademicChapter(10, "第10讲 无穷级数与空间解析几何", "常数项级数敛散性审敛法、幂级数收敛域与和函数、傅里叶级数/多元积分学(数一专项)")
                ),
                pedagogicalAdvice = "以武忠祥高数讲义为骨架构建概念网络，配合660题强化概念辨析，做题必须在草稿纸上规范书写算到底，杜绝'眼高手低'。"
            )

            lower.contains("线代") || lower.contains("线性代数") || lower.contains("李永乐") -> BookKnowledgeEntry(
                canonicalTitle = "《李永乐考研数学线性代数辅导讲义》",
                subjectCategory = "考研数学 (线性代数专项)",
                examWeightScore = "考研数学核心模块 (30-35分)",
                difficultyTier = "逻辑缜密",
                totalChapters = 6,
                chapters = listOf(
                    AcademicChapter(1, "第1章 行列式", "行列式定义、展开定理与化简性质、范德蒙行列式"),
                    AcademicChapter(2, "第2章 矩阵", "矩阵乘法、伴随矩阵性质、逆矩阵求法、初等变换与初等矩阵、矩阵的秩"),
                    AcademicChapter(3, "第3章 向量与向量空间", "线性相关与线性无关判别、极大线性无关组、基与维数、施密特正交化"),
                    AcademicChapter(4, "第4章 线性方程组", "齐次方程组基础解系与通解、非齐次方程组解的结构与公共解同解问题"),
                    AcademicChapter(5, "第5章 特征值与特征向量", "特征多项式性质、相似矩阵与相似对角化判定、实对称矩阵正交相似对角化"),
                    AcademicChapter(6, "第6章 二次型", "二次型矩阵表示、正交变换与配方法化标准形、惯性定理与正定二次型判定")
                ),
                pedagogicalAdvice = "线代知识网状交织，掌握李永乐'矩阵的秩'主线贯穿六大章节，务必熟记实对称矩阵正交相似对角化大题解题规范。"
            )

            lower.contains("概率") || lower.contains("数理统计") || lower.contains("余丙森") || lower.contains("王式安") -> BookKnowledgeEntry(
                canonicalTitle = "《余丙森/王式安考研数学概率论与数理统计辅导讲义》",
                subjectCategory = "考研数学 (数一/数三专项)",
                examWeightScore = "考研数学核心模块 (30-35分)",
                difficultyTier = "模型标准化",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "第1章 随机事件与概率", "条件概率、全概率公式、贝叶斯公式与事件独立性"),
                    AcademicChapter(2, "第2章 一维随机变量及其分布", "分布函数性质、常见离散型(0-1/二项/泊松)与连续型(均匀/指数/正态)分布"),
                    AcademicChapter(3, "第3章 二维随机变量及其分布", "联合分布函数、边缘分布与条件分布、二维正态分布、随机变量函数分布求法(卷积公式)"),
                    AcademicChapter(4, "第4章 随机变量的数字特征", "数学期望与方差性质、协方差与相关系数、切比雪夫不等式与大数定律"),
                    AcademicChapter(5, "第5章 数理统计与参数估计", "三大抽样分布(卡方/t/F分布)、点估计(矩估计法与最大似然估计法MLE)、无偏性与有效性")
                ),
                pedagogicalAdvice = "最大似然估计和二维连续型随机变量函数分布是每年必考大题，掌握标准化模板步骤即能拿满分。"
            )

            lower.contains("660") -> BookKnowledgeEntry(
                canonicalTitle = "《武忠祥/李永乐考研数学题海精选660题》",
                subjectCategory = "考研数学核心真题题库",
                examWeightScore = "精准突破选择题与填空题陷阱",
                difficultyTier = "概念精微",
                totalChapters = 6,
                chapters = listOf(
                    AcademicChapter(1, "高数选择题组精练", "极限保号性、导数定义与多元极值概念辨析"),
                    AcademicChapter(2, "高数填空题组精练", "定积分反常积分计算、微分方程求解与级数求和"),
                    AcademicChapter(3, "线代选择题组精练", "矩阵的秩、向量相关性、特征值判定与相似变换"),
                    AcademicChapter(4, "线代填空题组精练", "行列式计算、正交变换与正定矩阵参数确定"),
                    AcademicChapter(5, "概率选择题组精练", "事件独立性、不相关与独立区别、统计量分布判定"),
                    AcademicChapter(6, "概率填空题组精练", "期望方差协方差计算、矩估计与似然方程建立")
                ),
                pedagogicalAdvice = "660题属于硬核概念刷题集，错题率高是正常现象，重点标记选错题目的命题陷阱并回归讲义定理。"
            )

            // --- 汉硕 / 汉语国际教育 / 中国语言文学 ---
            lower.contains("现代汉语") || lower.contains("现汉") || lower.contains("邢福义") || lower.contains("黄伯荣") -> BookKnowledgeEntry(
                canonicalTitle = "《邢福义·现代汉语》/《黄伯荣廖序东·现代汉语》",
                subjectCategory = "汉硕/中国语言文学专业课",
                examWeightScore = "专业课核心分值 (75-150分)",
                difficultyTier = "严谨规范",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "第1章 语音", "声母发音部位方法、韵母配合规律、声调五度标记法、音变(轻声儿化变调)、国际音标IPA标音"),
                    AcademicChapter(2, "第2章 文字", "汉字的造字法(象形指事会意形声)、汉字形体演变、规范汉字与易错笔顺"),
                    AcademicChapter(3, "第3章 词汇", "词素与词的构造、词义性质与分析、同义词反义词辨析方法、熟语与成语误用"),
                    AcademicChapter(4, "第4章 语法", "词类划分标准、短语结构类型、单句句法层次分析法(画树形图/框式图)、句子成分分析、歧义句成因与分化、复句类型辨析"),
                    AcademicChapter(5, "第5章 修辞", "常见修辞格(比喻比拟借代对偶排比借喻)辨析与语用原则")
                ),
                pedagogicalAdvice = "层次分析法画树形图和国际音标标音是每年必考大题，每天坚持分析3个复杂单句短语并核对标准切分。"
            )

            lower.contains("古代汉语") || lower.contains("古汉") || lower.contains("郭锡良") || lower.contains("王力") -> BookKnowledgeEntry(
                canonicalTitle = "《郭锡良·古代汉语》(上下册) / 《王力·古代汉语》",
                subjectCategory = "汉硕/古代文学专业课",
                examWeightScore = "专业课核心分值 (50-100分)",
                difficultyTier = "深厚积淀",
                totalChapters = 7,
                chapters = listOf(
                    AcademicChapter(1, "通论一: 汉字形体构造与六书", "六书造字法、古今字、异体字、繁简字与通假字辨析"),
                    AcademicChapter(2, "通论二: 古代汉语词义分析", "古今词义异同(扩大/缩小/转移)、单音词与复音词、词的本义与引申义"),
                    AcademicChapter(3, "通论三: 词类活用", "名词作状语、名词动用、使动用法、意动用法规律"),
                    AcademicChapter(4, "通论四: 特殊句式", "判断句基本格式、被动句标记、宾语前置条件(否定句代词/疑问代词/之是提宾)、双宾语"),
                    AcademicChapter(5, "通论五: 古代汉语常用虚词", "之、其、者、所、以、于、而、则、乃、焉等虚词语法功能与用法区分"),
                    AcademicChapter(6, "通论六: 古书标点与文选精读", "先秦两汉散文名篇精读翻译与断句原则"),
                    AcademicChapter(7, "通论七: 音韵常识与中古音", "平水韵、反切注音法、上古音与中古音声韵调常识")
                ),
                pedagogicalAdvice = "坚持每日早读一段先秦文选，强化无标点古文断句手感，掌握名词使动意动活用与特殊句式语法标记。"
            )

            lower.contains("文化要略") || lower.contains("程裕祯") -> BookKnowledgeEntry(
                canonicalTitle = "《程裕祯·中国文化要略》(第五版)",
                subjectCategory = "汉硕专业基础综合 (文化部分)",
                examWeightScore = "汉硕分值 40-60分",
                difficultyTier = "广博识记",
                totalChapters = 6,
                chapters = listOf(
                    AcademicChapter(1, "第1单元 地理、历史常识与中华民族", "中国地理环境、行政区划沿革、上古神话与历史纪年"),
                    AcademicChapter(2, "第2单元 诸子百家与儒释道思想", "先秦诸子(儒墨道法)、两汉经学、魏晋玄学、宋明理学与心学、中国佛教道教发展史"),
                    AcademicChapter(3, "第3单元 典籍、科举制度与官职沿革", "四库全书分类、经史子集名著、古代官制三省六部、科举制度(院试乡试会试殿试)"),
                    AcademicChapter(4, "第4单元 古代文学与艺术", "诗经楚辞、唐诗宋词元曲明清小说流派、书法绘画艺术、中国古代建筑与园林"),
                    AcademicChapter(5, "第5单元 古代科技、民俗风尚与传统节日", "四大发明、农林医学、传统节日习俗、天干地支二十四节气"),
                    AcademicChapter(6, "第6单元 中外文化交流史", "丝绸之路、佛教东传、玄奘西行、郑和下西洋、明清传教士与西学东渐")
                ),
                pedagogicalAdvice = "文化要略知识点琐碎但重现率极高，建议利用艾宾浩斯闪卡进行主动回忆记忆，构建时间轴脉络。"
            )

            lower.contains("外国文化史") || lower.contains("孟昭毅") -> BookKnowledgeEntry(
                canonicalTitle = "《孟昭毅·外国文化史》(第二版)",
                subjectCategory = "汉硕专业基础综合",
                examWeightScore = "汉硕分值 20-30分",
                difficultyTier = "脉络梳理",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "第1篇 古代两河流域、古埃及与古印度文明", "美索不达米亚、汉谟拉比法典、种姓制度与古印度佛教兴起"),
                    AcademicChapter(2, "第2篇 古希腊与古罗马文明", "荷马史诗、古希腊哲学三贤、雅典民主、罗马法体系与基督教兴起"),
                    AcademicChapter(3, "第3篇 中世纪欧洲与伊斯兰文化", "中世纪基督教神学、骑士文学、阿拉伯帝国与伊斯兰文化成就"),
                    AcademicChapter(4, "第4篇 文艺复兴、宗教改革与启蒙运动", "意大利文艺复兴三杰、马丁路德宗教改革、法国启蒙运动思想家思想"),
                    AcademicChapter(5, "第5篇 现代欧美文化与思潮", "浪漫主义、现实主义文学、现代主义思潮与全球化文化交融")
                ),
                pedagogicalAdvice = "重点理清古希腊罗马到文艺复兴、启蒙运动的人文主义思想演进主线，结合大题简答题模板作答。"
            )

            lower.contains("对外汉语") || lower.contains("教学引论") || lower.contains("刘珣") -> BookKnowledgeEntry(
                canonicalTitle = "《刘珣·对外汉语教育学引论》",
                subjectCategory = "汉硕专业核心课",
                examWeightScore = "汉硕分值 50-70分 (含案例分析大题)",
                difficultyTier = "理论综合",
                totalChapters = 6,
                chapters = listOf(
                    AcademicChapter(1, "第1章 对外汉语教育学科性质与发展历史", "对外汉语教育的学科定位、任务与中国对外汉语教学发展沿革"),
                    AcademicChapter(2, "第2章 语言习得理论基础", "行为主义、心灵主义先天论、认知论、相互作用论与文化适应假说"),
                    AcademicChapter(3, "第3章 第二语言习得研究 (SLA)", "对比分析假说、偏误分析(偏误类型成因)、中介语理论、克拉申输入假说五大原则"),
                    AcademicChapter(4, "第4章 第二语言教学法流派", "语法翻译法、直接法、听说法、视听法、交际法、任务型教学法优缺点与教学设计"),
                    AcademicChapter(5, "第5章 对外汉语教学过程与课堂教学", "教学大纲、教材编写与选用、备课教案设计、课堂教学环节与提问技巧"),
                    AcademicChapter(6, "第6章 跨文化交际与案例分析", "文化休克四阶段、跨文化冲突成因、汉硕跨文化案例分析大题答题范式与万能模板")
                ),
                pedagogicalAdvice = "克拉申五大假说与中介语偏误分析是每年案例分析大题的理论武器，务必熟背二语习得理论术语。"
            )

            // --- 考研政治 ---
            lower.contains("政治") || lower.contains("肖秀荣") || lower.contains("1000题") || lower.contains("精讲精练") || lower.contains("徐涛") || lower.contains("腿姐") -> BookKnowledgeEntry(
                canonicalTitle = if (lower.contains("1000")) "《肖秀荣考研政治命题人1000题》" else "《肖秀荣考研政治知识点精讲精练/徐涛核心考案》",
                subjectCategory = "思想政治理论统考 (满分100分)",
                examWeightScore = "政治统考 100分",
                difficultyTier = "强化背诵",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "模块一 马克思主义基本原理 (马原)", "唯物论(物质与意识)、辩证法(联系发展与三大规律)、认识论(实践认识真理)、唯物史观、马克思主义政治经济学(劳动价值论/剩余价值论)"),
                    AcademicChapter(2, "模块二 毛泽东思想和中国特色社会主义理论体系概论 (毛中特)", "新民主主义革命理论、社会主义改造理论、中国特色社会主义理论体系构建"),
                    AcademicChapter(3, "模块三 习近平新时代中国特色社会主义思想概论 (习思想)", "高质量发展、全过程人民民主、中国式现代化、全面从严治党、新发展格局"),
                    AcademicChapter(4, "模块四 中国近现代史纲要 (史纲)", "旧民主主义革命探索(洋务/戊戌/辛亥)、开天辟地中国共产党诞生、抗日战争与解放战争、新中国建立与探索"),
                    AcademicChapter(5, "模块五 思想道德与法治 (思法) 及当代时政", "人生观、理想信念、社会主义核心价值观、道德修养与中国特色社会主义法治体系")
                ),
                pedagogicalAdvice = "前期重点突破马原哲学唯物辩证法逻辑，二刷肖1000客观题错题，11月后结合背诵手册冲刺主观大题默写。"
            )

            // --- 考研英语 ---
            lower.contains("英语") || lower.contains("词汇") || lower.contains("黄皮书") || lower.contains("阅读") || lower.contains("长难句") || lower.contains("恋练有词") || lower.contains("红宝书") -> BookKnowledgeEntry(
                canonicalTitle = "《考研英语历年真题超精解·黄皮书》/《考研英语核心词汇与长难句》",
                subjectCategory = "考研英语统考 (英一/英二)",
                examWeightScore = "考研英语 100分 (关键单科过线项)",
                difficultyTier = "长线积累",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "第1阶段 核心大纲词汇主动记忆与长难句拆解", "利用高频词根词缀背词，五大基本句型、非谓语动词与从句嵌套剖析"),
                    AcademicChapter(2, "第2阶段 历年真题阅读精读分析 (2010-2016)", "逐篇手译长难句、剖析命题人设坑套路(无中生有/偷换概念/正反倒置)"),
                    AcademicChapter(3, "第3阶段 历年真题阅读强化突破 (2017-2022)", "阅读六大题型(主旨题/细节题/推理题/态度题/词汇题/例证题)解题技巧"),
                    AcademicChapter(4, "第4阶段 新题型与翻译专项提分演练", "英一七选五/排序题/小标题，英二信息匹配与全文翻译技巧"),
                    AcademicChapter(5, "第5阶段 小作文应用文与大作文图画/图表模板构建", "书信通知告示模板、图画描述段/论证段/建议段金句语料库构建与全真模考")
                ),
                pedagogicalAdvice = "近10年真题阅读必须按考场限时要求完成，每篇阅读做完后进行无死角精读，提炼生词并摘抄长难句。"
            )

            // --- 203 日语 ---
            lower.contains("日语") || lower.contains("203") || lower.contains("标日") -> BookKnowledgeEntry(
                canonicalTitle = "《203考研日语高分专项突破》/《新版标准日本语》",
                subjectCategory = "全国统考203日语 (满分100分)",
                examWeightScore = "统考科目 100分",
                difficultyTier = "语法精准",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "第1部分 基础语法与动词活用变形", "动词各类活用(未然/连用/终止/连体/假定/命令)、使役态、被动态与敬语体系"),
                    AcademicChapter(2, "第2部分 N2/N1核心句型与助词精辨", "常见助词对比、推量表达、授受动词关系辨析"),
                    AcademicChapter(3, "第3部分 考研日语长篇阅读限时攻坚", "说明文论述文主旨把握、接续词逻辑关系定位解题"),
                    AcademicChapter(4, "第4部分 日汉互译翻译技能规范", "长句结构切分、日汉表达习惯转化"),
                    AcademicChapter(5, "第5部分 考研日语命题作文实战", "原稿纸书写规范、常用论述句式与模板框架")
                ),
                pedagogicalAdvice = "牢固掌握敬语系统与使役被动态，每日保持两篇长篇阅读精读训练，考前规范演练原稿纸日文写作。"
            )

            // --- 西医综合 (306) ---
            lower.contains("西医") || lower.contains("生理") || lower.contains("生化") || lower.contains("病理") || lower.contains("内科") || lower.contains("外科") || lower.contains("贺银成") -> BookKnowledgeEntry(
                canonicalTitle = "《贺银成考研西医临床医学综合能力辅导讲义》",
                subjectCategory = "西医综合统考 (满分300分)",
                examWeightScore = "专业课 300分 (最高分值权重)",
                difficultyTier = "极高密度",
                totalChapters = 5,
                chapters = listOf(
                    AcademicChapter(1, "生理学核心机制", "细胞基本功能、血液循环、呼吸系统、消化吸收、神经系统调节与内分泌"),
                    AcademicChapter(2, "生物化学代谢网络", "蛋白质与核酸结构功能、酶动力学、三大物质代谢(糖/脂/氨基酸)、遗传信息传递"),
                    AcademicChapter(3, "病理学形态病变", "细胞组织的适应与损伤、炎症渗出机制、肿瘤分类与生物学行为、心血管呼吸消化系统疾病"),
                    AcademicChapter(4, "内科学临床诊断", "心血管疾病(心衰/心律失常)、呼吸系统(COPD/哮喘)、消化系统(溃疡/肝硬化)、肾脏病与内分泌疾病诊断治疗"),
                    AcademicChapter(5, "外科学与临床基本技能", "无菌术、水与电解质酸碱平衡、休克抢救、创伤愈合、普通外科疾病手术指征")
                ),
                pedagogicalAdvice = "生理学重机制推理，生化学重代谢通路横向联系，内科学紧扣诊断标准与首选药物，多做真题多选题防丢分。"
            )

            // --- 通用兜底 ---
            else -> BookKnowledgeEntry(
                canonicalTitle = "《$clean》",
                subjectCategory = "专业研读与备考教材",
                examWeightScore = "核心考试专业考核范围",
                difficultyTier = "核心主干",
                totalChapters = 10,
                chapters = (1..10).map { ch ->
                    AcademicChapter(
                        chapterNumber = ch,
                        title = "第 $ch 章 核心理论与考点梳理",
                        keyPoints = "重点概念、定理公式推导与课后典型题训练",
                        estimatedMinutes = 45
                    )
                },
                pedagogicalAdvice = "建议采用'考点速览 + 真题倒逼定位'策略，跳过过时繁复内容，聚焦历年常考高频题型。"
            )
        }
    }

    /**
     * Finds the matching chapter title for a given book and chapter number.
     */
    fun getChapterTitle(rawBookTitle: String, chapterNum: Int): String {
        val entry = resolveBook(rawBookTitle)
        val matchedCh = entry.chapters.firstOrNull { it.chapterNumber == chapterNum }
        return matchedCh?.title ?: "第 $chapterNum 章 核心考点演练"
    }

    /**
     * Builds an AI Prompt Knowledge Section explaining canonical book context and syllabus,
     * ensuring Gemini or DeepSeek never hallucinates or mistakes the book!
     */
    fun buildAiKnowledgePromptSection(
        books: List<String>,
        progressProfiles: List<com.example.model.BookProgressProfile>
    ): String {
        if (books.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("\n=== OFFICIAL ACADEMIC BOOKS & SYLLABUS KNOWLEDGE BASE (STRICTLY RESPECT) ===\n")
        sb.append("The AI must align tasks with the canonical syllabus chapters and student's current progress:\n")

        books.forEachIndexed { index, rawBook ->
            val clean = BookLifecycleEngine.cleanBookTitle(rawBook)
            val entry = resolveBook(clean)
            val prof = progressProfiles.firstOrNull { it.bookTitle == clean }

            val startCh = prof?.startChapter ?: 1
            val isCompleted = prof?.isCompleted == true
            val targetDays = prof?.targetFinishDays

            sb.append("\n[BOOK #${index + 1}] ${entry.canonicalTitle}\n")
            sb.append("  - Subject: ${entry.subjectCategory} | Weight: ${entry.examWeightScore}\n")
            sb.append("  - Difficulty: ${entry.difficultyTier} | Advice: ${entry.pedagogicalAdvice}\n")

            if (isCompleted) {
                sb.append("  - STUDENT STATUS: ALREADY COMPLETED & ARCHIVED (DO NOT schedule new tasks for this book!)\n")
            } else {
                sb.append("  - STUDENT PROGRESS: Currently at Chapter $startCh")
                if (startCh > 1) {
                    sb.append(" (Student has already finished Chapters 1 to ${startCh - 1}. You MUST START daily scheduling from Chapter $startCh! Do NOT repeat finished chapters!)")
                }
                if (targetDays != null) {
                    sb.append(" | Goal: Complete within first $targetDays days!")
                }
                sb.append("\n  - Canonical Chapter Directory:\n")
                entry.chapters.forEach { ch ->
                    val statusMarker = when {
                        ch.chapterNumber < startCh -> "[ALREADY FINISHED - SKIP]"
                        ch.chapterNumber == startCh -> "[START HERE ON DAY 1]"
                        else -> "[UPCOMING]"
                    }
                    sb.append("    * ${ch.title} $statusMarker: ${ch.keyPoints}\n")
                }
            }
        }
        sb.append("=========================================================================\n")
        return sb.toString()
    }

    /**
     * Automatically retrieves canonical syllabus ground truth for a given book title.
     * Can be auto-filled or confirmed by the user.
     */
    fun getAutoGeneratedCatalogGroundTruth(rawTitle: String): String {
        val entry = resolveBook(rawTitle)
        val sb = StringBuilder()
        sb.append("【权威事实基准 (Ground Truth)】${entry.canonicalTitle}\n")
        sb.append("• 学科归属：${entry.subjectCategory} (${entry.examWeightScore})\n")
        sb.append("• 官方总章数/范围：共 ${entry.totalChapters} 章/讲\n")
        sb.append("• 权威目录大纲：\n")
        entry.chapters.forEach { ch ->
            sb.append("  - ${ch.title}：${ch.keyPoints}\n")
        }
        sb.append("• 权威备考建议：${entry.pedagogicalAdvice}")
        return sb.toString().trim()
    }
}
