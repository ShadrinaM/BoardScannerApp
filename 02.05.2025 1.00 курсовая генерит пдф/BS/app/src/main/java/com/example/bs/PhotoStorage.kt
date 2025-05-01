package com.example.bs

import android.net.Uri

class PhotoStorage {
    private val photos = mutableListOf<Uri>()

    fun addPhoto(uri: Uri) {
        photos.add(uri)
    }

    fun getAllPhotos(): List<Uri> = photos.toList()

    fun clear() {
        photos.clear()
    }
}
