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

    private var trimmedTrackName: String? = null
    private var trackName: String? = null
        set(newTrackName) {
            field = newTrackName
            if (newTrackName != null) {
                trimmedTrackName = if (newTrackName.length > TRIMMED_LENGTH) {
                    context.getString(R.string.trunked_title, newTrackName.take(TRIMMED_LENGTH))
                } else {
                    newTrackName
                }
            }
        }
    private var isFull = false

    init {
        binding.trackName.setOnClickListener {
            val length = trackName?.length
            if (length != null && length > TRIMMED_LENGTH) {
                if (isFull) {
                    binding.trackName.text = trimmedTrackName
                } else {
                    binding.trackName.text = trackName
                }
                isFull = !isFull
            }
        }
    }

    fun bind(track: Track) {
        trackName = track.name
        isFull = false

        with(binding) {
            trackName.text = trimmedTrackName
            trackArtists.text = track.artists.joinToString(separator = ", ") { it.name }
            Glide.with(context)
                .load(track.album.images.firstOrNull()?.url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_baseline_album_placeholder)
                .into(trackCover)
        }
    }

    private companion object {
        private const val TRIMMED_LENGTH = 30
    }
}
