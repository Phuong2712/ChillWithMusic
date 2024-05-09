package com.example.listentomusic.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.listentomusic.R
import com.example.listentomusic.constant.Constants
import com.example.listentomusic.databinding.ActivityLoginAndSignUpBinding
import com.example.listentomusic.ui.fragment.LoginFragment
import com.example.listentomusic.ui.fragment.SignUpFragment

class LoginAndSignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginAndSignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginAndSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        if(intent?.action == Constants.ACTION_LOG_IN) replaceFragment(LoginFragment.newInstance())
        else replaceFragment(SignUpFragment.newInstance())
    }

    fun goToLogin(){
        replaceFragment(LoginFragment.newInstance())
    }

    fun goToSignUp(){
        replaceFragment(SignUpFragment.newInstance())
    }

    private fun replaceFragment(fragment: Fragment){
        // them , sua, xoa , thay the fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}






