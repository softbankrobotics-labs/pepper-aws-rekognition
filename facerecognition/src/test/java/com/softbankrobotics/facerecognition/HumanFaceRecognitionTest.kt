package com.softbankrobotics.facerecognition

import com.aldebaran.qi.Consumer
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.`object`.human.Human
import com.aldebaran.qi.sdk.`object`.image.TimestampedImage
import io.mockk.*
import junit.framework.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.*
import java.lang.Thread.sleep


class HumanMock {

    fun triggerFaceChanged(timestampedImage: TimestampedImage) {
        facePictureListener.captured.onFacePictureChanged(timestampedImage)
    }
    private val facePictureListener = slot<Human.OnFacePictureChangedListener>()
    val human = mockHuman()

    private fun mockHuman(): Human {
        val human = mockk<Human> {
            every { async() } returns mockk<Human.Async> {
                every { facePicture } returns mockk<Future<TimestampedImage?>> {
                    every { andThenConsume(any()) } returns mockk<Future<Void>>()
                }
                every { addOnFacePictureChangedListener(capture(facePictureListener)) } returns mockk<Future<Void>>()
                every { removeOnFacePictureChangedListener(any()) } returns mockk<Future<Void>>()
            }
        }
        return human
    }
}

fun mockAWSFaceRecognition(collectionId: String, faceId: String, sleepTime: Long = 0): AWSFaceRecognition {
    val awsFaceRecognition = mockk<AWSFaceRecognition> {
        every { recognizeFaceAsync(collectionId, faceImage = any()) } returns mockk<Future<String?>> {
            val callback = slot<Consumer<String?>>()
            every { andThenConsume(capture(callback)) } answers {
                sleep(sleepTime)
                callback.captured.consume(faceId)
                mockk<Future<Void>>()
            }
        }
    }
    return awsFaceRecognition
}

class HumanFaceRecognitionTest {

    @Test
    fun testSimpleRecognition() {

        // Create the HumanFaceRecognition object
        val humanFaceRecognition = HumanFaceRecognition()
        humanFaceRecognition.collectionId = "dummy"

        val humanMock = HumanMock()

        // Verify it does not know the 'human' faceId
        assertEquals(null, humanFaceRecognition.getFaceId(humanMock.human))

        // Add a listener to be notified when faceId is available for the human
        val listener = spyk(object: OnFaceIdAvailableListener {
            override fun onFaceIdAvailable(faceId: String) {
            }
        })
        humanFaceRecognition.addOnFaceIdAvailableListener(humanMock.human, listener)

        // Set AWS lib to return id 'face1'
        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face1")
        // Triggers the facePictureChanged
        humanMock.triggerFaceChanged(mockk<TimestampedImage>())

        // Set AWS lib to return id 'face2'
        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face2")
        // Triggers the facePictureChanged
        humanMock.triggerFaceChanged(mockk<TimestampedImage>())

        // Verify the onFaceIdAvailable was called
        verify { listener.onFaceIdAvailable(faceId = "face1") }
        // Verify the onFaceIdAvailable was NOT called with "face2" - listener is called only the first time
        verify(inverse=true) { listener.onFaceIdAvailable(faceId = "face2") }

        // Verify the 'human' faceId is now "toto"
        assertEquals("face1", humanFaceRecognition.getFaceId(humanMock.human))
    }


    @Test
    fun testMultipleHumanRecognition() {
        // Goal:
        // Recognize 3 humans

        // Create the HumanFaceRecognition object
        val humanFaceRecognition = HumanFaceRecognition()
        humanFaceRecognition.collectionId = "dummy"

        val humanMock1 = HumanMock()
        val humanMock2 = HumanMock()
        val humanMock3 = HumanMock()

        // Verify it does not know the 'human' faceId
        assertEquals(null, humanFaceRecognition.getFaceId(humanMock1.human))
        assertEquals(null, humanFaceRecognition.getFaceId(humanMock2.human))
        assertEquals(null, humanFaceRecognition.getFaceId(humanMock3.human))

        // Add a listener to be notified when faceId is available for the human
        val listener = spyk(object: OnFaceIdAvailableListener {
            override fun onFaceIdAvailable(faceId: String) {
            }
        })
        humanFaceRecognition.addOnFaceIdAvailableListener(humanMock1.human, listener)
        humanFaceRecognition.addOnFaceIdAvailableListener(humanMock2.human, listener)
        humanFaceRecognition.addOnFaceIdAvailableListener(humanMock3.human, listener)

        // Set AWS lib to return id 'face1'
        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face1")
        // Triggers the facePictureChanged
        humanMock1.triggerFaceChanged(mockk<TimestampedImage>())

        // Set AWS lib to return id 'face2'
        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face2")
        // Triggers the facePictureChanged
        humanMock2.triggerFaceChanged(mockk<TimestampedImage>())

        // Set AWS lib to return id 'face2'
        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face3")
        // Triggers the facePictureChanged
        humanMock3.triggerFaceChanged(mockk<TimestampedImage>())

        // Verify the onFaceIdAvailable was called
        verifyOrder {
            listener.onFaceIdAvailable(faceId = "face1")
            listener.onFaceIdAvailable(faceId = "face2")
            listener.onFaceIdAvailable(faceId = "face3")
        }

        // Verify the 'human' faceId is now "toto"
        assertEquals("face1", humanFaceRecognition.getFaceId(humanMock1.human))
        assertEquals("face2", humanFaceRecognition.getFaceId(humanMock2.human))
        assertEquals("face3", humanFaceRecognition.getFaceId(humanMock3.human))
    }


    @Test
    fun testCallingRemoveListenerFromFaceIdAvailableListener() {
        // Goal:
        // Try to call the removeAllOnFaceIdAvailableListerner in the faceIdAvailable listener, and
        // see that it works.

        // Create the HumanFaceRecognition object
        val humanFaceRecognition = HumanFaceRecognition()
        humanFaceRecognition.collectionId = "dummy"

        val humanMock1 = HumanMock()
        var stepReached = false

        // Add a listener to be notified when faceId is available for the human
        val listener = spyk(object: OnFaceIdAvailableListener {
            override fun onFaceIdAvailable(faceId: String) {
                humanFaceRecognition.removeAllOnFaceIdAvailableListerner(humanMock1.human)
                // Verify there is no deadlock & we reach this line
                stepReached = true
            }
        })

        humanFaceRecognition.addOnFaceIdAvailableListener(humanMock1.human, listener)
        // Set AWS lib to return id 'face1'
        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face1")
        // Triggers the facePictureChanged
        humanMock1.triggerFaceChanged(mockk<TimestampedImage>())

        assertEquals(true, stepReached)
    }

    @Test
    fun testMultipleAWSAnswers() {
        // Goal:
        // Simulate that the human face is extracted many time for same human, and that AWS reply
        // a different face id each time. Moreover, simulate that AWS takes 500ms to reply.
        // Verify in the end there is only one faceId answered for that human.

        runBlocking {
            // Create the HumanFaceRecognition object
            val humanFaceRecognition = HumanFaceRecognition()
            humanFaceRecognition.collectionId = "dummy"

            val humanMock1 = HumanMock()
            val listener = spyk(object: OnFaceIdAvailableListener {
                override fun onFaceIdAvailable(faceId: String) {
                }
            })
            humanFaceRecognition.addOnFaceIdAvailableListener(humanMock1.human, listener)

            // We want our code to run on 10 threads
            val scope = CoroutineScope(newFixedThreadPoolContext(10, "pool"))
            scope.launch {
                val coroutines = 1.rangeTo(100).map {
                    //create 100 coroutines (light-weight threads).
                    launch {
                        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face$it", 500)
                        humanMock1.triggerFaceChanged(mockk<TimestampedImage>())
                    }
                }
                coroutines.forEach { corotuine ->
                    corotuine.join() // wait for all coroutines to finish their jobs.
                }
            }.join()

            // Verify the onFaceIdAvailable was called
            verify(exactly=1) { listener.onFaceIdAvailable(faceId = any()) }
        }
    }

    @Test
    fun testMultipleAWSAnswersWithRemoveListener() {
        runBlocking {
            // Create the HumanFaceRecognition object
            val humanFaceRecognition = HumanFaceRecognition()
            humanFaceRecognition.collectionId = "dummy"

            val humanMock1 = HumanMock()
            val listener = spyk(object: OnFaceIdAvailableListener {
                override fun onFaceIdAvailable(faceId: String) {
                    humanFaceRecognition.removeAllOnFaceIdAvailableListerner(humanMock1.human)
                }
            })
            humanFaceRecognition.addOnFaceIdAvailableListener(humanMock1.human, listener)

            // We want our code to run on 10 threads
            val scope = CoroutineScope(newFixedThreadPoolContext(10, "pool"))
            scope.launch {
                val coroutines = 1.rangeTo(100).map {
                    //create 100 coroutines (light-weight threads).
                    launch {
                        humanFaceRecognition.faceRecognition = mockAWSFaceRecognition("dummy", "face$it", 500)
                        humanMock1.triggerFaceChanged(mockk<TimestampedImage>())
                    }
                }
                coroutines.forEach { corotuine ->
                    corotuine.join() // wait for all coroutines to finish their jobs.
                }
            }.join()

            // Verify the onFaceIdAvailable was called
            verify(exactly=1) { listener.onFaceIdAvailable(faceId = any()) }
        }
    }
}