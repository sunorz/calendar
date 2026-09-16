package com.sunplace.calendar.data.model

import androidx.annotation.DrawableRes
import com.sunplace.calendar.R

enum class TodayPattern(
    val id: String,
    val title: String,
    val pinyin: String,
    val category: String,
    @DrawableRes val drawableRes: Int
) {
    // 5瓣系列
    SHILIUWUBAN("shiliuwuban", "石榴五瓣", "Shiliu Wuban", "5瓣", R.drawable.ic_pattern_shiliuwuban),
    MUDANWUBAN("mudanwuban", "牡丹五瓣", "Mudan Wuban", "5瓣", R.drawable.ic_pattern_mudanwuban),
    MEIHUAWUBAN("meihuawuban", "梅花五瓣", "Meihua Wuban", "5瓣", R.drawable.ic_pattern_meihuawuban),
    TAOHUAWUBAN("taohuawuban", "桃花五瓣", "Taohua Wuban", "5瓣", R.drawable.ic_pattern_taohuawuban),
    XINGHUAWUBAN("xinghuawuban", "杏花五瓣", "Xinghua Wuban", "5瓣", R.drawable.ic_pattern_xinghuawuban),
    LIANHUAWUBAN("lianhuawuban", "莲花五瓣", "Lianhua Wuban", "5瓣", R.drawable.ic_pattern_lianhuawuban),
    JUGENGWUBAN("jugengwuban", "桔梗五瓣", "Jugeng Wuban", "5瓣", R.drawable.ic_pattern_jugengwuban),
    MUJINWUBAN("mujinwuban", "木槿五瓣", "Mujin Wuban", "5瓣", R.drawable.ic_pattern_mujinwuban),
    SHANCHAWUBAN("shanchawuban", "山茶五瓣", "Shancha Wuban", "5瓣", R.drawable.ic_pattern_shanchawuban),
    XINGMANGWUBAN("xingmangwuban", "星芒五瓣", "Xingmang Wuban", "5瓣", R.drawable.ic_pattern_xingmangwuban),

    // 4瓣系列
    HAITANGSIBAN("haitangsiban", "海棠四瓣", "Haitang Siban", "4瓣", R.drawable.ic_pattern_haitangsiban),
    FANGSHENGSIBAN("fangshengsiban", "方胜四瓣", "Fangsheng Siban", "4瓣", R.drawable.ic_pattern_fangshengsiban),
    SHIDISIBAN("shidisiban", "柿蒂四瓣", "Shidi Siban", "4瓣", R.drawable.ic_pattern_shidisiban),
    LINGHUASIBAN("linghuasiban", "菱花四瓣", "Linghua Siban", "4瓣", R.drawable.ic_pattern_linghuasiban),
    LINGZHISIBAN("lingzhisiban", "灵芝四瓣", "Lingzhi Siban", "4瓣", R.drawable.ic_pattern_lingzhisiban),
    RUYISIBAN("ruyisiban", "如意四瓣", "Ruyi Siban", "4瓣", R.drawable.ic_pattern_ruyisiban),
    JUANCAOSIBAN("juancaosiban", "卷草四瓣", "Juancao Siban", "4瓣", R.drawable.ic_pattern_juancaosiban),
    KUIXINSIBAN("kuixinsiban", "葵心四瓣", "Kuixin Siban", "4瓣", R.drawable.ic_pattern_kuixinsiban),
    LIANXINSIBAN("lianxinsiban", "莲心四瓣", "Lianxin Siban", "4瓣", R.drawable.ic_pattern_lianxinsiban),
    YUNJIANSIBAN("yunjiansiban", "云肩四瓣", "Yunjian Siban", "4瓣", R.drawable.ic_pattern_yunjiansiban),

    // 6瓣系列
    BAOXIANGLLIUBAN("baoxiangliuban", "宝相六瓣", "Baoxiang Liuban", "6瓣", R.drawable.ic_pattern_baoxiangliuban),
    FANHUALIUBAN("fanhualiuban", "繁花六瓣", "Fanhua Liuban", "6瓣", R.drawable.ic_pattern_fanhualiuban),
    HUIXUANLIUBAN("huixuanliuban", "回旋六瓣", "Huixuan Liuban", "6瓣", R.drawable.ic_pattern_huixuanliuban),
    LINGHUALIUBAN("linghualiuban", "菱花六瓣", "Linghua Liuban", "6瓣", R.drawable.ic_pattern_linghualiuban),
    RUYILIUBAN("ruyiliuban", "如意六瓣", "Ruyi Liuban", "6瓣", R.drawable.ic_pattern_ruyiliuban),
    SHIDILIUBAN("shidiliuban", "柿蒂六瓣", "Shidi Liuban", "6瓣", R.drawable.ic_pattern_shidiliuban),
    TUANHUALIUBAN("tuanhualiuban", "团花六瓣", "Tuanhua Liuban", "6瓣", R.drawable.ic_pattern_tuanhualiuban),
    XINGLUNLIUBAN("xinglunliuban", "星轮六瓣", "Xinglun Liuban", "6瓣", R.drawable.ic_pattern_xinglunliuban),
    XUEHUALIUBAN("xuehualiuban", "雪花六瓣", "Xuehua Liuban", "6瓣", R.drawable.ic_pattern_xuehualiuban),
    YUNLIANLIUBAN("yunlianliuban", "云连六瓣", "Yunlian Liuban", "6瓣", R.drawable.ic_pattern_yunlianliuban),

    // 8瓣系列
    BABANJUWEN("babanjuwen", "八瓣菊纹", "Baban Juwen", "8瓣", R.drawable.ic_pattern_babanjuwen),
    BABAOLIANWEN("babaolianwen", "八宝莲纹", "Babao Lianwen", "8瓣", R.drawable.ic_pattern_babaolianwen),
    BAFANGLUNWEN("bafanglunwen", "八方轮纹", "Bafang Lunwen", "8瓣", R.drawable.ic_pattern_bafanglunwen),
    BAFANGRUYI("bafangruyi", "八方如意", "Bafang Ruyi", "8瓣", R.drawable.ic_pattern_bafangruyi),
    BAFANGSHIDI("bafangshidi", "八方柿蒂", "Bafang Shidi", "8瓣", R.drawable.ic_pattern_bafangshidi),
    BAJIAOTUANHUA("bajiaotuanhua", "八角团花", "Bajiao Tuanhua", "8瓣", R.drawable.ic_pattern_bajiaotuanhua),
    BAMIANYUNWEN("bamianyunwen", "八面云纹", "Bamian Yunwen", "8瓣", R.drawable.ic_pattern_bamianyunwen),
    BAXIANGXINGWEN("baxiangxingwen", "八相星纹", "Baxiang Xingwen", "8瓣", R.drawable.ic_pattern_baxiangxingwen),
    BAYEBAOXIANG("bayebaoxiang", "八叶宝相", "Baye Baoxiang", "8瓣", R.drawable.ic_pattern_bayebaoxiang),
    BAYEFANHUA("bayefanhua", "八叶繁花", "Baye Fanhua", "8瓣", R.drawable.ic_pattern_bayefanhua);

    fun getDisplayName(isEnglish: Boolean): String {
        if (!isEnglish) return title
        return when {
            pinyin.endsWith(" Wuban") -> "${pinyin.removeSuffix(" Wuban")} (5 Petals)"
            pinyin.endsWith(" Siban") -> "${pinyin.removeSuffix(" Siban")} (4 Petals)"
            pinyin.endsWith(" Liuban") -> "${pinyin.removeSuffix(" Liuban")} (6 Petals)"
            category == "8瓣" -> {
                val cleanPinyin = pinyin
                    .replace("Baban ", "")
                    .replace("Bafang ", "")
                    .replace("Bajiao ", "")
                    .replace("Bamian ", "")
                    .replace("Baxiang ", "")
                    .replace("Baye ", "")
                "$cleanPinyin (8 Petals)"
            }
            else -> "$pinyin (${category.replace("瓣", " Petals")})"
        }
    }

    companion object {
        val DEFAULT = SHILIUWUBAN

        fun fromId(id: String?): TodayPattern {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
