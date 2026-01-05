package com.orb.hubble.data

import android.content.Context
import com.orb.hubble.utils.SaveToStorageStrategy
import java.io.File
import javax.inject.Inject

class CacheStorageStrategy(
    private val context: Context
): SaveToStorageStrategy {
    override fun getFilePath(fileName: String): String {
        return File(context.cacheDir, fileName).absolutePath
    }
}