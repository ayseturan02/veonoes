package com.veonoes.ar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.veonoes.R

class GlassesAdapter(
    private val items: List<GlassesSpec>,
    private val onPick: (GlassesSpec) -> Unit
) : RecyclerView.Adapter<GlassesAdapter.VH>() {

    private var selected = 0

    inner class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_glasses, parent, false) as ImageView
        return VH(iv)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        Glide.with(holder.itemView)
            .load(item.url)
            .into(holder.iv)
        holder.iv.alpha = if (position == selected) 1f else 0.6f
        holder.iv.setOnClickListener {
            val old = selected
            selected = position
            notifyItemChanged(old)
            notifyItemChanged(selected)
            onPick(item)
        }
    }
}
