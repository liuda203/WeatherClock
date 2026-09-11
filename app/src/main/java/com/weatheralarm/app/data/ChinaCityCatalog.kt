package com.weatheralarm.app.data

import android.content.Context
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicReference

/**
 * 基于用户提供的中国天气网城市区号表（assets/china_city_codes.json）。
 */
object ChinaCityCatalog {
    private val cache = AtomicReference<List<ChinaCity>?>(null)

    fun all(context: Context): List<ChinaCity> {
        cache.get()?.let { return it }
        synchronized(this) {
            cache.get()?.let { return it }
            val text = context.assets.open("china_city_codes.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            val list = buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ChinaCity(
                            code = o.getString("code"),
                            name = o.getString("name"),
                            province = o.getString("province"),
                            parent = o.getString("parent"),
                            display = o.getString("display")
                        )
                    )
                }
            }
            cache.set(list)
            return list
        }
    }

    fun search(context: Context, query: String, limit: Int = 20): List<CitySuggestion> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()

        val scored = all(context).mapNotNull { city ->
            val score = scoreMatch(q, city) ?: return@mapNotNull null
            score to CitySuggestion(
                code = city.code,
                name = city.name,
                province = city.province,
                parent = city.parent,
                displayName = "${city.display}（${city.code}）"
            )
        }

        return scored
            .sortedWith(
                compareBy<Pair<Int, CitySuggestion>> { it.first }
                    .thenBy { it.second.province }
                    .thenBy { it.second.parent }
                    .thenBy { it.second.name.length }
                    .thenBy { it.second.name }
            )
            .take(limit)
            .map { it.second }
    }

    fun findByCode(context: Context, code: String): ChinaCity? =
        all(context).firstOrNull { it.code == code }

    /**
     * 用定位得到的省/市/区名称，在本地区号表中尽量精确匹配。
     */
    fun resolveFromAddress(
        context: Context,
        provinceHint: String?,
        cityHint: String?,
        districtHint: String?,
        fallbackLabel: String?
    ): ChinaCity? {
        val cities = all(context)
        val province = normalizeAdmin(provinceHint)
        val city = normalizeAdmin(cityHint)
        val district = normalizeAdmin(districtHint)

        // 1) 省 + 区县名精确匹配
        if (!province.isNullOrBlank() && !district.isNullOrBlank()) {
            cities.firstOrNull {
                normalizeAdmin(it.province) == province &&
                    (normalizeAdmin(it.name) == district || it.name.contains(district))
            }?.let { return it }
        }

        // 2) 省 + 市 + 区
        if (!province.isNullOrBlank() && !city.isNullOrBlank() && !district.isNullOrBlank()) {
            cities.firstOrNull {
                normalizeAdmin(it.province) == province &&
                    normalizeAdmin(it.parent).orEmpty().contains(city) &&
                    (normalizeAdmin(it.name) == district || it.name.contains(district))
            }?.let { return it }
        }

        // 3) 省 + 市
        if (!province.isNullOrBlank() && !city.isNullOrBlank()) {
            cities.firstOrNull {
                normalizeAdmin(it.province) == province &&
                    (normalizeAdmin(it.name) == city || normalizeAdmin(it.parent) == city)
            }?.let { return it }
        }

        // 4) 仅区县名（若唯一）
        if (!district.isNullOrBlank()) {
            val hits = cities.filter {
                normalizeAdmin(it.name) == district || it.name == districtHint
            }
            if (hits.size == 1) return hits.first()
            if (!province.isNullOrBlank()) {
                hits.firstOrNull { normalizeAdmin(it.province) == province }?.let { return it }
            }
        }

        // 5) 回退：用完整地址文本包含匹配
        val label = fallbackLabel.orEmpty()
        if (label.isNotBlank()) {
            cities.firstOrNull { label.contains(it.display.replace(" · ", "")) || label.contains(it.name) }
                ?.let { return it }
        }
        return null
    }

    private fun scoreMatch(query: String, city: ChinaCity): Int? {
        val name = normalize(city.name)
        val parent = normalize(city.parent)
        val province = normalize(city.province)
        val display = normalize(city.display)
        val code = city.code

        return when {
            code == query -> 0
            name == query -> 1
            parent == query -> 2
            display == query -> 3
            name.startsWith(query) -> 10
            parent.startsWith(query) -> 11
            province == query -> 12
            name.contains(query) -> 20
            parent.contains(query) -> 21
            display.contains(query) -> 22
            province.contains(query) -> 30
            // 支持“广东深圳”“江苏 南通”这类组合
            query.length >= 2 && display.replace(" ", "").contains(query.replace(" ", "")) -> 25
            else -> null
        }
    }

    private fun normalize(text: String): String =
        text.trim()
            .lowercase()
            .replace(" ", "")
            .replace("　", "")

    private fun normalizeAdmin(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return text.trim()
            .removeSuffix("特别行政区")
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
            .removeSuffix("自治区")
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("地区")
            .removeSuffix("盟")
    }
}
