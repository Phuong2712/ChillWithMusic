package com.example.listentomusic.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.listentomusic.constant.Constants
import com.example.listentomusic.databinding.FragmentHomeBinding
import com.example.listentomusic.epoxy.MyEpoxyController
import com.example.listentomusic.model.MusicItem
import com.example.listentomusic.ui.activity.HistoryActivity
import com.example.listentomusic.ui.activity.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var listMusic : ArrayList<MusicItem>
    private lateinit var myController : MyEpoxyController
    private var isFiltering = false
    private var isFragmentRunning = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    //khởi chạy đối tượng
    //chạy trong HistoryActivity
    private val historyActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == Activity.RESULT_OK){
                val selectedSongId : String = result.data?.action!!
                (requireActivity() as MainActivity).startPlaying(getSongId(selectedSongId))
            }
        }

    private fun getSongId(id: String) : MusicItem{
        var musicItem = MusicItem()
        for(item in listMusic){
            if(item.id.toString() == id) musicItem = item
        }
        return musicItem
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFragmentRunning = true
        setupView()
        initData("")
        handEvent()
    }

    private fun handEvent() {
        // xử lý sự kiện vs nút back
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireActivity() as MainActivity).handleBackpress()
            }
        })

        // theo dõi và xử lý sự thay đổi trong nội dung của TextView
        binding.etSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                isFiltering = binding.etSearch.text.toString() != ""
                initData(binding.etSearch.text.toString())
            }
        })

        binding.btnHistory.setOnClickListener {
            historyActivityLauncher.launch(Intent(requireActivity(), HistoryActivity::class.java))
        }
    }

    private fun initData(keyword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = Firebase.database
            val myRef = database.reference.child(Constants.CHILD_MUSICS)

            myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val tempList = dataSnapshot.getValue<List<MusicItem>>()
                    tempList?.let{
                        for(item in it){
                            if(item.name!!.contains(keyword, true) ||
                                item.author!!.contains(keyword, true))
                                listMusic.add(item)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        if(isFragmentRunning){
                            if(isFiltering) binding.rvMusics.layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
                            else binding.rvMusics.layoutManager = LinearLayoutManager(requireContext())
                            myController.setIsFiltering(isFiltering)
                            myController.setListMusic(listMusic)
                            myController.requestModelBuild()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        }
    }

    private fun setupView() {
        myController = MyEpoxyController.newInstance(requireContext()){
            (requireActivity() as MainActivity).startPlaying(it)
        }
        binding.rvMusics.setControllerAndBuildModels(myController)
    }

    override fun onResume() {
        super.onResume()
        isFragmentRunning = true
    }

    override fun onPause() {
        super.onPause()
        isFragmentRunning = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFragmentRunning = false
        _binding = null
    }

    companion object{
        fun newInstance():HomeFragment{
            val homeFragment = HomeFragment()
            val args = Bundle()
            homeFragment.arguments = args
            return homeFragment
        }
    }
}