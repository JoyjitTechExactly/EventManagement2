package com.example.eventmanagement2.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.eventmanagement2.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLoader @Inject constructor(private val context: Context) {
    
    private val defaultOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_error_placeholder)
    
    fun loadImage(url: String, imageView: ImageView, placeholder: Int = R.drawable.ic_image_placeholder) {
        Glide.with(context)
            .load(if (url.isNotEmpty()) url else null)
            .apply(defaultOptions.placeholder(placeholder))
            .into(imageView)
    }
    
    fun loadCircularImage(url: String, imageView: ImageView, placeholder: Int = R.drawable.ic_image_placeholder) {
        Glide.with(context)
            .load(if (url.isNotEmpty()) url else null)
            .apply(
                defaultOptions
                    .circleCrop()
                    .placeholder(placeholder)
            )
            .into(imageView)
    }
    
    fun loadImageWithThumbnail(originalUrl: String, thumbnailUrl: String, imageView: ImageView) {
        Glide.with(context)
            .load(if (originalUrl.isNotEmpty()) originalUrl else null)
            .thumbnail(
                Glide.with(context)
                    .load(if (thumbnailUrl.isNotEmpty()) thumbnailUrl else null)
                    .apply(RequestOptions().centerCrop())
            )
            .apply(defaultOptions)
            .into(imageView)
    }
    
    fun clear(view: ImageView) {
        Glide.with(context).clear(view)
    }
}
