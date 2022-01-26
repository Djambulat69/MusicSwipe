package com.isaev.musicswipe.ui

import androidx.recyclerview.widget.DiffUtil
import com.isaev.musicswipe.data.Track

class TracksDiffCallback : DiffUtil.ItemCallback<Track>() {

    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }
}
