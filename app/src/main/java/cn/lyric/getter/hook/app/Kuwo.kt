package cn.lyric.getter.hook.app


import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import cn.lyric.getter.hook.BaseHook
import cn.lyric.getter.tool.EventTools
import cn.lyric.getter.tool.HookTools
import cn.lyric.getter.tool.HookTools.eventTools
import cn.xiaowine.xkt.Tool.isNot
import cn.xiaowine.xkt.Tool.isNotNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.enums.MatchType
import java.util.Timer
import java.util.TimerTask


@SuppressLint("StaticFieldLeak")
object Kuwo : BaseHook() {

    init {
        System.loadLibrary("dexkit")
    }


    val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    lateinit var context: Context

    private var timer: Timer? = null
    private var isRunning = false
    private fun startTimer() {
        if (isRunning) return
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (!audioManager.isMusicActive) {
                    eventTools.cleanLyric()
                    stopTimer()
                }
            }
        }, 0, 1000)
        isRunning = true
    }

    private fun stopTimer() {
        if (!isRunning) return
        timer?.cancel()
        isRunning = false
    }


    override fun init() {
        super.init()
        HookTools.getApplication {
            context = it
            loadClassOrNull("cn.kuwo.mod.playcontrol.RemoteControlLyricMgr").isNotNull { clazz ->
                clazz.methodFinder().first { name == "updateLyricText" }.createHook {
                    after { param ->
                        startTimer()
                        eventTools.sendLyric(param.args[0].toString())
                    }
                }
            }.isNot {
                DexKitBridge.create(context.classLoader, false).use { dexKitBridge ->
                    dexKitBridge.isNotNull { bridge ->
                        val result = bridge.findMethodUsingString {
                            usingString = "bluetooth_car_lyric"
                            matchType = MatchType.FULL
                            methodReturnType = "void"
                        }
                        result.forEach { res ->
                            if (!res.declaringClassName.contains("ui") && res.isMethod) {
                                loadClass(res.declaringClassName).methodFinder().first { name == res.name }.createHook {
                                    after { hookParam ->
                                        startTimer()
                                        eventTools.sendLyric(hookParam.args[0].toString())
                                    }
                                }
                                HookTools.openBluetoothA2dpOn()
                            }

                        }
                    }
                }
            }
        }
    }
}