package com.example.shop.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import com.example.shop.R
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*



public class SoundPoolPlayer private constructor() {
    private lateinit var shortPlayer: SoundPool
    private val sounds: HashMap<Int, Any> = HashMap()
    private var initObservable: Subject<Boolean> = PublishSubject.create()
    private var soundsLoaded: Boolean = false

    private fun preparePlayer (context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shortPlayer = SoundPool.Builder().setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                    .build()
        } else {
            this.shortPlayer = SoundPool(4, AudioManager.STREAM_NOTIFICATION, 0)
        }

        shortPlayer.setOnLoadCompleteListener{ _, _, _  ->
                soundsLoaded = true
                initObservable.onNext(soundsLoaded)
        }
        sounds[R.raw.fail] = this.shortPlayer.load(context, R.raw.fail, 1)
        sounds[R.raw.succ] = this.shortPlayer.load(context, R.raw.succ, 1)
    }

    public fun playShortResource(piResource: Int) {
        val iSoundId = sounds[piResource] as Int
        if(soundsLoaded) {
            this.shortPlayer.play(iSoundId, 0.30f, 0.30f, 0, 0, 1f)
        } else {
            initObservable.subscribe { _ -> this.shortPlayer.play(iSoundId, 0.30f, 0.30f, 0, 0, 1f) }
        }
    }

    companion object {
        @Volatile
        private var instance: SoundPoolPlayer? = null

        fun getInstance(context: Context): SoundPoolPlayer {
            var localInstance = instance
            if (localInstance != null) return localInstance

            return synchronized(this) {
                localInstance = instance
                if (localInstance == null) {
                    instance = SoundPoolPlayer().also { it.preparePlayer(context) }
                    localInstance = instance
                }
                localInstance!!
            }
        }
    }
}