package com.veonoes.ar

import com.google.firebase.storage.FirebaseStorage

data class GlassesSpec(
    val url: String,
    val scale: Float = 210f,
    val smoothFactor: Float = 0.02f
)

object GlassesRepo {
    fun loadGlasses(onResult: (List<GlassesSpec>) -> Unit) {
        val storage = FirebaseStorage.getInstance().reference
        val models = listOf("glasses_black.png", "glasses_black_full.png", "glasses_gold.png", "glasses_round.png")
        val list = mutableListOf<GlassesSpec>()
        var loaded = 0
        for (name in models) {
            storage.child(name).downloadUrl.addOnSuccessListener { uri ->
                list.add(GlassesSpec(uri.toString()))
                loaded++
                if (loaded == models.size) onResult(list)
            }.addOnFailureListener {
                loaded++
                if (loaded == models.size) onResult(list)
            }
        }
    }
}
