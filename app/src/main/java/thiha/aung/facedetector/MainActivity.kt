package thiha.aung.facedetector

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.mindorks.paracamera.Camera
import kotlinx.android.synthetic.main.activity_main.*


private const val PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    private lateinit var camera: Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init Firebase
        FirebaseApp.initializeApp(this)

        // Configure Camera
        camera = Camera.Builder()
            .resetToCorrectOrientation(true)//1
            .setTakePhotoRequestCode(Camera.REQUEST_TAKE_PHOTO)//2
            .setDirectory("pics")//3
            .setName("face_${System.currentTimeMillis()}")//3
            .setImageFormat(Camera.IMAGE_JPEG)//4
            .setCompression(75)//5
            .build(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.deleteImage()
    }

    fun takePicture(view: View) {
        if (!hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            !hasPermission(android.Manifest.permission.CAMERA)) {
            // If do not have permissions then request it
            requestPermissions()
        } else {
            // else all permissions granted, go ahead and take a picture using camera
            try {
                camera.takePicture()
            } catch (e: Exception) {
                // Show a toast for exception
                Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            Snackbar.make(mainLayout, R.string.permission_message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.OK) {
                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
                }
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            return
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this,
            permission) == PackageManager.PERMISSION_GRANTED

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        camera.takePicture()
                    } catch (e: Exception) {
                        Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                            Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
                val bitmap = camera.cameraBitmap
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    runFaceContourDetection(bitmap)
                } else {
                    Toast.makeText(this.applicationContext, getString(R.string.picture_not_taken),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun displayResultMessage(isSmiling: Boolean) {
        responseCardView.visibility = View.VISIBLE

        if (isSmiling) {
            responseCardView.setCardBackgroundColor(Color.GREEN)
            responseTextView.text = getString(R.string.smiling)
        } else {
            responseCardView.setCardBackgroundColor(Color.RED)
            responseTextView.text = getString(R.string.not_smiling)
        }
    }

    private fun runFaceContourDetection(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE) // or FirebaseVisionFaceDetectorOptions.FAST
//            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)

        detector.detectInImage(image)
            .addOnSuccessListener { faces ->

                progressBar.visibility = View.GONE

                processFaceContourDetectionResult(faces)

            }
            .addOnFailureListener { error ->
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(this.applicationContext, getString(R.string.error),
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun processFaceContourDetectionResult(faces: List<FirebaseVisionFace>) { // Task completed successfully
        if (faces.isEmpty()) {
            Toast.makeText(this.applicationContext, getString(R.string.no_face_found),
                Toast.LENGTH_SHORT).show()
        } else {
            graphicOverlay.clear()
            for (i in faces.indices) {
                val face = faces[i]
                val faceGraphic = FaceContourGraphic(graphicOverlay)
                graphicOverlay.add(faceGraphic)
                faceGraphic.updateFace(face)
            }
        }
    }


    /*
    faces.firstOrNull()?.also { face ->

                    // Classification
                    val smilingProbability = face.smilingProbability
                    val isSmiling = smilingProbability > 50
                    displayResultMessage(isSmiling)


                    val leftMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT)
                    val bottomMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)
                    val rightMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT)


                    val lowerLipBottomContours = face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).points

                    /*val leftEyeOpenProbability = face.leftEyeOpenProbability
                    val rightEyeOpenProbability = face.rightEyeOpenProbability*/

                    /*// Landmarks
                    val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
                    val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
                    val nose = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE)
                    val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                    val rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)


                    // Contours
                    val faceContours = face.getContour(FirebaseVisionFaceContour.FACE).points
                    val leftEyebrowTopContours = face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).points
                    val leftEyebrowBottomContours = face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM).points
                    val rightEyebrowTopContours = face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP).points
                    val rightEyebrowBottomContours = face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM).points
                    val leftEyeContours = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).points
                    val rightEyeContours = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).points
                    val upperLipTopContours = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).points
                    val upperLipBottomContours = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).points
                    val lowerLipTopContours = face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).points
                    val lowerLipBottomContours = face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).points
                    val noseBridgeContours = face.getContour(FirebaseVisionFaceContour.NOSE_BRIDGE).points
                    val noseBottomContours = face.getContour(FirebaseVisionFaceContour.NOSE_BOTTOM).points*/
                }
    * */

}