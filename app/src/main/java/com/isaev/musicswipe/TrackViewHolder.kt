package com.isaev.musicswipe

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.isaev.musicswipe.databinding.TrackCardItemBinding

class TrackViewHolder(view: View, private val mediaPlayer: (position: Int) -> Unit) :
    RecyclerView.ViewHolder(view) {

    private val binding: TrackCardItemBinding = TrackCardItemBinding.bind(itemView)

    init {
        binding.playButton.setOnClickListener {
            mediaPlayer(adapterPosition)
        }
    }

    fun bind(playTrack: PlayTrack) {
        var isFull = false
        with(binding) {
            /*spotifyLogo.updateLayoutParams {
                height = itemView.context.resources.getDimensionPixelSize(
                    itemView.context.resources.getDimension(70).toInt()
                )
            }*/
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
            playButton.setImageResource(
                if (playTrack.isPlaying) R.drawable.ic_baseline_pause_circle else R.drawable.ic_baseline_play_circle
            )
            Glide.with(itemView.context)
                .load(playTrack.track.album.images.firstOrNull()?.url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_baseline_album_placeholder)
                .into(trackCover)
        }
    }

    fun updatePlayback(playTrack: PlayTrack) {
        binding.playButton.setImageResource(
            if (playTrack.isPlaying) R.drawable.ic_baseline_pause_circle else R.drawable.ic_baseline_play_circle
        )
    }
}
