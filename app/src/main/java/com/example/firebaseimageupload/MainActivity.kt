package com.example.firebaseimageupload

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.collage.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// for logging purposes
private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

    private lateinit var imageButton1: ImageButton
    private lateinit var uploadImageFab: FloatingActionButton
    private lateinit var uploadProgressBar: ProgressBar

    private lateinit var mainView: View

    private var newPhotoPath: String? = null
    private var visibleImagePath: String? = null
    private var imageFileName: String? = null
    private var photoUri: Uri? = null

    // for saving on rotation
    private val NEW_PHOTO_PATH_KEY = "new photo path key"
    private val VISIBLE_IMAGE_PATH_KEY = "visible image path key"

    private val storage = Firebase.storage

    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result -> handleImage(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newPhotoPath = savedInstanceState?.getString(NEW_PHOTO_PATH_KEY)
        visibleImagePath = savedInstanceState?.getString(VISIBLE_IMAGE_PATH_KEY)
        mainView = findViewById(R.id.content)

        uploadProgressBar = findViewById(R.id.upload_progress_bar)
        uploadImageFab = findViewById(R.id.upload_image_button)

        uploadImageFab.setOnClickListener {
            uploadImage()
        }

        imageButton1 = findViewById(R.id.imageButton1)
        imageButton1.setOnClickListener {
            takePicture()
        }
    }

    private fun uploadImage() {
        // check to make sure there is an image to upload
        if (photoUri != null && imageFileName != null) {

            // show progress bar while uploading image
            uploadProgressBar.visibility = View.VISIBLE

            val imageStorageRootReference = storage.reference
            val imageCollectionReference = imageStorageRootReference.child("images")
            val imageFileReference = imageCollectionReference.child(imageFileName!!)  // !! fixing error checking for null types
            // we've already checked for null, so we can force this -- non null assertion

            imageFileReference.putFile(photoUri!!)
                .addOnCompleteListener {
                    Snackbar.make(mainView, "Image uploaded", Snackbar.LENGTH_LONG).show()
                    uploadProgressBar.visibility = View.GONE // remove loading bar when image is done

                }
                .addOnFailureListener { error ->
                    Snackbar.make(mainView, "Error uploading image", Snackbar.LENGTH_LONG).show()
                    Log.e(TAG, "Error uploading image $imageFileName", error)
                    uploadProgressBar.visibility = View.GONE
                }

        } else {
            Snackbar.make(mainView, "Take a picture first!", Snackbar.LENGTH_LONG).show()
        }
    }

    // save the two instance state variables in the bundle
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(NEW_PHOTO_PATH_KEY, newPhotoPath)
        outState.putString(VISIBLE_IMAGE_PATH_KEY, visibleImagePath)
    }

    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val (photoFile, photoFilePath) = createImageFile()

        if (photoFile != null) {
            newPhotoPath = photoFilePath
            photoUri = FileProvider.getUriForFile(
                this,
                "com.example.collage.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        startActivity(takePictureIntent)
        cameraActivityLauncher.launch(takePictureIntent)
    }

    // creates new image file with unique name
    private fun createImageFile(): Pair<File?, String?> {
        try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            imageFileName = "COLLAGE_$dateTime"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(imageFileName, ".jpg", storageDir)
            val filePath = file.absolutePath
            return file to filePath

        } catch (ex: IOException) {
            return null to null
        }
    }

    // function to decide what to do with the photo, set up the path to save it
    private fun handleImage(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, user took picture, image at $newPhotoPath")
                visibleImagePath = newPhotoPath
            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result cancelled, no picture taken")
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "On window focus changed $hasFocus visible image at $visibleImagePath")
        if (hasFocus) {
            visibleImagePath?.let { imagePath ->
                loadImage(imageButton1, imagePath) }
        }
    }

    private fun loadImage(imageButton: ImageButton, imagePath: String) {
        // use image library to help work with sizing the photo, instead of writing
        // all code from scratch -- add into build.gradle(module)
        Picasso.get()
            .load(File(imagePath))
            .error(android.R.drawable.stat_notify_error) // displays if issue loading image / give user feedback
            .fit()
            .centerCrop()
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "Loaded image $imagePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image $imagePath", e)
                }
            })
    }

}