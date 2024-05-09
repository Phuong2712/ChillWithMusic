package com.example.listentomusic.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.listentomusic.constant.Constants
import com.example.listentomusic.databinding.ActivityHistoryBinding
import com.example.listentomusic.epoxy.HistoryController
import com.example.listentomusic.model.MusicItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private var listMusic = ArrayList<MusicItem>()
    private var listFavorite = ArrayList<MusicItem>()
    private lateinit var myController : HistoryController
    private var isFirstTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if(currentUser == null){
            binding.tvNeedLogin.visibility = View.VISIBLE
        }else{
            myController = HistoryController.newInstance(this@HistoryActivity,{
                // truyền dữ liệu kết quả trở lại cho Activity gọi
                val resultIntent = Intent()
                resultIntent.action = it.id.toString()
                setResult(RESULT_OK, resultIntent)
                finish()
            },{
                updateFavorite(it)
            })

            binding.rvFav.setControllerAndBuildModels(myController)
            initData()
        }
    }

    private fun updateFavorite(musicItem: MusicItem) {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if(currentUser != null){
            val ref = Firebase.database.reference
            if(musicItem.isFavorite){
                musicItem.isFavorite = false
                listFavorite.remove(musicItem)
                ref.child("users").child(auth.currentUser?.uid!!)
                    .child(Constants.CHILD_FAVORITE)
                    .setValue(listFavorite)
            }else{
                listFavorite.add(musicItem)
                ref.child("users").child(auth.currentUser?.uid!!)
                    .child(Constants.CHILD_FAVORITE)
                    .setValue(listFavorite)
            }
            for(item in listMusic){
                item.isFavorite = false
            }
            for(item in listMusic){
                if(listFavorite.contains(item)) item.isFavorite = true
            }
            myController.setOnClickFav {
                updateFavorite(it)
            }
            myController.setListMusic(listMusic)
            myController.requestModelBuild()
        }
    }

    private fun initData() {
        getHistoryList()
    }

    private fun getHistoryList() {
        CoroutineScope(Dispatchers.IO).launch {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            val database = Firebase.database
            if(currentUser == null){
                binding.tvNeedLogin.visibility = View.VISIBLE
            }else{
                val ref = database.reference.child(Constants.CHILD_USERS)
                    .child(auth.currentUser?.uid!!)
                    .child(Constants.CHILD_HISTORY)
                ref.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        listMusic.clear()
                        val tempList =  snapshot.getValue<List<MusicItem>>()
                        tempList?.let{
                            for(item in it){
                                listMusic.add(item)
                            }
                        }

                        if(isFirstTime){
                            listMusic.reverse()
                            isFirstTime = false
                        }
                        getFavorite()
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }

    private fun getFavorite() {
        CoroutineScope(Dispatchers.IO).launch {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            val database = Firebase.database
            if(currentUser == null){
                binding.tvNeedLogin.visibility = View.VISIBLE
            }else{
                val ref = database.reference.child(Constants.CHILD_USERS)
                    .child(auth.currentUser?.uid!!)
                    .child(Constants.CHILD_FAVORITE)
                ref.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        listFavorite.clear()
                        val tempList = snapshot.getValue<List<MusicItem>>()
                        tempList?.let{
                            for(item in it){
                                item.isFavorite = false
                                listFavorite.add(item)
                            }
                            for(item in listMusic){
                                if(listFavorite.contains(item)) item.isFavorite = true
                            }
                        }

                        CoroutineScope(Dispatchers.Main).launch{
                            myController.setListMusic(listMusic)
                            myController.requestModelBuild()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }
}














