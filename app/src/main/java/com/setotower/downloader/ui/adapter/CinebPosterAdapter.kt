package com.setotower.downloader.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.setotower.downloader.R
import com.setotower.downloader.util.CinebMedia
import com.squareup.picasso.Picasso

class CinebPosterAdapter(
    private var items: List<CinebMedia>,
    private val onClick: (CinebMedia) -> Unit
) : RecyclerView.Adapter<CinebPosterAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.cineb_poster)
        val title: TextView = view.findViewById(R.id.cineb_poster_title)
        val meta: TextView = view.findViewById(R.id.cineb_poster_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.cineb_poster_card, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.meta.text = item.releaseDate.take(4).ifBlank { "Cineb" }
        if (item.posterUrl.isBlank()) holder.poster.setImageResource(R.color.black)
        else Picasso.get().load(item.posterUrl).fit().centerCrop().into(holder.poster)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<CinebMedia>) {
        items = newItems
        notifyDataSetChanged()
    }
}
