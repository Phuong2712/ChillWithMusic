package com.example.listentomusic.ui.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Looper.*
import android.provider.SyncStateContract.Constants
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene.Transition
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import androidx.transition.Transition.TransitionListener
import com.bumptech.glide.Glide
import com.example.listentomusic.R
import com.example.listentomusic.databinding.ActivityMainBinding
import com.example.listentomusic.extension.isUserInteractionEnabled
import com.example.listentomusic.model.MusicItem
import com.example.listentomusic.service.MyService
import com.example.listentomusic.ui.fragment.FavoriteFragment
import com.example.listentomusic.ui.fragment.HomeFragment
import com.example.listentomusic.ui.fragment.ProfileFragment
import com.example.listentomusic.util.FormatterUtil
import com.example.listentomusic.util.MediaPlayerCallback
import com.example.listentomusic.util.MediaPlayerUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var myService: MyService? = null
    private var isServiceConnected = false
    private lateinit var auth : FirebaseAuth
    private var curFragment = com.example.listentomusic.constant.Constants.FRAGMENT_HOME
    private var isInStatePlaying = false
    private var isPlayingFavorite = false
    private lateinit var curSong : MusicItem
    private val listBeforeShuffle by lazy { ArrayList<MusicItem>() }
    private val curPlayingList by lazy { ArrayList<MusicItem>() }
    private val favoriteList by lazy { ArrayList<MusicItem>() }
    private var curPosInPlayList = 0
    private var curRepeat = com.example.listentomusic.constant.Constants.REPEAT_NONE
    private val uiUpdateHandler by lazy { Handler(Looper.getMainLooper()) } //xử lý tin nhắn và công việc trên giao diện người dùng

    private val musicReceiver : BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                com.example.listentomusic.constant.Constants.ACTION_PREVIOUS ->{
                    playPreviousSong()
                }

                com.example.listentomusic.constant.Constants.ACTION_NEXT -> {
                    playNextSong()
                }

                com.example.listentomusic.constant.Constants.ACTION_PLAY_PAUSE -> {
                    MediaPlayerUtil.playOrPause()
                    playOrPause()
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as MyService.MyBinder
            myService = myBinder.getMyService()
            isServiceConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            myService = null
            isServiceConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        setupView()
        handleEvent()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initData() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(com.example.listentomusic.constant.Constants.ACTION_PREVIOUS)
        intentFilter.addAction(com.example.listentomusic.constant.Constants.ACTION_PLAY_PAUSE)
        intentFilter.addAction(com.example.listentomusic.constant.Constants.ACTION_NEXT)
        registerReceiver(musicReceiver, intentFilter)

        auth = Firebase.auth
        val curretntUser = auth.currentUser
        if(curretntUser != null){
            getFavorite(curretntUser.uid)
        }
    }

    private fun setupView() {
        replaceFragment(HomeFragment.newInstance())
        binding.rootView.transitionToState(R.id.endGone, 0)
        binding.tvSongNameBottom.isSelected = true
        binding.tvSongAuthorBottom.isSelected = true
    }

    fun handleBackpress(){
        if(isInStatePlaying)
            binding.rootView.transitionToState(R.id.end, 500)
        else if(curFragment == com.example.listentomusic.constant.Constants.FRAGMENT_HOME)
            finish()
        else{
            replaceFragment(HomeFragment.newInstance())
            updateBottomNav(com.example.listentomusic.constant.Constants.FRAGMENT_HOME)
            curFragment = com.example.listentomusic.constant.Constants.FRAGMENT_HOME
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleEvent() {
        // đăng kí sự thay đổi của MontionLayout
        binding.rootView.addTransitionListener(object : MotionLayout.TransitionListener{
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                isInStatePlaying = currentId == R.id.start
                binding.fragmentContainer.isUserInteractionEnabled(!isInStatePlaying)
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }
        })

        binding.sbPlayer.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, process: Int, fromUser: Boolean) {
                binding.tvCurrentTime.text = FormatterUtil.secondsToFormattedTime(process)
                binding.tvTotalTime.text = FormatterUtil.secondsToFormattedTime(seekBar!!.max - process)
            }
            // khi bắt đầu chạm vào thanh truot
            override fun onStartTrackingTouch(p0: SeekBar?) {

            }
            // khi kết thúc chạm vào thanh trượt
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                MediaPlayerUtil.seekTo(seekBar!!.progress)
            }
        })

        binding.btnClosePlayer.setOnClickListener {
            binding.rootView.transitionToState(R.id.end, 500)
        }

        binding.imagePlay.setOnClickListener {
            MediaPlayerUtil.playOrPause()
            playOrPause()
        }

        binding.imgNext.setOnClickListener {
            playNextSong()
        }

        binding.imgBackBottom.setOnClickListener {
            playPreviousSong()
        }

        binding.imgShuffing.setOnClickListener {
            shuffle()
        }

        binding.imgRepeat.setOnClickListener {
            handleRepeat()
        }

        binding.iconFav.setOnClickListener {
            auth = Firebase.auth
            val currentUser = auth.currentUser
            if(currentUser == null){
                Toast.makeText(this@MainActivity,
                    "Bạn cần đăng nhập để sử dụng chức năng này",
                    Toast.LENGTH_LONG).show()
            }else{
                addToFavorite()
            }
        }

        binding.btnHome.setOnClickListener {
            replaceFragment(HomeFragment.newInstance())
            updateBottomNav(com.example.listentomusic.constant.Constants.FRAGMENT_HOME)
            curFragment = com.example.listentomusic.constant.Constants.FRAGMENT_HOME
        }

        binding.btnFav.setOnClickListener {
            replaceFragment(FavoriteFragment.newInstance())
            updateBottomNav(com.example.listentomusic.constant.Constants.FRAGMENT_FAVORITE)
            curFragment = com.example.listentomusic.constant.Constants.FRAGMENT_FAVORITE
        }

        binding.btnProfile.setOnClickListener {
            replaceFragment(ProfileFragment.newInstance())
            updateBottomNav(com.example.listentomusic.constant.Constants.FRAGMENT_PROFILE)
            curFragment = com.example.listentomusic.constant.Constants.FRAGMENT_PROFILE
        }
    }

    private fun addToFavorite(){
        val database = Firebase.database
        val isInList = favoriteList.remove(curSong)
        if(isInList){
            binding.iconFav.setImageResource(R.drawable.ic_favorite_active)
        }else{
            Toast.makeText(this@MainActivity, "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT)
                .show()
            binding.iconFav.setImageResource(R.drawable.ic_favorite)
            favoriteList.add(curSong)
        }
        database.reference.child("users")
            .child(auth.currentUser?.uid!!)
            .child(com.example.listentomusic.constant.Constants.CHILD_FAVORITE)
            .setValue(favoriteList)
    }

    private fun handleRepeat(){
        when(curRepeat){
            com.example.listentomusic.constant.Constants.REPEAT_NONE -> {
                curRepeat = com.example.listentomusic.constant.Constants.REPEAT_LIST
                binding.imgRepeat.setImageResource(R.drawable.ic_repeat_list)
            }

            com.example.listentomusic.constant.Constants.REPEAT_LIST -> {
                curRepeat = com.example.listentomusic.constant.Constants.REPEAT_SONG
                binding.imgRepeat.setImageResource(R.drawable.ic_repeat_one)
            }

            com.example.listentomusic.constant.Constants.REPEAT_SONG -> {
                curRepeat = com.example.listentomusic.constant.Constants.REPEAT_NONE
                binding.imgRepeat.setImageResource(R.drawable.ic_repeat)
            }
        }
    }

    private fun shuffle(){
        if(listBeforeShuffle.isEmpty()){
            listBeforeShuffle.addAll(curPlayingList)
            listBeforeShuffle.remove(curSong)
            listBeforeShuffle.shuffle() // xáo trộn
            listBeforeShuffle.add(curSong)

            curPosInPlayList = listBeforeShuffle.size - 1
            binding.imgShuffing.setColorFilter(Color.parseColor("#3DDC84"))
        }else{
            curPlayingList.clear()
            var count = 0
            for(item in listBeforeShuffle){
                curPlayingList.add(item)
                if(item.id == curSong.id) curPosInPlayList = count
                count ++
            }
            listBeforeShuffle.clear()
            binding.imgShuffing.setColorFilter(Color.parseColor("#FFFFFF"))
        }
    }

    private fun playOrPause(){
        if(MediaPlayerUtil.isPlaying()) binding.imagePlay.setImageResource(R.drawable.ic_pause)
        else binding.imagePlay.setImageResource(R.drawable.ic_play)
        if (MediaPlayerUtil.isPlaying()) binding.imgPlayBottom.setImageResource(R.drawable.ic_pause_bottom)
        else binding.imgPlayBottom.setImageResource(R.drawable.ic_play_bottom)
        startMusicService(curSong)
    }

    private fun startMusicService(musicSelected: MusicItem) {
        val intent = Intent(this@MainActivity, MyService::class.java)
        intent.putExtra(com.example.listentomusic.constant.Constants.EXTRA_SONG_NAME, musicSelected.name)
        intent.putExtra(com.example.listentomusic.constant.Constants.EXTRA_SONG_AUTHOR, musicSelected.author)
        intent.putExtra(com.example.listentomusic.constant.Constants.EXTRA_IMAGE_URL, musicSelected.image)
        intent.putExtra(com.example.listentomusic.constant.Constants.EXTRA_PLAY_OR_PAUSE, MediaPlayerUtil.isPlaying())
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun updateBottomNav(curFragment : Int){
        binding.btnHome.setImageResource(if(curFragment == com.example.listentomusic.constant.Constants.FRAGMENT_HOME) R.drawable.ic_home_active else R.drawable.ic_home)
        binding.btnFav.setImageResource(if(curFragment == com.example.listentomusic.constant.Constants.FRAGMENT_FAVORITE) R.drawable.ic_favorite_home_active else R.drawable.ic_favorite)
        binding.btnProfile.setImageResource(if(curFragment == com.example.listentomusic.constant.Constants.FRAGMENT_PROFILE) R.drawable.ic_profile_active else R.drawable.ic_person)
    }

    private fun updateHistory(musicSelected: MusicItem) {
        auth = Firebase.auth
        val currentUser = auth.currentUser
        if(currentUser == null){

        }else{
            val database = Firebase.database
            val ref = database.reference.child("users")
                .child(auth.currentUser?.uid!!)
                .child(com.example.listentomusic.constant.Constants.CHILD_HISTORY)
            ref.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyList = ArrayList<MusicItem>()
                    val tempList = snapshot.getValue<List<MusicItem>>()
                    tempList?.let{
                        historyList.addAll(it)
                    }
                    historyList.remove(musicSelected)
                    historyList.add(musicSelected)
                    database.reference.child("users")
                        .child(auth.currentUser?.uid!!)
                        .child(com.example.listentomusic.constant.Constants.CHILD_HISTORY)
                        .setValue(historyList)
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

    private fun initMusicData(musicSelected: MusicItem){
        curSong = musicSelected
        val isInList = favoriteList.contains(curSong)
        if (!isInList) binding.iconFav.setImageResource(R.drawable.ic_fav_white)
        else binding.iconFav.setImageResource(R.drawable.ic_favorite_active)
        Glide.with(this@MainActivity).load(curSong.image).error(R.drawable.img_error).into(binding.imgSongPlayer)
        binding.tvSongName.text = curSong.name
        binding.tvSongAuthor.text = curSong.author
        binding.sbPlayer.progress = 0
        binding.sbPlayer.secondaryProgress = 0
        updateViewControlBottom()
    }

    private fun updateViewControlBottom(){
        binding.tvSongNameBottom.text = curSong.name
        binding.tvSongAuthorBottom.text = curSong.author
    }

    private fun playPreviousSong(){
        if(curPlayingList.isEmpty()){
            startPlaying(curSong)
            return
        }
        val newPos = if(curPosInPlayList > 0) curPosInPlayList - 1 else curPlayingList.size - 1
        curPosInPlayList = newPos
        val newSong = curPlayingList[newPos]
        initMusicData(newSong)
        updateHistory(newSong)
        startMusicService(newSong)
        MediaPlayerUtil.playNewOrResume(
            newSong.id!!,
            newSong.src.toString(),
            false,
            object : MediaPlayerCallback{
                override fun onPrepared() {
                    updateViewPlaying()
                    startMusicService(newSong)
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        "can_t_play_this_song",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDone() {
                    onCompleteCurSong()
                }

            }
        )
    }

    private fun playNextSong(){
        if(curPlayingList.isEmpty()){
            startPlaying(curSong)
            return
        }
        val newPos = if(curPosInPlayList < curPlayingList.size - 1) curPosInPlayList + 1 else 0
        curPosInPlayList = newPos
        val newSong = curPlayingList[newPos]
        initMusicData(newSong)
        updateHistory(newSong)
        MediaPlayerUtil.playNewOrResume(
            newSong.id!!,
            newSong.src.toString(),
            false,
            object : MediaPlayerCallback{
                override fun onPrepared() {
                    updateViewPlaying()
                    startMusicService(newSong)
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        "can_t_play_this_song",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDone() {
                    onCompleteCurSong()
                }

            }
        )
    }

    fun startPlaying(musicSelected: MusicItem, isFromFavorite : Boolean = false){
        turnOnMusicPlayer(musicSelected)
        updateHistory(musicSelected)
        if(isFromFavorite){
            isPlayingFavorite = true
            curPlayingList.clear()
            curPlayingList.addAll(favoriteList)
        }else{
            isPlayingFavorite = false
            CoroutineScope(Dispatchers.IO).launch {
                val database = Firebase.database
                val myref = database.reference.child(com.example.listentomusic.constant.Constants.CHILD_MUSICS)
                val query = myref.orderByChild("playList").equalTo(musicSelected.playList)
                query.addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        curPlayingList.clear()
                        var count = 0
                        for(data in snapshot.children){
                            data.getValue<MusicItem>()?.let{
                                if(it.id == musicSelected.id){
                                    curPosInPlayList = count
                                }
                                curPlayingList.add(it)
                                count ++
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
            }
        }
    }

    private fun turnOnMusicPlayer(musicSelected: MusicItem) {
        visibleMusicPlayer()
        initMusicData(musicSelected)
        MediaPlayerUtil.playNewOrResume(
            musicSelected.id!!,
            musicSelected.src.toString(),
            false,
            object : MediaPlayerCallback {
                override fun onPrepared() {
                    updateViewPlaying()
                    startMusicService(musicSelected)
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        "Can't play this song",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDone() {
                    onCompleteCurSong()
                }

            }
        )
    }

    private fun onCompleteCurSong(){
        when(curRepeat){
            com.example.listentomusic.constant.Constants.REPEAT_NONE -> {
                if(curPosInPlayList == curPlayingList.size - 1){
                    binding.sbPlayer.progress = 0
                    binding.imagePlay.setImageResource(R.drawable.ic_play)
                    binding.imgPlayBottom.setImageResource(R.drawable.ic_play_bottom)
                }else playNextSong()
            }

            com.example.listentomusic.constant.Constants.REPEAT_LIST -> playNextSong()

            com.example.listentomusic.constant.Constants.REPEAT_SONG -> {
                curPosInPlayList--
                playNextSong()
            }
        }
    }

    private fun updateViewPlaying(){
        playOrPause()
        binding.sbPlayer.max = MediaPlayerUtil.getDurationInSecond()
        updatePlayingTime()
        updateSeekBar()
        val uiUpdateRunnable = object : Runnable {
            override fun run() {
                if (MediaPlayerUtil.isPlaying()) {
                    updatePlayingTime()
                    updateSeekBar()
                }
                uiUpdateHandler.postDelayed(this, 1000)
            }
        }
        uiUpdateHandler.postDelayed(uiUpdateRunnable, 1000)
    }

    private fun updatePlayingTime(){
        binding.tvCurrentTime.text = MediaPlayerUtil.getCurrentTime()
        binding.tvTotalTime.text = MediaPlayerUtil.getRemainTime()
    }

    private fun updateSeekBar(){
        binding.sbPlayer.progress = MediaPlayerUtil.getCurrentTimeInSecond()
        binding.sbPlayer.secondaryProgress = MediaPlayerUtil.getMediaBufferPercent()
    }

    private fun visibleMusicPlayer(){
        binding.rootView.transitionToState(R.id.start, 500)
    }

    private fun getFavorite(id : String){
        CoroutineScope(Dispatchers.IO).launch {
            val database = Firebase.database
            val ref = database.reference.child("users")
                .child(auth.currentUser?.uid!!)
                .child(com.example.listentomusic.constant.Constants.CHILD_FAVORITE)
            ref.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    favoriteList.clear()
                    val tempList = snapshot.getValue<List<MusicItem>>()
                    tempList?.let{
                        favoriteList.addAll(it)
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        val isInList = favoriteList.contains(curSong)
                        if (!isInList) binding.iconFav.setImageResource(R.drawable.ic_fav_white)
                        else binding.iconFav.setImageResource(R.drawable.ic_favorite_active)

                        if(isPlayingFavorite){
                            curPlayingList.clear()
                            curPlayingList.addAll(favoriteList)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

    private fun replaceFragment(fragment : Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun stopBoundService(){
        if(isServiceConnected){
            unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    private fun stopForegroundService(){
        val intent = Intent(this@MainActivity, MyService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        MediaPlayerUtil.releaseAll()
        stopBoundService()
        stopForegroundService()
        unregisterReceiver(musicReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }
}



