package com.isaev.musicswipe.ui

import android.os.Bundle
import android.view.View
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class TracksCardStackListener(
    private val viewModel: TracksViewModel,
    private val cardStack: CardStackView,
    private inline val onPrepareTrack: () -> Unit,
    private inline val getSavedInstanceState: () -> Bundle?
) : CardStackListener {
    override fun onCardAppeared(view: View?, position: Int) {
        val savedInstanceState = getSavedInstanceState()
        if (savedInstanceState != null) { // Prevents preparing track again on rotation
            val savedTopPosition: Int = savedInstanceState.getInt(TracksFragment.CARD_STACK_STATE_KEY)
            if (savedTopPosition == position || position == 0) return
        }

        val adapter = cardStack.adapter as TracksAdapter

        val cardsLeft = adapter.itemCount - position
        if (cardsLeft < TRACKS_PREFETCH_DISTANCE) {
            viewModel.loadRecommendations()
        }

        val track = adapter.tracks.getOrNull(position)

        track?.previewUrl?.let {
            viewModel.prepareNewTrack(track.previewUrl)
            onPrepareTrack()
        }
    }

    override fun onCardCanceled() {}
    override fun onCardDisappeared(view: View?, position: Int) {}
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardSwiped(direction: Direction?) {
        if (direction == Direction.Right) {
            val position = (cardStack.layoutManager as CardStackLayoutManager).topPosition - 1
            val adapter = cardStack.adapter as TracksAdapter
            val likedTrack = adapter.tracks.getOrNull(position)
            likedTrack?.let {
                viewModel.onTrackLiked(likedTrack)
            }
        }
    }

    companion object {
        private const val TRACKS_PREFETCH_DISTANCE = 5
    }
}
