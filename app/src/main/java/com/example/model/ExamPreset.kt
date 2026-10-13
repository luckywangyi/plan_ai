package com.example.model

import java.util.Locale

data class ExamPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String = "考研科目",
    val tag: String = "核心",
    val targetDays: Int = 60,
    val defaultDailyHours: Float = 2.0f,
    val rawInputs: StudyRawInputs
)

object ExamPresets {
    // 7 Core Subject Presets requested by user
    val presets = listOf(
        ExamPreset(
            id = "preset_english_one",
            title = "考研英一",
            subtitle = "黄皮书历年真题超精解 + 大纲高频核心词汇 + 长难句与写作突破",
            category = "公共课",
            tag = "100分",
            targetDays = 60,
            defaultDailyHours = 1.5f,
            rawInputs = StudyRawInputs(
                examTitle = "考研英语一 (全国统考)",
                targetDate = "",
                dailyHoursAvailable = 1.5f,
                books = listOf(
                    "[考研英一] 考研英语大纲高频核心词汇便携手册 (艾宾浩斯滚动)",
                    "[考研英一] 考研英语长难句核心语法突破 (拆分与组合剖析)",
                    "[考研英一] 考研英语黄皮书真题精读与命题人逻辑精解"
                ),
                problemSets = listOf(
                    "[考研英一] 英语一历年真题阅读理解 80 篇超精读精练",
                    "[考研英一] 新题型、完形填空与英译汉专项题库",
                    "[考研英一] 考研英语高分大小作文模板构建与实战仿写"
                ),
                videoLectures = listOf(
                    "[考研英一] 真题阅读逻辑出题陷阱剖析课 (14小时)",
                    "[考研英一] 英语长难句结构拆分与句法重构课 (8小时)"
                ),
                customNotes = "英语战略：晨起黄金时段进行词汇与长难句主动唤醒；真题阅读必须逐句精剖，严格记录生词并列入艾宾浩斯复盘矩阵；作文注重逻辑展开与句式多样性。"
            )
        ),
        ExamPreset(
            id = "preset_cs_408",
            title = "408",
            subtitle = "王道四本书 (数据结构 + 计组 + 操作系统 + 计算机网络) + 课后精选与真题",
            category = "工科专业课",
            tag = "150分",
            targetDays = 60,
            defaultDailyHours = 2.5f,
            rawInputs = StudyRawInputs(
                examTitle = "全国考研 408 计算机专业基础综合",
                targetDate = "",
                dailyHoursAvailable = 2.5f,
                books = listOf(
                    "[408] 王道《数据结构复习指导》",
                    "[408] 王道《计算机组成原理复习指导》",
                    "[408] 王道《操作系统复习指导》",
                    "[408] 王道《计算机网络复习指导》"
                ),
                problemSets = listOf(
                    "[408] 王道四科单科课后核心习题与综合大题精练",
                    "[408] 408 历年真题全真演练分类超精解 (近15年真题卷)",
                    "[408] 算法大题纸上手写代码与时空复杂度专项演练 (150题)"
                ),
                videoLectures = listOf(
                    "[408] 王道考研数据结构核心算法与树图高阶专题 (16小时)",
                    "[408] 王道考研计算机组成原理指令流水线与微架构串讲 (14小时)"
                ),
                customNotes = "408 战略：紧扣王道四本书复习指导，下午高精力期攻坚数据结构算法手写与计组数据通路；代码题务必纸上手写并推演时空复杂度；操作系统抓牢进程调度与虚存置换，计网理清协议分层。"
            )
        ),
        ExamPreset(
            id = "preset_math_one",
            title = "考研数一",
            subtitle = "武忠祥高数辅导讲义 + 概率论基础讲义 + 武忠祥基础过关660题",
            category = "公共课",
            tag = "150分",
            targetDays = 60,
            defaultDailyHours = 2.5f,
            rawInputs = StudyRawInputs(
                examTitle = "全国考研 数学一 (全科强化)",
                targetDate = "",
                dailyHoursAvailable = 2.5f,
                books = listOf(
                    "[考研数一] 武忠祥《高等数学基础/强化辅导讲义》",
                    "[考研数一] 《概率论与数理统计基础讲义》",
                    "[考研数一] 线性代数辅导讲义 (考研数一核心考点精解)"
                ),
                problemSets = listOf(
                    "[考研数一] 武忠祥/李永乐《考研数学基础过关660题》(概念诊断与精练)",
                    "[考研数一] 数学一历年真题全精解析分类汇编 (2010-2025)",
                    "[考研数一] 武忠祥十七堂高数重难点专题题型深度精练"
                ),
                videoLectures = listOf(
                    "[考研数一] 武忠祥高等数学极限、多元微积分与微分方程重难点强化 (26小时)",
                    "[考研数一] 概率论基础讲义二维随机变量与数字特征专题 (12小时)"
                ),
                customNotes = "数一战略：上午黄金时段集中计算；以武忠祥高数讲义为核心建立完整体系，主攻武忠祥660题攻破概念易错点与计算精度；草稿纸上务必规范算到底；概率论基础讲义强化分布函数与大数定律。"
            )
        ),
        ExamPreset(
            id = "preset_politics",
            title = "政治",
            subtitle = "肖秀荣1000题 + 知识点精讲精练/徐涛核心考案 + 肖四肖八冲刺",
            category = "公共课",
            tag = "100分",
            targetDays = 45,
            defaultDailyHours = 1.0f,
            rawInputs = StudyRawInputs(
                examTitle = "全国考研 思想政治理论",
                targetDate = "",
                dailyHoursAvailable = 1.0f,
                books = listOf(
                    "[政治] 肖秀荣《思想政治理论知识点精讲精练》/ 徐涛《核心考案》",
                    "[政治] 考研政治核心考点掌中宝与背诵手册"
                ),
                problemSets = listOf(
                    "[政治] 肖秀荣《考研政治1000题》(核心题库选择题演练与错题二刷)",
                    "[政治] 历年真题分类演练与选择题命题陷阱剖析",
                    "[政治] 肖秀荣终极预测四套卷与八套卷 (肖四肖八分析题冲刺)"
                ),
                videoLectures = listOf(
                    "[政治] 马原唯物辩证法与政经逻辑框架精讲 (15小时)",
                    "[政治] 近现代史纲要脉络与毛中特新考点串讲 (10小时)"
                ),
                customNotes = "政治战略：紧扣肖1000题夯实客观题选择题基础，前期重点攻关马原与史纲；利用晚间碎片时间反复二刷错题；考前一个月集中默写背诵肖四肖八分析题预测。"
            )
        ),
        ExamPreset(
            id = "preset_japanese_203",
            title = "日语",
            subtitle = "《考研日语高分专项突破·阅读分册》+ 新版标日中级 + 核心语法与真题",
            category = "公共课/外语",
            tag = "100分",
            targetDays = 60,
            defaultDailyHours = 1.5f,
            rawInputs = StudyRawInputs(
                examTitle = "全国考研 203 日语统考",
                targetDate = "",
                dailyHoursAvailable = 1.5f,
                books = listOf(
                    "[日语] 《考研日语高分专项突破·阅读分册》",
                    "[日语] 新版中日交流标准日本语 (中级上下册)",
                    "[日语] 考研日语必备高频核心词汇与语法句型手册"
                ),
                problemSets = listOf(
                    "[日语] 《考研日语高分专项突破·阅读分册》深度精读精练 (80篇高分攻坚)",
                    "[日语] 考研日语203历年真题超精解与全真演练卷 (近15年真题)",
                    "[日语] 日语作文范文精读与汉日互译专项训练集"
                ),
                videoLectures = listOf(
                    "[日语] 考研日语阅读出题套路破局与长难句精读剖析 (14小时)",
                    "[日语] 考研日语敬语、助词与复合动词专项通讲 (12小时)"
                ),
                customNotes = "日语战略：紧扣《考研日语高分专项突破·阅读分册》，每日坚持限时阅读与长难句剖析；系统梳理动词活用变形、易混助词辨析与敬语体系；强化汉日互译准确度与句意抓取速度。"
            )
        ),
        ExamPreset(
            id = "preset_modern_chinese",
            title = "现代汉语",
            subtitle = "邢福义版《现代汉语》(第三版) + 《现代汉语考研与辅导习题集》",
            category = "文科专业课",
            tag = "150分",
            targetDays = 60,
            defaultDailyHours = 2.0f,
            rawInputs = StudyRawInputs(
                examTitle = "汉语言文字学/汉硕 现代汉语专业课",
                targetDate = "",
                dailyHoursAvailable = 2.0f,
                books = listOf(
                    "[现代汉语] 邢福义版《现代汉语》(第三版 上下册)",
                    "[现代汉语] 现代汉语通论与教学语法核心要点 (陆俭明/邵敬敏)"
                ),
                problemSets = listOf(
                    "[现代汉语] 《现代汉语考研与辅导习题集》(核心题型强化精练)",
                    "[现代汉语] 语音拼音国际音标、汉字造字法与词汇辨析专项 (500题)",
                    "[现代汉语] 语法歧义句分析与句法成分层次分析法画树形图大题集"
                ),
                videoLectures = listOf(
                    "[现代汉语] 邢福义现代汉语语法体系与短语单复句层次分析法攻坚 (14小时)",
                    "[现代汉语] 现代汉语语音音变规则与国际音标应用课 (8小时)"
                ),
                customNotes = "现汉战略：以邢福义第三版现代汉语为标准，严格记忆概念定义，关键词字斟句酌；熟练掌握国际音标、声韵调配合规律；结合《现代汉语考研与辅导习题集》，层次分析法必须严格规范画出句法树形图。"
            )
        ),
        ExamPreset(
            id = "preset_ancient_chinese",
            title = "古代汉语",
            subtitle = "郭锡良版《古代汉语》(上下两册) + 经典文选精读 + 断句翻译实战",
            category = "文科专业课",
            tag = "150分",
            targetDays = 60,
            defaultDailyHours = 2.0f,
            rawInputs = StudyRawInputs(
                examTitle = "汉语言文字学/汉硕 古代汉语专业课",
                targetDate = "",
                dailyHoursAvailable = 2.0f,
                books = listOf(
                    "[古代汉语] 郭锡良版《古代汉语》(上下两册)",
                    "[古代汉语] 古代汉语通论与古代文化常识要点解析",
                    "[古代汉语] 古代汉语常用字与文言虚词通释手册"
                ),
                problemSets = listOf(
                    "[古代汉语] 郭锡良古代汉语经典文选精读、注释与词类活用训练 (上下两册名篇)",
                    "[古代汉语] 名校古代汉语考研真题无标点文言文断句与翻译集",
                    "[古代汉语] 古汉语通假异体字、古今字与音韵训诂专项试题"
                ),
                videoLectures = listOf(
                    "[古代汉语] 郭锡良古代汉语通论、六书造字与文言句式精讲 (15小时)",
                    "[古代汉语] 文言文断句标点技巧与经典文选精读课 (12小时)"
                ),
                customNotes = "古汉战略：立足郭锡良版古代汉语上下两册，坚持每日精读并出声诵读经典名篇；牢固掌握词类活用与常见文言虚词用法；强化无标点断句与直译能力。"
            )
        ),
        ExamPreset(
            id = "preset_international_chinese",
            title = "汉语国际教育",
            subtitle = "程裕祯《中国文化要略》(第五版) + 刘珣《引论》+ 孟昭毅《外国文化史》(第二版) + 冲关宝典",
            category = "文科专业课",
            tag = "150分",
            targetDays = 60,
            defaultDailyHours = 2.0f,
            rawInputs = StudyRawInputs(
                examTitle = "汉语国际教育硕士 (汉硕354/445专业综合)",
                targetDate = "",
                dailyHoursAvailable = 2.0f,
                books = listOf(
                    "[汉语国际教育] 程裕祯版《中国文化要略》(第五版)",
                    "[汉语国际教育] 刘珣《对外汉语教学引论》",
                    "[汉语国际教育] 孟昭毅版《外国文化史》(第二版)"
                ),
                problemSets = listOf(
                    "[汉语国际教育] 《汉语国际教育冲关宝典》(考点全梳理与高频考题演练)",
                    "[汉语国际教育] 汉硕历年真题中外文化常识与教引大题分类汇编",
                    "[汉语国际教育] 对外汉语教学案例分析与跨文化交际教案编写专项"
                ),
                videoLectures = listOf(
                    "[汉语国际教育] 刘珣对外汉语教学引论教育学心理学与二语习得串讲 (14小时)",
                    "[汉语国际教育] 中国文化要略与外国文化史核心考点脉络速记课 (12小时)"
                ),
                customNotes = "汉硕综合战略：以程裕祯《中国文化要略》(第五版)与孟昭毅《外国文化史》(第二版)为基准梳理中外文化线索；深入理解刘珣《对外汉语教学引论》中的二语习得与教学法流派；利用《汉语国际教育冲关宝典》进行考点全真冲关与大题案例分析规范写作。"
            )
        )
    )

    /**
     * Intelligently merges multiple selected subject presets into a unified, multi-disciplinary StudyRawInputs plan.
     */
    fun mergePresets(selected: List<ExamPreset>): StudyRawInputs {
        if (selected.isEmpty()) {
            return presets.first().rawInputs
        }
        if (selected.size == 1) {
            return selected.first().rawInputs
        }

        val subjectNames = selected.map { it.title }
        val compositeTitle = "全国研究生考试多学科综合备考 (${subjectNames.joinToString(" + ")})"

        // Determine target days (use maximum days among subjects to avoid truncation)
        val maxDays = selected.maxOf { it.targetDays }

        // Determine daily hours available based on subject count
        // 1 subject: default (e.g. 2h)
        // 2 subjects: ~4.0h
        // 3 subjects: ~5.5h
        // 4 subjects: ~7.0h
        // 5+ subjects: ~8.0h - 9.0h
        val calculatedHours = when (selected.size) {
            2 -> 4.5f
            3 -> 6.0f
            4 -> 7.5f
            else -> minOf(9.0f, 4.0f + selected.size * 1.0f)
        }

        // Aggregate unique books, problems, lectures
        val combinedBooks = selected.flatMap { it.rawInputs.books }.distinct()
        val combinedProblems = selected.flatMap { it.rawInputs.problemSets }.distinct()
        val combinedLectures = selected.flatMap { it.rawInputs.videoLectures }.distinct()

        // Synthesize strategic multi-subject synergy advice
        val combinedNotes = buildString {
            append("【多学科科学统筹备考原则】\n")
            append("1. 早晨精力唤醒期：安排记忆型与语言学科 (")
            val langSubjects = selected.filter { 
                it.id.contains("english") || it.id.contains("japanese") || it.id.contains("ancient") || 
                it.id.contains("politics") || it.id.contains("modern") || it.id.contains("international")
            }
            append(if (langSubjects.isNotEmpty()) langSubjects.joinToString("、") { it.title } else "语言积累与文化识记")
            append(")，主抓词汇、长难句、文选背诵、文化常识与核心概念记忆。\n")

            append("2. 上午黄金高能期：安排强逻辑与推导演算学科 (")
            val mathCsSubjects = selected.filter { it.id.contains("math") || it.id.contains("408") }
            append(if (mathCsSubjects.isNotEmpty()) mathCsSubjects.joinToString("、") { it.title } else "数理与核心专业课")
            append(")，专注深度概念突破、定理推导与硬核例题推演。\n")

            append("3. 下午综合攻坚期：安排大题实训、真题模考与专业分析 (")
            val majorSubjects = selected.filter { 
                it.id.contains("modern") || it.id.contains("408") || it.id.contains("ancient") || 
                it.id.contains("math") || it.id.contains("international")
            }
            append(if (majorSubjects.isNotEmpty()) majorSubjects.joinToString("、") { it.title } else "专业大题与综合真题")
            append(")，强化解题速度与规范书写。\n")

            append("4. 晚间与临睡期：安排多学科错题复盘与艾宾浩斯记忆滚动闭环，整理当日盲区，严格防范单科疲劳与偏废。\n\n")

            append("【各学科专属战术要点】\n")
            selected.forEach { preset ->
                append("• ${preset.title}: ${preset.rawInputs.customNotes}\n")
            }
        }.trim()

        return StudyRawInputs(
            examTitle = compositeTitle,
            targetDate = "",
            dailyHoursAvailable = calculatedHours,
            books = combinedBooks,
            problemSets = combinedProblems,
            videoLectures = combinedLectures,
            customNotes = combinedNotes
        )
    }
}

