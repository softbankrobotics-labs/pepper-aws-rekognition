package com.softbankrobotics.facerecognition

import android.content.Context
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.`object`.human.Human
import com.aldebaran.qi.sdk.`object`.image.TimestampedImage

interface OnFaceIdAvailableListener {
    fun onFaceIdAvailable(faceId: String)
}

class HumanFaceRecognition internal constructor(){

    lateinit var collectionId: String
    lateinit var faceRecognition: AWSFaceRecognition

    constructor(context: Context, identityPoolId: String, logins: Map<String, String>, region: String, collectionId: String): this() {
        this.collectionId = collectionId
        this.faceRecognition = AWSFaceRecognition(context, identityPoolId, logins, region)
    }

    private data class HumanData(
        var faceId: String?,
        val facePictureChangedListener: OnFacePictureChangedListener,
        val faceIdAvailableListeners: MutableList<OnFaceIdAvailableListener>
    );
    private val humanDatas = hashMapOf<Human, HumanData>()

    private inner class OnFacePictureChangedListener(val human: Human): Human.OnFacePictureChangedListener {
        override fun onFacePictureChanged(facePicture: TimestampedImage?) {
            if ((facePicture != null) && (humanDatas[human]?.faceId == null)) {
                faceRecognition.recognizeFaceAsync(collectionId, facePicture).andThenConsume { faceId ->
                    if (faceId != null) {
                        // User is recognised
                        synchronized(humanDatas) {
                            val humanData = humanDatas[human]
                            if (humanData != null && humanData.faceId == null) {
                                humanData.faceId = faceId
                                val listenerList = humanData.faceIdAvailableListeners.toMutableList()
                                // Clear the listeners, face id was found
                                humanData.faceIdAvailableListeners.clear()
                                human.async().removeOnFacePictureChangedListener(humanData.facePictureChangedListener)
                                listenerList
                            }
                            else
                                listOf<OnFaceIdAvailableListener>()
                        }.forEach { listener ->
                            listener.onFaceIdAvailable(faceId)
                        }
                    }
                }
            }
        }
    }

    fun getFaceId(human: Human): String? {
        return synchronized(humanDatas) { humanDatas[human]?.faceId }
    }

    fun addOnFaceIdAvailableListener(human: Human, listener: OnFaceIdAvailableListener) {
        var setListeners = false
        val (faceId, faceListener, dummy) = synchronized(humanDatas) {
            val humanData = humanDatas[human]
            if (humanData != null) {
                if (humanData.faceId == null) {
                    if (humanData.faceIdAvailableListeners.size == 0)
                        setListeners = true
                    humanData.faceIdAvailableListeners.add(listener)
                }
            }
            else {
                val faceListener = OnFacePictureChangedListener(human)
                humanDatas[human] = HumanData(null, faceListener, mutableListOf(listener))
                setListeners = true
            }
            humanDatas[human]!!
        }
        if (faceId != null)
            listener.onFaceIdAvailable(faceId)
        else {
            if (setListeners) {
                human.async().facePicture.andThenConsume { faceListener.onFacePictureChanged(it) }
                human.async().addOnFacePictureChangedListener(faceListener)
            }
        }
    }

    fun removeOnFaceIdAvailableListener(human: Human, listener: OnFaceIdAvailableListener): Future<Void> {
        synchronized(humanDatas) {
            humanDatas[human]?.let {
                it.faceIdAvailableListeners.remove(listener)
                if (it.faceIdAvailableListeners.size == 0) {
                    return human.async().removeOnFacePictureChangedListener(it.facePictureChangedListener)
                }
            }
        }
        return Future.of(null)
    }

    fun removeAllOnFaceIdAvailableListener(human: Human): Future<Void> {
        synchronized(humanDatas) {
            humanDatas[human]?.let {
                it.faceIdAvailableListeners.clear()
                return human.async().removeOnFacePictureChangedListener(it.facePictureChangedListener)
            }
        }
        return Future.of(null)
    }

}

