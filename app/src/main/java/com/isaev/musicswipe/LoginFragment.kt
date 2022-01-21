package com.isaev.musicswipe

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.isaev.musicswipe.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding: FragmentLoginBinding get() = _binding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLoginBinding.bind(view)

        binding.loginButton.setOnClickListener {
            fragmentInteractor?.openLoginWebView()
        }
    }

    companion object {
        fun newInstance() = LoginFragment()
    }
}
