package com.isaev.musicswipe

import android.content.Context
import android.view.animation.AccelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.yuyakaido.android.cardstackview.*

fun Fragment.makeSnackBar(text: CharSequence, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
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

val Fragment.fragmentInteractor: FragmentInteractor?
    get() = activity as? FragmentInteractor

inline fun <reified VM : ViewModel> Fragment.viewModelsFactory(
    noinline createViewModel: (() -> VM)? = null
): Lazy<VM> {

    return viewModels(factoryProducer = createViewModel?.let { { createViewModelFactory(it) } })

}

inline fun <reified VM : ViewModel> AppCompatActivity.viewModelsFactory(
    noinline createViewModel: (() -> VM)? = null
): Lazy<VM> {

    return viewModels(factoryProducer = createViewModel?.let { { createViewModelFactory(it) } })

}

@Suppress("unchecked_cast")
fun <VM : ViewModel> createViewModelFactory(createViewModel: () -> VM): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return createViewModel() as T
        }
    }
}
