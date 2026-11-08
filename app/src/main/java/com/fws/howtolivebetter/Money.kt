package com.fws.howtolivebetter

import java.text.NumberFormat
import java.util.Locale

/**
 * Tỷ giá THAM KHẢO quy đổi từ 1 nhân dân tệ (CNY). Sửa tại đây khi cần cập nhật.
 * VND và USD theo tỷ giá bạn cung cấp (10.000 CNY = 38.540.000 VND = 1.498,64 USD).
 * Các đồng còn lại là giá trị ước tính quanh tháng 11/2026 (suy ra từ USD), chỉ mang tính tham khảo.
 */
enum class Cur(val code: String, val perCny: Double, val decimals: Int, val viName: String) {
    VND("VND", 3854.0, 0, "đồng"),
    USD("USD", 0.149864, 2, "đô la Mỹ"),
    CNY("CNY", 1.0, 0, "nhân dân tệ"),
    EUR("EUR", 0.129, 2, "ơ-rô"),
    BRL("BRL", 0.77, 2, "rê-an Brazil"),
    RUB("RUB", 11.4, 0, "rúp Nga"),
    JPY("JPY", 22.7, 0, "yên Nhật"),
    KRW("KRW", 192.0, 0, "won Hàn Quốc"),
}

const val FX_DATE = "11/2026"

object Money {
    // ⟦1000⟧ hoặc ⟦100~300⟧ — token do script dữ liệu chèn vào thay cho "… nhân dân tệ".
    private val TOKEN = Regex("⟦([0-9.]+)(?:~([0-9.]+))?⟧")

    fun curFor(lang: String): Cur = when (lang) {
        "vi" -> Cur.VND; "zh" -> Cur.CNY; "es" -> Cur.EUR; "pt" -> Cur.BRL
        "ru" -> Cur.RUB; "ja" -> Cur.JPY; "ko" -> Cur.KRW; else -> Cur.USD
    }

    fun localeFor(lang: String): Locale = when (lang) {
        "vi" -> Locale("vi", "VN"); "zh" -> Locale.SIMPLIFIED_CHINESE; "es" -> Locale("es", "ES")
        "pt" -> Locale("pt", "BR"); "ru" -> Locale("ru", "RU"); "ja" -> Locale.JAPAN
        "ko" -> Locale.KOREA; else -> Locale.US
    }

    private fun num(v: Double, cur: Cur, loc: Locale): String {
        val nf = NumberFormat.getNumberInstance(loc)
        val d = if (cur == Cur.CNY) (if (v % 1.0 == 0.0) 0 else 2) else cur.decimals
        nf.minimumFractionDigits = d
        nf.maximumFractionDigits = d
        return nf.format(v)
    }

    /** Hiển thị trên màn hình: "≈ 38.540.000 VND" hoặc "≈ 385.400 – 1.155.300 VND". */
    fun render(text: String, cur: Cur, loc: Locale): String = TOKEN.replace(text) { m ->
        val a = num(m.groupValues[1].toDouble() * cur.perCny, cur, loc)
        val b = m.groupValues[2].takeIf { it.isNotEmpty() }?.let { num(it.toDouble() * cur.perCny, cur, loc) }
        val body = if (b != null) "$a – $b" else a
        (if (cur == Cur.CNY) "" else "≈ ") + body + " " + cur.code
    }

    /** Bản đọc thành tiếng (nội dung tiếng Việt → đọc bằng giọng Việt, tên tiền tệ bằng chữ). */
    fun speech(text: String, cur: Cur): String {
        val loc = localeFor("vi")
        return TOKEN.replace(text) { m ->
            val a = num(m.groupValues[1].toDouble() * cur.perCny, cur, loc)
            val b = m.groupValues[2].takeIf { it.isNotEmpty() }?.let { num(it.toDouble() * cur.perCny, cur, loc) }
            (if (b != null) "từ $a đến $b" else "khoảng $a") + " " + cur.viName
        }.replace("%", " phần trăm").replace("–", " đến ").replace("≈", "khoảng")
    }
}
