package com.example.listentomusic.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.provider.SyncStateContract.Constants
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.example.listentomusic.R
import com.example.listentomusic.app.MyApplication
import com.example.listentomusic.receiver.MyReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyService : Service() {
    private val myBinder = MyBinder();
    //Binder(): hỗ trợ liên kết các thành phần trong ứng dụng
    class MyBinder : Binder(){
        fun getMyService() : MyService{
            return MyService()
        }
    }

    override fun onBind(p0: Intent?): IBinder{
        return myBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            pushNotification(
                intent.getStringExtra(com.example.listentomusic.constant.Constants.EXTRA_SONG_NAME),
                intent.getStringExtra(com.example.listentomusic.constant.Constants.EXTRA_SONG_AUTHOR),
                intent.getStringExtra(com.example.listentomusic.constant.Constants.EXTRA_IMAGE_URL),
                intent.getBooleanExtra(com.example.listentomusic.constant.Constants.EXTRA_PLAY_OR_PAUSE, true)
            )
        }
        return START_NOT_STICKY
    }

    private fun pushNotification(
        songName: String?,
        songAuthor: String?,
        image: String?,
        isPlaying: Boolean) {
        val notificationIntent = this.packageManager.getLaunchIntentForPackage(packageName)
        // pedingIntent: lưu trữ và thực hiện sau đó
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        CoroutineScope(Dispatchers.IO).launch {
            val icon = try {
                Glide.with(this@MyService)
                    .asBitmap()
                    .load(image)
                    .submit(512, 512)
                    .get()
            } catch (e: Exception){
                // decodeResourse: giải mã hình ảnh thành bitmap
                BitmapFactory.decodeResource(this@MyService.resources, R.drawable.img_error)
            }

            CoroutineScope(Dispatchers.Main).launch {
                // NotificationCompat: xây dựng thông báo và cung cấp các tùy chọn tùy chỉnh như
                // biểu tượng, tiêu đề, nội dung, âm thanh, độ ưu tiên, hành động, hình ảnh
                val notification = NotificationCompat.Builder(this@MyService, MyApplication.CHANNEL_ID)
                    .setContentTitle(songName)
                    .setContentTitle(songAuthor)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(false) // xử lí thông báo có xoá bởi user dc hay k
                    .setContentIntent(pendingIntent)
                    //setContentIntent: sự kiện khi người dùng nhấn vào nội dung của thông báo.
                    .setLargeIcon(icon)
                    .setColor(Color.parseColor("#FFFFFF"))
                    .addAction(R.drawable.ic_previous_song, "Previous", getPendingIntent(this@MyService, com.example.listentomusic.constant.Constants.ACTION_PREVIOUS))
                    .addAction(if (isPlaying) R.drawable.ic_pause_bottom else R.drawable.ic_play_bottom, "Pause", getPendingIntent(this@MyService, com.example.listentomusic.constant.Constants.ACTION_PLAY_PAUSE))
                    .addAction(R.drawable.ic_next, "Next", getPendingIntent(this@MyService, com.example.listentomusic.constant.Constants.ACTION_NEXT))
                    .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0,1,2))
                    .build()

                startForeground(134,notification)
            }
        }
    }

    private fun getPendingIntent(context : Context, action:String): PendingIntent? {
        val intent = Intent(context, MyReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(
            context.applicationContext,
            com.example.listentomusic.constant.Constants.REQUEST_CODE_CONTROL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}





