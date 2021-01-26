package com.softbankrobotics.facerecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.Promise
import com.aldebaran.qi.sdk.`object`.image.TimestampedImage
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.CreateCollectionRequest
import com.amazonaws.services.rekognition.model.DeleteCollectionRequest
import com.amazonaws.services.rekognition.model.DeleteFacesRequest
import com.amazonaws.services.rekognition.model.FaceMatch
import com.amazonaws.services.rekognition.model.Image
import com.amazonaws.services.rekognition.model.IndexFacesRequest
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 *  Use this class to recognize faces of users detected by Pepper.
 */
class AWSFaceRecognition(
    context: Context,
    identityPoolId: String,
    logins: Map<String, String>,
    region: String,
    asyncRequestPoolSize: Int = 1
) {
    // Initialize the Amazon Cognito credentials provider
    private var credentialsProvider = CognitoCachingCredentialsProvider(
        context, identityPoolId, Regions.fromName(region)).apply {
        setLogins(logins)
    }
    private val rekognitionClient = AmazonRekognitionClient(credentialsProvider)
        .apply { setRegion(Region.getRegion(region)) }

    val executor by lazy {
        ThreadPoolExecutor(
            asyncRequestPoolSize,
            asyncRequestPoolSize,
            60, TimeUnit.SECONDS,
            SynchronousQueue<Runnable>()
        )
    }

    fun recognizeFaceAsync(collectionId: String, faceImage: TimestampedImage): Future<String?> {
        val image = faceImage.getBitmap()
        return if (image != null) recognizeFaceAsync(collectionId, image) else Future.of(null)
    }

    fun recognizeFaceAsync(collectionId: String, face: Bitmap): Future<String?> {
        val promise = Promise<String?>()
        try {
            executor.submit {
                try {
                    val faceId = recognizeFace(collectionId, face)
                    promise.setValue(faceId)
                } catch (exception: Exception) {
                    promise.setError(exception.message)
                }
            }
        } catch (e: RejectedExecutionException) {
            promise.setError("Too many recognition request already running.")
        }
        return promise.future
    }

    fun recognizeFace(collectionId: String, face: TimestampedImage): String? {
        val image = face.getBitmap()
        return if (image != null) recognizeFace(collectionId, image) else null
    }

    // Recognize a face and return face id
    fun recognizeFace(collectionId: String, face: Bitmap): String? {
        Log.d(TAG, "recognizeFace($collectionId)")
        val facePicture = addWhiteBorder(face, 20)
        val image = facePicture.toAwsImage()

        val faceMatchList = recognizeFaces(collectionId, image)
        Log.e(TAG, "Found " + faceMatchList.size + " matchs")

        if (faceMatchList.size == 0)
            return learnFace(collectionId, image)

        if (faceMatchList.size > 1)
            Log.e(TAG, "More than 1 face recognized. This is weird")

        val faceMatch = faceMatchList[0]
        Log.i(TAG, "Confidence: " + faceMatch.similarity!!)
        Log.i(TAG, "FaceId: " + faceMatch.face.faceId)
        return if (faceMatch.similarity > 70) {
            faceMatch.face.faceId
        } else null
    }

    private fun createCollection(collectionId: String) {
        Log.d(TAG, "createCollection($collectionId)")
        rekognitionClient.createCollection(CreateCollectionRequest(collectionId))
    }

    fun deleteFaces(collectionId: String, facesId: List<String>) {
        Log.d(TAG, "deleteFaces($collectionId)")
        rekognitionClient.deleteFaces(DeleteFacesRequest(collectionId, facesId))
    }
    fun deleteCollection(collectionId: String) {
        Log.d(TAG, "deleteCollection($collectionId)")
        rekognitionClient.deleteCollection(DeleteCollectionRequest(collectionId))
    }

    private fun recognizeFaces(collectionId: String, image: Image): List<FaceMatch> {
        Log.d(TAG, "recognizeFaces($collectionId)")
        val res = rekognitionClient.searchFacesByImage(
            SearchFacesByImageRequest(
                collectionId,
                image
            )
        )
        return res.faceMatches
    }

    private fun learnFace(collectionId: String, image: Image): String? {
        Log.d(TAG, "learnFace($collectionId)")
        val indexFacesResult = rekognitionClient.indexFaces(IndexFacesRequest(collectionId, image))
        val faceRecordList = indexFacesResult.faceRecords
        if (faceRecordList.size == 0) {
            return null
        }
        if (faceRecordList.size > 1) {
            Log.e(TAG, "More than 1 face learned. This is weird.")
            return null
        }
        val learnedFace = faceRecordList[0].face
        return learnedFace.faceId
    }

    private fun addWhiteBorder(bmp: Bitmap, borderSize: Int): Bitmap {
        val bmpWithBorder =
            Bitmap.createBitmap(bmp.width + borderSize * 2, bmp.height + borderSize * 2, bmp.config)
        val canvas = Canvas(bmpWithBorder)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bmp, borderSize.toFloat(), borderSize.toFloat(), null)
        return bmpWithBorder
    }
}


