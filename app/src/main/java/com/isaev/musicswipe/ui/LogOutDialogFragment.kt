package com.isaev.musicswipe.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.isaev.musicswipe.R
import com.isaev.musicswipe.data.SpotifyAuthService
import com.isaev.musicswipe.databinding.LogOutDialogBinding
import com.isaev.musicswipe.myApplication
import javax.inject.Inject

class LogOutDialogFragment : DialogFragment() {

    @Inject
    lateinit var spotifyAuthService: SpotifyAuthService

    lateinit var binding: LogOutDialogBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        myApplication.daggerComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        binding = LogOutDialogBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.log_out, null)
            .create()
    }

    override fun onStart() {
        super.onStart()

        val avatarUrl = requireArguments().getString(AVATAR_URL_ARG)

        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.ic_account_circle)
            .into(binding.dialogAvatar)

        val dialog = (requireDialog() as AlertDialog)
        val logOutButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        val primaryColor: Int = MaterialColors.getColor(
            requireContext(),
            R.attr.colorPrimary,
            resources.getColor(R.color.spotify_green, requireContext().theme)
        )

        val onPrimaryColor: Int = MaterialColors.getColor(
            requireContext(),
            R.attr.colorOnPrimary,
            resources.getColor(R.color.white, requireContext().theme)
        )


        logOutButton.setBackgroundColor(primaryColor)
        logOutButton.setTextColor(onPrimaryColor)

        logOutButton.setOnClickListener {
            if (spotifyAuthService.isAuthorized()) {
                spotifyAuthService.logOut()
                dismiss()
            }
        }
    }

    companion object {
        fun newInstance(avatarUrl: String?) = LogOutDialogFragment().apply {
            arguments = bundleOf(AVATAR_URL_ARG to avatarUrl)
        }

        private const val AVATAR_URL_ARG = "avatar url arg"
    }
}
