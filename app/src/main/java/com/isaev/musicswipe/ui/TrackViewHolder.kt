package com.isaev.musicswipe.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.isaev.musicswipe.R
import com.isaev.musicswipe.context
import com.isaev.musicswipe.data.Track
import com.isaev.musicswipe.databinding.TrackCardItemBinding

class TrackViewHolder(view: View) :
    RecyclerView.ViewHolder(view) {

    private val binding: TrackCardItemBinding = TrackCardItemBinding.bind(itemView)

    fun bind(track: Track) {
        var isFull = false
        with(binding) {
            val fullTrackName = track.name
            if (fullTrackName.length > 30) {
                val trunkedTitle = context.getString(R.string.trunked_title, track.name.take(30))
                trackName.text = trunkedTitle
                trackName.setOnClickListener {
                    if (isFull) {
                        trackName.text = trunkedTitle
                    } else {
                        trackName.text = fullTrackName
                    }
                    isFull = !isFull
                }
            } else {
                trackName.text = fullTrackName
                trackName.setOnClickListener(null)
            }
            trackArtists.text = track.artists.joinToString(separator = ", ") { it.name }
            Glide.with(itemView.context)
                .load(track.album.images.firstOrNull()?.url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_baseline_album_placeholder)
                .into(trackCover)
        }
    }
}
