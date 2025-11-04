package com.veonoes.ar

import com.veonoes.R

// Tek bir gözlük modelini tanımlayan veri sınıfı
data class GlassesSpec(
    val resId: Int,                  // drawable kaynağı
    val baselineEyeDistancePx: Float, // gözler arası referans mesafesi
    val yOffsetRatio: Float           // göz hizasına göre dikey ofset oranı
)

// Gözlüklerin listesini yöneten nesne
object GlassesRepo {

    val all: List<GlassesSpec> = listOf(
        GlassesSpec(R.drawable.glasses_black, 230f, 0.05f),
        GlassesSpec(R.drawable.glasses_gold, 230f, 0.05f),
        GlassesSpec(R.drawable.glasses_round, 230f, 0.05f),
        GlassesSpec(R.drawable.glasses_black_full, 240f, 0.04f) // yeni model
    )

    // Deeplink'ten gelen model anahtarını liste index'ine çevirir
    fun indexOfKey(key: String?): Int = when (key) {
        "black" -> 0
        "gold" -> 1
        "round" -> 2
        "blackfull", "black_full" -> 3
        else -> 0
    }
}
