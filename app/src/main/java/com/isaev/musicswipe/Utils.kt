package com.isaev.musicswipe

import android.content.Context
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.yuyakaido.android.cardstackview.*

fun Fragment.snackBar(text: CharSequence, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(requireView(), text, duration)
}

fun CardStackView.swipeLeft() {
    val swipeLeftSettings = SwipeAnimationSetting.Builder()
        .setDirection(Direction.Left)
        .setDuration(Duration.Normal.duration)
        .setInterpolator(AccelerateInterpolator())
        .build()

    (layoutManager as CardStackLayoutManager).setSwipeAnimationSetting(swipeLeftSettings)
    swipe()
}

fun CardStackView.swipeRight() {
    val swipeRightSettings = SwipeAnimationSetting.Builder()
        .setDirection(Direction.Right)
        .setDuration(Duration.Normal.duration)
        .setInterpolator(AccelerateInterpolator())
        .build()

    (layoutManager as CardStackLayoutManager).setSwipeAnimationSetting(swipeRightSettings)
    swipe()
}

val RecyclerView.ViewHolder.context: Context
    get() = itemView.context

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycleScope
