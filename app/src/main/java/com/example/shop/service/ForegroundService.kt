package com.example.shop.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.util.Log.w
import com.example.shop.MainActivity
import com.example.shop.R
import com.example.shop.app.AppSingleton
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.prefs.Preferences

class ForegroundService : Service() {
    var disposable: Disposable? = null
    val preferences = Preferences.getInstance()
    val databaseHelper = AppSingleton.INSTANCE.databaseHelper
    var delayOnBoot:Boolean = false

    var callBack: CallBack? = null
    set(value) {
        field = value
        printUseCase.callBack = value
    }

    private val printUseCase:EOPrintUseCase = EOPrintUseCase(
            SmartposPrinter(),
            Backend(OnlineBackendAPI.create(), preferences),
            callBack,
            ReceiptsRepo(databaseHelper),
            ResultsRepo(databaseHelper),
            ErrorsRepo(databaseHelper),
            OperationsRepo(databaseHelper)
    )


    interface CallBack {
        fun addNewReceipts(list:List<SaleReceiptDB>)
        fun addNewOperations(list:List<OperationDB>)
        fun updateStatus(id:Long, numberInTurn:String, printedAt: Date?)
        fun refreshList()
        fun logOut()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        delayOnBoot = intent?.getBooleanExtra(DELAY_EXTRA,false) ?: false 
        when (intent?.action) {                         
            Consts.ACTION.STARTFOREGROUND_ACTION -> start()
            Consts.ACTION.STOPFOREGROUND_ACTION -> stop()
        }
        return Service.START_STICKY
    }

    private fun start() {
        Intent(this, MainActivity::class.java)
                .setAction(Consts.ACTION.MAIN_ACTION)
                .setFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
                .let {
                    PendingIntent.getActivity(this, 0, it, 0)
                }.let {
                    NotificationCompat.Builder(this,"com.example.shop")
                            .setContentTitle("Super hero")
                            .setTicker("Super hero начал работу")
                            .setContentText("SuperHero запущен")
                            .setSmallIcon(R.mipmap.ic_check_white_48dp)
                            .setContentIntent(it)
                            .setOngoing(true)
                            .build()
                }.also {
                    startForeground(Consts.NOTIFICATION_ID.FOREGROUND_SERVICE, it)
                     startCoreObs()
                }
    }


    @SuppressLint("CheckResult")
    private fun startCoreObs() {
        disposable = printUseCase.coreObs()
                .retryWhen { errors ->
                    errors.flatMap {
                        if (it is UnauthorizedException)
                            return@flatMap Observable.error<Throwable>(it) //don't retry
                        return@flatMap Observable.just(null) //retry
                    }
                }
                .setIOScheduler()
                .subscribe({},{
                    it.printStackTrace()
                    callBack?.logOut()
                    CrashlyticsParamsSendUtils.sendException(it)
                    stop()
                }, {
                    w("COOOOMPLEEETE!!!!")
                })
    }


    private fun stop() {
        disposable?.dispose()
        stopForeground(true)
        stopSelf()
    }


    private val mLocalbinder = MyBinder()
    override fun onBind(intent: Intent): IBinder? {
        // Used only in case of bound services.
        return mLocalbinder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(LOG_TAG, "In onDestroy service")
    }

    inner class MyBinder : Binder() {
        val service: ForegroundService
            get() = this@ForegroundService
    }
    companion object {
        private val LOG_TAG = "ForegroundService"
        const val DELAY_EXTRA= "extra_delay"
    }



}