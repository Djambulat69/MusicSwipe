package com.isaev.musicswipe

import android.view.View
import androidx.recyclerview.widget.RecyclerView
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
        with(binding) {
            trackName.text = playTrack.track.name
            trackPreviewUrl.text = playTrack.track.previewUrl
            playButton.setImageResource(
                if (playTrack.isPlaying) R.drawable.ic_baseline_pause_circle else R.drawable.ic_baseline_play_circle
            )
        }
    }
}
