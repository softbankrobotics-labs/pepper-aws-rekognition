package com.softbankrobotics.pepperrecognition

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.human.Human
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.softbankrobotics.facerecognition.HumanFaceRecognition
import com.softbankrobotics.facerecognition.OnFaceIdAvailableListener
import com.softbankrobotics.facerecognition.getBitmap
import com.softbankrobotics.facerecognition.TAG
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {

    private var currentEngagedHuman: Human? = null
    private var qiContext: QiContext? = null
    private var currentUserId: String? = null
    private val humanUnknownImage by lazy {
        BitmapFactory.decodeResource(
            resources,
            R.drawable.humanwho
        )
    }

    private val userNumbers = hashMapOf<String, Int>()
    private var numberCounter = 0
    private var currentSay = Future.of<Void>(null)

    private val humanFaceReco by lazy {
        HumanFaceRecognition(
            applicationContext,
            BuildConfig.AWS_IDENTITY_POOL,
            hashMapOf(BuildConfig.AWS_USER_POOL_PROVIDER_NAME to BuildConfig.AWS_USER_ID_TOKEN),
            BuildConfig.AWS_REGION,
            BuildConfig.AWS_COLLECTION_ID
        )
    }

    fun assignUserNumber(faceId: String): Int {
        numberCounter += 1
        userNumbers[faceId] = numberCounter
        saveUserNumber(faceId, numberCounter)
        return numberCounter
    }

    fun onFaceIdAvailable(human: Human, faceId: String) {
        runOnUiThread {
            // If we are still talking about same human:
            if (currentEngagedHuman == human) {
                Log.i(TAG, "Found face $faceId")
                val newlyAssignedNumber = !userNumbers.containsKey(faceId)
                val userNumber = userNumbers[faceId] ?: assignUserNumber(faceId)
                humanstate.text = "Human engaged: $userNumber"

                var greet = ""
                if (newlyAssignedNumber) {
                    greet = "Hello! This is the first time I have seen you, I will assign you number $userNumber."
                } else {
                    greet = "Hello number $userNumber ! Nice to see you again!"
                }

                if (currentSay.isDone) {
                    // Do not stacks several Say actions
                    SayBuilder.with(qiContext).withText(greet).buildAsync().andThenConsume { say ->
                        currentSay = say.async().run()
                    }
                }
            }
        }
    }

    fun humanDisengaged() {
        runOnUiThread {
            Log.i(TAG, "Human disengaged")
            currentEngagedHuman?.let {
                it.async().removeAllOnFacePictureChangedListeners()
                humanFaceReco.removeAllOnFaceIdAvailableListener(it)
            }
            currentEngagedHuman = null
            currentUserId = null
            humanstate.text = "No human"
            profile_image.setImageBitmap(humanUnknownImage)
            profile_image.borderColor = getColor(R.color.lightBlue)

        }
    }

    fun newHumanEngaged(human: Human) {
        Log.i(TAG, "new Human engaged")

        runOnUiThread {
            currentEngagedHuman?.let {
                it.async().removeAllOnFacePictureChangedListeners()
                humanFaceReco.removeAllOnFaceIdAvailableListener(it)
            }
            currentEngagedHuman = human
            currentUserId = null
            humanstate.text = "Human engaged: Unknown"
            profile_image.setImageBitmap(humanUnknownImage)
            human.async().facePicture.andThenConsume {
                it.getBitmap()?.let {
                    runOnUiThread {
                        profile_image.setImageBitmap(it)
                        profile_image.borderColor = getColor(R.color.green)
                    }
                }
            }
            human.async().addOnFacePictureChangedListener {
                it.getBitmap()?.let {
                    runOnUiThread {
                        profile_image.setImageBitmap(it)
                        profile_image.borderColor = getColor(R.color.green)
                    }
                }
            }
            humanFaceReco.addOnFaceIdAvailableListener(human, object : OnFaceIdAvailableListener {
                override fun onFaceIdAvailable(faceId: String) {
                    onFaceIdAvailable(human, faceId)
                }
            })
        }
    }

    fun onEngagedHumanChanged(human: Human?) {
        Log.i(TAG, "onEngagedHumanChanged")

        runOnUiThread {
            if (human == null) {
                humanDisengaged()
            } else if (human == currentEngagedHuman) {
                Log.i(TAG, "Human already engaged")
            } else {
                newHumanEngaged(human)
            }
        }
    }

    private fun loadSavedNumbers() {
        val allEntries = getPreferences(Context.MODE_PRIVATE).all
        for (entry in allEntries.entries) {
            val number = entry.value as Int
            userNumbers[entry.key] = number
            if (numberCounter < number)
                numberCounter = number
        }
    }

    private fun saveUserNumber(id: String, number: Int) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(id, number)
        editor.apply()
    }

    override fun onRobotFocusGained(qiContext: QiContext) {
        this.qiContext = qiContext
        qiContext.humanAwareness.addOnEngagedHumanChangedListener(::onEngagedHumanChanged)
        onEngagedHumanChanged(qiContext.humanAwareness.engagedHuman)
    }

    override fun onRobotFocusLost() {
        qiContext?.humanAwareness?.removeAllOnEngagedHumanChangedListeners()
        currentEngagedHuman?.removeAllOnFacePictureChangedListeners()
    }

    override fun onRobotFocusRefused(reason: String?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activateImmersiveMode()
        QiSDK.register(this, this)
        loadSavedNumbers()
    }

    override fun onDestroy() {
        super.onDestroy()
        QiSDK.unregister(this)
    }

    fun activateImmersiveMode() {
        window.decorView.apply {
            systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
}
