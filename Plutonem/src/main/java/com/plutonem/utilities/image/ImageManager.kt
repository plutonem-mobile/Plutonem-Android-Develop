package com.plutonem.utilities.image

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.CENTER
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.plutonem.modules.GlideApp
import com.plutonem.modules.GlideRequest
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton for asynchronous image fetching/loading with support for placeholders, transformations and more.
 */
@Singleton
class ImageManager @Inject constructor(private val placeholderManager: ImagePlaceholderManager) {
    interface RequestListener<T> {
        fun onLoadFailed(e: Exception?)
        fun onResourceReady(resource: T)
    }

    /**
     * Return true if this [Context] is available.
     * Availability is defined as the following:
     * + [Context] is not null
     * + [Context] is not destroyed (tested with [FragmentActivity.isDestroyed] or [Activity.isDestroyed])
     */
    private fun Context?.isAvailable(): Boolean {
        if (this == null) {
            return false
        } else if (this !is Application) {
            if (this is FragmentActivity) {
                return !this.isDestroyed
            } else if (this is Activity) {
                return !this.isDestroyed
            }
        }
        return true
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * If no URL is provided, it only loads the placeholder
     */
    @JvmOverloads
    fun load(imageView: ImageView, imageType: ImageType, imgUrl: String = "", scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads the DrawableResource into the ImageView.
     */
    @JvmOverloads
    fun load(imageView: ImageView, @DrawableRes resourceId: Int, scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(resourceId)
                .applyScaleType(scaleType)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView and applies circle transformation. Adds placeholder and
     * error placeholder depending on the ImageType.
     */
    @JvmOverloads
    fun loadIntoCircle(
            imageView: ImageView,
            imageType: ImageType,
            imgUrl: String,
            requestListener: RequestListener<Drawable>? = null,
            version: Int? = null
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        GlideApp.with(context)
                .load(imgUrl)
                .addFallback(imageType)
                .addPlaceholder(imageType)
                .circleCrop()
                .attachRequestListener(requestListener)
                .addSignature(version)
                .into(imageView)
                .clearOnDetach()
    }

    /**
     * Cancel any pending requests and free any resources that may have been
     * loaded for the view.
     */
    fun cancelRequestAndClearImageView(imageView: ImageView) {
        GlideApp.with(imageView.context).clear(imageView)
    }


    private fun <T : Any> GlideRequest<T>.applyScaleType(
            scaleType: ImageView.ScaleType
    ): GlideRequest<T> {
        return when (scaleType) {
            ImageView.ScaleType.CENTER_CROP -> this.centerCrop()
            ImageView.ScaleType.CENTER_INSIDE -> this.centerInside()
            ImageView.ScaleType.FIT_CENTER -> this.fitCenter()
            ImageView.ScaleType.CENTER -> this
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.MATRIX -> {
                AppLog.e(AppLog.T.UTILS, String.format("ScaleType %s is not supported.", scaleType.toString()))
                this
            }
        }
    }

    private fun <T : Any> GlideRequest<T>.addPlaceholder(imageType: ImageType): GlideRequest<T> {
        val placeholderImageRes = placeholderManager.getPlaceholderResource(imageType)
        return if (placeholderImageRes == null) {
            this
        } else {
            this.placeholder(placeholderImageRes)
        }
    }

    private fun <T : Any> GlideRequest<T>.addFallback(imageType: ImageType): GlideRequest<T> {
        val errorImageRes = placeholderManager.getErrorResource(imageType)
        return if (errorImageRes == null) {
            this
        } else {
            this.error(errorImageRes)
        }
    }

    /**
     * Changing the signature invalidates cache.
     */
    private fun <T : Any> GlideRequest<T>.addSignature(signature: Int?): GlideRequest<T> {
        return if (signature == null) {
            this
        } else {
            this.signature(ObjectKey(signature))
        }
    }

    private fun <T : Any> GlideRequest<T>.attachRequestListener(
            requestListener: RequestListener<T>?
    ): GlideRequest<T> {
        return if (requestListener == null) {
            this
        } else {
            this.listener(object : com.bumptech.glide.request.RequestListener<T> {
                override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<T>?,
                        isFirstResource: Boolean
                ): Boolean {
                    requestListener.onLoadFailed(e)
                    return false
                }

                override fun onResourceReady(
                        resource: T?,
                        model: Any?,
                        target: Target<T>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                ): Boolean {
                    if (resource != null) {
                        requestListener.onResourceReady(resource)
                    } else {
                        // according to the Glide's JavaDoc, this shouldn't happen
                        AppLog.e(AppLog.T.UTILS, "Resource in ImageManager.onResourceReady is null.")
                        requestListener.onLoadFailed(null)
                    }
                    return false
                }
            })
        }
    }

    @Deprecated("Object for backward compatibility with code which doesn't support DI")
    companion object {
        @JvmStatic
        @Deprecated("Use injected ImageManager")
        val instance: ImageManager by lazy { ImageManager(ImagePlaceholderManager()) }
    }
}