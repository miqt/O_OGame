package com.miqt.o_ogame.presenter

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.miqt.miwrenchlib.MNetUtils
import com.miqt.o_ogame.cfg
import com.miqt.o_ogame.entity.Data
import com.miqt.o_ogame.entity.Device
import com.miqt.o_ogame.net.AUdpReceive
import com.miqt.o_ogame.net.UdpSend
import com.miqt.o_ogame.view.IDevListView
import java.util.*
import kotlin.collections.ArrayList

class PublishPresenter(private val dlview: IDevListView, private val mContext: Context) : IUdpPublishPresenter {

    private val mReceiver = ArrayList<IUdpPublishPresenter.PublishReceiver>()
    private val mDevList = ArrayList<Device>()
    private val udpsend = UdpSend()

    init {

    }

    private val udpReceiver = object : AUdpReceive() {
        override fun onRecerver(message: String) {
            val data = Gson().fromJson(message, Data::class.java)
            when (data.what) {
                Data.TYPE_DEVICE_INFO -> {
                    val devInfo: Device? = Gson().fromJson<Data<Device>>(message, object : TypeToken<Data<Device>>() {}.type).data
                    if (devInfo != null) {
                        if (mDevList.size == 0) {
                            addDivList(devInfo)
                        }
                        if (!mDevList.any { it.ip == devInfo.ip }) {
                            addDivList(devInfo)
                        }
                    }
                }
            }
            //
            for (i in mReceiver) {
                i.onReverver(message)
            }
        }
    }

    private fun addDivList(devInfo: Device) {
        mDevList.add(devInfo)
        val list = ArrayList<String>()
        mDevList.mapTo(list) { it.ip + it.name }
        dlview.notifyDataSetChanged(list)
    }

    private lateinit var mTimer: Timer

    override fun start(time: Long) {
        Thread(Runnable {
            udpReceiver.join()
        }).start()
        mTimer = Timer()
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                val name = Build.MODEL
                val ip = MNetUtils.getIPAddress(mContext)
                val port = Build.MODEL
                val dev = Device(name, ip, port).copy()
                val data = Data(Data.TYPE_DEVICE_INFO, dev)
                val str = Gson().toJson(data)
                publish(str)
            }
        }, time, cfg.rhythm)
    }

    override fun stop() {
        mTimer.purge()
        mTimer.cancel()
        udpReceiver.close()
        udpsend.close()
    }

    override fun publish(text: String) {
        udpsend.send(text)
    }

    override fun addPublishReceiver(reveiver: IUdpPublishPresenter.PublishReceiver) {
        mReceiver.add(reveiver)
    }

    override fun getCurrDev(): List<Device> = mDevList


}