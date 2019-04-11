package com.company.wizapp2.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.sap.cloud.mobile.foundation.common.ClientProvider

import java.io.InputStream

/**
 * Custom implementation of AppGlideModule
 *
 * Set up Glide to use ClientProvider's authenticated OkHttpClient
 */
@GlideModule
class CustomGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Set up Glide to use application OkHttpClient
        val client = ClientProvider.get()
        val factory = OkHttpUrlLoader.Factory(client)
        glide.registry.replace(GlideUrl::class.java, InputStream::class.java, factory)
    }
}