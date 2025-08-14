package com.example.cocktaildb.utils.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.data.model.DataCocktail
import com.example.cocktaildb.databinding.ItemCocktailSearchBinding
import java.net.URL
import java.util.concurrent.Executors

class CocktailAdapter(
    private var items: List<DataCocktail>
) : RecyclerView.Adapter<CocktailAdapter.VH>() {

    class VH(val binding: ItemCocktailSearchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCocktailSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvCategory.text = item.category ?: "Unknown"

        if (item.rating != null) {
            holder.binding.tvRating.text = String.format("%.1f", item.rating)
            holder.binding.tvRating.parent?.let {
                (it as? View)?.visibility = View.VISIBLE
            }
        } else {
            holder.binding.tvRating.parent?.let {
                (it as? View)?.visibility = View.GONE
            }
        }

        ImageLoader.load(item.imageUrl, holder.binding.ivThumb)
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<DataCocktail>) {
        items = newItems
        notifyDataSetChanged()
    }
}

private object ImageLoader {
    private val executor = Executors.newFixedThreadPool(3)
    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun load(url: String?, target: ImageView) {
        if (url.isNullOrEmpty()) {
            target.setImageDrawable(null)
            return
        }
        val cached = cache.get(url)
        if (cached != null) {
            target.setImageBitmap(cached)
            return
        }
        target.tag = url
        executor.execute {
            try {
                val bmp = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    cache.put(url, bmp)
                    target.post {
                        if (target.tag == url) target.setImageBitmap(bmp)
                    }
                }
            } catch (_: Exception) {

            }
        }
    }
}

