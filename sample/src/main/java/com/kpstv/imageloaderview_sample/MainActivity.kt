package com.kpstv.imageloaderview_sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.kpstv.imageloaderview.AnimationType
import com.kpstv.imageloaderview.ImageLoaderView

class MainActivity : AppCompatActivity() {

    private val imageLoaderView1: ImageLoaderView by lazy { findViewById(R.id.imageloaderview1) }
    private val imageLoaderView2: ImageLoaderView by lazy { findViewById(R.id.imageloaderview2) }
    private val imageLoaderView3: ImageLoaderView by lazy { findViewById(R.id.imageloaderview3) }
    private val imageLoaderView4: ImageLoaderView by lazy { findViewById(R.id.imageloaderview4) }
    private val imageLoaderView5: ImageLoaderView by lazy { findViewById(R.id.imageloaderview5) }
    private val imageLoaderView6: ImageLoaderView by lazy { findViewById(R.id.imageloaderview6) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fake load of the image
        loadImage(imageLoaderView1, R.drawable.image1)
        loadImage(imageLoaderView2, R.drawable.image2)
        loadImage(imageLoaderView3, R.drawable.image3)
        loadImageWithoutAnimation(imageLoaderView4, R.drawable.image4)
        loadImageWithoutAnimation(imageLoaderView5, R.drawable.image5)
        loadImageWithoutAnimation(imageLoaderView6, R.drawable.image6)
    }

    private fun loadImage(imageView: ImageLoaderView, resId: Int) {
        imageView.postDelayed({
            val cover = ContextCompat.getDrawable(this, resId)
            imageView.setImageDrawable(cover, animationType = ImageLoaderView.CIRCLE_IN)
        }, 5000)
    }

    private fun loadImageWithoutAnimation(imageView: ImageLoaderView, resId: Int) {
        imageView.postDelayed({
            val cover = ContextCompat.getDrawable(this, resId)
            imageView.setImageDrawable(cover)
            imageView.stopAllSideEffects()
            imageView.setOnClickListener { Toast.makeText(it.context, "Clicked", Toast.LENGTH_SHORT).show() }
        }, 5000)
    }
}