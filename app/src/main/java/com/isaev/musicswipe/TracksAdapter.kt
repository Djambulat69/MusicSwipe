package com.isaev.musicswipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.yuyakaido.android.cardstackview.CardStackLayoutManager

class TracksAdapter(
    diffCallback: TracksDiffCallback,
    private val layoutManager: CardStackLayoutManager,
    private val mediaPlayer: (position: Int) -> Unit
) :
    RecyclerView.Adapter<TrackViewHolder>() {

    private val differ = AsyncListDiffer(this, diffCallback).apply {
        addListListener { prevList, _ ->
            if (layoutManager.topPosition == prevList.size) {
                layoutManager.cardStackListener.onCardAppeared(layoutManager.topView, layoutManager.topPosition)
            }
        }
    }

    var tracks: List<PlayTrack>
        get() = differ.currentList
        set(newTracks) {
            differ.submitList(newTracks)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_card_item, parent, false)
        return TrackViewHolder(view, mediaPlayer)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    private companion object {
        const val TAG = "TracksAdapter"
    }
}
