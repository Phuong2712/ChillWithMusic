package com.example.listentomusic.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.listentomusic.databinding.FragmentSignUpBinding
import com.example.listentomusic.model.User
import com.example.listentomusic.ui.activity.LoginAndSignUpActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class SignUpFragment : Fragment() {
    private var _binding : FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    private val passwordPattern = "(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=])(?=.{6,}).+"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupView()
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher
            .addCallback(object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })

        binding.btnSignUp.setOnClickListener {
            signUp()
        }

        binding.btnLogin.setOnClickListener {
            (requireActivity() as LoginAndSignUpActivity).goToLogin()
        }
    }

    private fun signUp() {
        val name = binding.etSignUpName.text.toString().trim()
        val email = binding.etSignUpUserName.text.toString().trim()
        val password = binding.etSignUpPassWord.text.toString().trim()
        val rePassword = binding.etRePassWord.text.toString().trim()

        if(!email.matches(emailPattern.toRegex()))
            binding.etSignUpUserName.error = "Email không hợp lệ"
        else if(name.isEmpty())
            binding.etSignUpName.error = "Họ tên không được bỏ trống"
        else if(!password.matches(passwordPattern.toRegex()))
            binding.etSignUpPassWord.error = "Pass không hợp lệ"
        else if(password != rePassword)
            binding.etRePassWord.error = "Mật khẩu không khớp"
        else{
            val auth = Firebase.auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()){ task ->
                    if(task.isSuccessful){
                        Toast.makeText(requireContext(),
                            "Đăng ký tài khoản thành công",
                            Toast.LENGTH_LONG).show()
                        val user = auth.currentUser
                        writeNewUser(user!!.uid, email, password, name)
                        requireActivity().finish()
                    }
                    else{
                        Toast.makeText(
                            requireContext(),
                            "Tài khoản đã tồn tại",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }

    private fun writeNewUser(userId: String, email: String, password: String, name: String) {
        val user = User(userId, email, password, name)
        // ghi dữ liệu vào firebase
        val database = Firebase.database.reference
        database.child("users").child(userId).setValue(user)
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
        fun newInstance() : SignUpFragment{
            val args = Bundle()
            val signUpFragment = SignUpFragment()
            signUpFragment.arguments = args
            return signUpFragment
        }
    }
}