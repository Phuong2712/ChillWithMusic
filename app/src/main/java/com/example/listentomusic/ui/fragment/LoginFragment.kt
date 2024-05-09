package com.example.listentomusic.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.listentomusic.databinding.FragmentLoginBinding
import com.example.listentomusic.ui.activity.LoginAndSignUpActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class LoginFragment : Fragment() {
    private var _binding : FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    private val passwordPattern = "(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=])(?=.{6,}).+"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupView()
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        })

        binding.tvSignUp.setOnClickListener{
            (requireActivity() as LoginAndSignUpActivity).goToSignUp()
        }

        binding.btnSignIn.setOnClickListener{
            login()
        }
    }

    private fun login() {
        val userEmail = binding.etUserName.text.toString()
        val userPassword = binding.etPassWord.text.toString()

        val auth = Firebase.auth

        if(!userEmail.matches(emailPattern.toRegex())){
            binding.etUserName.error = "Email không hợp lệ"
        }else if(!userPassword.matches(passwordPattern.toRegex())){
            binding.etPassWord.error = "Tài khoản hoac mật khẩu sai"
        }else{
            auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(requireActivity()){ task->
                    if(task.isSuccessful){
                        Toast.makeText(requireContext(),
                            "Đăng nhập thành công",
                            Toast.LENGTH_LONG).show()
                        requireActivity().finish()
                    }else{
                        Toast.makeText(requireContext(),
                            "Đăng nhập không thành công",
                            Toast.LENGTH_LONG).show()
                        requireActivity().finish()
                    }
                }
        }
    }

    private fun setupView() {

    }

    private fun initData() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() : LoginFragment{
            val args = Bundle()
            val loginFragment = LoginFragment()
            loginFragment.arguments = args
            return loginFragment
        }
    }
}
