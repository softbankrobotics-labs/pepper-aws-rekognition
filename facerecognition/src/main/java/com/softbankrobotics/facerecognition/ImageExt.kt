package com.softbankrobotics.facerecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aldebaran.qi.sdk.`object`.image.TimestampedImage
import com.amazonaws.services.rekognition.model.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


fun TimestampedImage.getBitmap(): Bitmap? {
    val buffer = image.data
    buffer.rewind()
    val pictureBufferSize = buffer.remaining()
    val pictureArray = ByteArray(pictureBufferSize)
    buffer.get(pictureArray)
    return BitmapFactory.decodeByteArray(pictureArray, 0, pictureBufferSize)
}

fun Bitmap.toAwsImage(): Image {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, bos)
    val byte_arr = bos.toByteArray()
    return Image().withBytes(ByteBuffer.wrap(byte_arr))
}
