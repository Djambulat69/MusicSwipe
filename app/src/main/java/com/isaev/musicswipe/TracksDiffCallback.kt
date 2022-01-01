package com.isaev.musicswipe

import androidx.recyclerview.widget.DiffUtil

class TracksDiffCallback : DiffUtil.ItemCallback<PlayTrack>() {

    override fun areItemsTheSame(oldItem: PlayTrack, newItem: PlayTrack): Boolean {
        return oldItem.track.id == newItem.track.id
    }

    override fun areContentsTheSame(oldItem: PlayTrack, newItem: PlayTrack): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: PlayTrack, newItem: PlayTrack): Any? {
        return if (oldItem.isPlaying != newItem.isPlaying) newItem.isPlaying else null
    }
}
