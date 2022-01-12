package com.isaev.musicswipe

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.isaev.musicswipe.databinding.TrackCardItemBinding

class TrackViewHolder(view: View) :
    RecyclerView.ViewHolder(view) {

    private val binding: TrackCardItemBinding = TrackCardItemBinding.bind(itemView)

    fun bind(playTrack: PlayTrack) {
        var isFull = false
        with(binding) {
            val fullTrackName = playTrack.track.name
            if (fullTrackName.length > 30) {
                trackName.text = playTrack.track.name.take(30) + "..."
                trackName.setOnClickListener {
                    if (isFull) {
                        trackName.text = playTrack.track.name.take(30) + "..."
                    } else {
                        trackName.text = playTrack.track.name
                    }
                    isFull = !isFull
                }
            } else {
                trackName.text = fullTrackName
                trackName.setOnClickListener(null)
            }
            trackArtists.text = playTrack.track.artists.joinToString(separator = ", ") { it.name }
            Glide.with(itemView.context)
                .load(playTrack.track.album.images.firstOrNull()?.url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_baseline_album_placeholder)
                .into(trackCover)
        }
    }
}
