package com.example.listentomusic.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.listentomusic.R
import com.example.listentomusic.constant.Constants
import com.example.listentomusic.databinding.DialogChangeNameBinding
import com.example.listentomusic.databinding.DialogChangePassBinding
import com.example.listentomusic.databinding.FragmentProfileBinding
import com.example.listentomusic.model.User
import com.example.listentomusic.ui.activity.LoginAndSignUpActivity
import com.example.listentomusic.ui.activity.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.storage.storage

class ProfileFragment : Fragment() {
    private var _binding : FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth : FirebaseAuth
    private val storage by lazy { Firebase.storage }
    private lateinit var myUser : User
    private val passwordPattern = "(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=])(?=.{6,}).+"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupView()
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    (requireActivity() as MainActivity).handleBackpress()
                }
            }
        )

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(requireContext(), LoginAndSignUpActivity::class.java).apply{
                this.action = Constants.ACTION_SIGN_UP
            })
        }

        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(requireContext(), LoginAndSignUpActivity::class.java).apply{
                this.action = Constants.ACTION_LOG_IN
            })
        }

        binding.btnSignOut.setOnClickListener {
            Firebase.auth.signOut()
            updateViewSignedOut()
        }

        binding.imgProfile.setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT) //lấy mã nguồn dữ liệu
            pickImageLauncher.launch(intent)
        }

        binding.tvChangePass.setOnClickListener {
            showDialogChangePass()
        }

        binding.tvChangeInfo.setOnClickListener {
            showDialogChangeName()
        }
    }

    private fun showDialogChangePass() {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DialogChangePassBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = builder.create()

        binding.btnChangePass.setOnClickListener {
            val passwordNew = binding.etNewPass.text.toString()
            val passwordOld = binding.etOldPass.text.toString()

            if(!passwordNew.matches(passwordPattern.toRegex()))
                binding.etNewPass.error = "Mật khẩu không đúng định dạng"
            else if(passwordNew != passwordOld)
                binding.etNewPass.error = "Mật khẩu không khớp"
            else if(passwordOld != myUser.password)
                binding.etOldPass.error = "Mật khẩu cũ không đúng"
            else{
                changePass(passwordNew)
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun changePass(password: String) {
        val user = Firebase.auth.currentUser

        user?.updatePassword(password)
            // trình xử lý (listener) xử lý kết quả hoặc lỗi của một tác vụ Firebase khi nó hoàn thành.
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val database = Firebase.database
                    database.reference.child("user")
                        .child(auth.currentUser?.uid!!)
                        .child(Constants.CHILD_PASSWORD)
                        .setValue(password)
                }
                Toast.makeText(requireContext(), "Thay đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDialogChangeName() {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DialogChangeNameBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)
        val dialog = builder.create()

        binding.btnChangeName.setOnClickListener {
            val database = Firebase.database
            database.reference.child("users").child(auth.currentUser?.uid!!).child(Constants.CHILD_NAME).setValue(binding.etReNewName.text.toString())
            Toast.makeText(requireContext(), "Thay đổi tên thành công", Toast.LENGTH_SHORT).show()
            setupView()
            dialog.dismiss() // đóng cửa sổ
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun saveProfileImage(uri : Uri){
        val link = "${auth.currentUser?.uid}.ipg"
        val ref = storage.reference.child("profile_image/$link")
        val uploadTask = ref.putFile(uri)

        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                Toast.makeText(requireContext(), "Cập nhật ảnh thất bại", Toast.LENGTH_SHORT).show()
                task.exception.let {
                    throw it!!
                }
            }
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if(task.isSuccessful){
                val downloadUri = task.result
                Toast.makeText(requireContext(), "Cập nhật ảnh thành công", Toast.LENGTH_SHORT).show()
                val database = Firebase.database.reference
                database.child("user").child(auth.currentUser?.uid!!).child(Constants.CHILD_PROFILE_IMAGE).setValue(downloadUri.toString())
                Glide.with(requireContext()).load(downloadUri).error(R.drawable.ic_launcher_foreground).into(binding.imgProfile)
            }
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val selectedImageUri : Uri? = result.data?.data
                selectedImageUri?.let{
                    saveProfileImage(it)
                }
            }
        }

    private fun initData() {
        auth = Firebase.auth
    }

    private fun setupView() {
        val currentUser = auth.currentUser
        if(currentUser == null)
            updateViewSignedOut()
        else
            updateViewSignedIn(currentUser)
    }

    private fun updateViewSignedIn(user: FirebaseUser) {
        binding.btnSignUp.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE
        binding.tvChangeInfo.visibility = View.VISIBLE
        binding.tvChangePass.visibility = View.VISIBLE
        binding.btnSignOut.visibility = View.VISIBLE
        binding.tvHelloUser.visibility = View.VISIBLE

        val database = Firebase.database
        val myRef = database.reference.child(Constants.CHILD_USERS)
        val query = myRef.orderByChild("id").equalTo(user.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(datasnapshot: DataSnapshot) {
                val mapResult = datasnapshot.getValue<HashMap<String, User>>()
                mapResult?.let{
                    myUser = it[user.uid]!!
                    binding.tvHelloUser.text = "Xin chào, ${it[user.uid]?.name.toString()}"
                    Glide.with(requireContext()).load(it[user.uid]?.image).error(R.drawable.ic_launcher_foreground)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun updateViewSignedOut() {
        binding.btnSignIn.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.VISIBLE
        binding.tvChangeInfo.visibility = View.GONE
        binding.tvChangePass.visibility = View.GONE
        binding.btnSignOut.visibility = View.GONE
        binding.tvHelloUser.visibility = View.GONE
        binding.imgProfile.setImageResource(R.drawable.ic_launcher_foreground)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() : ProfileFragment{
            val args = Bundle()
            val profileFragment = ProfileFragment()
            profileFragment.arguments = args
            return profileFragment
        }
    }
}