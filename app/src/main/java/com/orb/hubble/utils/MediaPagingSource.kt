package com.orb.hubble.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import com.orb.hubble.ui.screens.MediaItem
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPagingSource(
    private val context: Context,
    private val selectedFolder: String? = null
) : PagingSource<Int, MediaItem>() {

    private val exemptedFolders = listOf("Music", "Cache")

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val offset = params.key ?: 0
                val limit = params.loadSize
                val mediaList = mutableListOf<MediaItem>()

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.DURATION,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )

                var selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"

                val selectionArgs = mutableListOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )

                val exemptedPlaceholders = exemptedFolders.joinToString { "?" }
                selection += " AND ${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} NOT IN ($exemptedPlaceholders)"
                selectionArgs.addAll(exemptedFolders)

                if (selectedFolder != null) {
                    selection += " AND ${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} = ?"
                    selectionArgs.add(selectedFolder)
                }

                val queryUri = MediaStore.Files.getContentUri("external")

                // 3. Fixed the API 30+ Query Crash by using a Bundle for limits/offsets
                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bundle = Bundle().apply {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
                        putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Files.FileColumns.DATE_ADDED))
                        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    }
                    context.contentResolver.query(queryUri, projection, bundle, null)
                } else {
                    val sortOrderString = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                    context.contentResolver.query(queryUri, projection, selection, selectionArgs.toTypedArray(), sortOrderString)
                }

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dateCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val mimeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val durCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val mimeType = it.getString(mimeCol) ?: "image/jpeg"
                        val isVideo = mimeType.startsWith("video")
                        val contentUri = if (isVideo) {
                            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        } else {
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        }

                        val thumbnail = if (isVideo) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    context.contentResolver.loadThumbnail(contentUri, Size(250, 250), null)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Video.Thumbnails.getThumbnail(
                                        context.contentResolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null
                                    )
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        mediaList.add(
                            MediaItem(
                                id = id,
                                name = it.getString(nameCol) ?: "",
                                uri = contentUri,
                                mimeType = mimeType,
                                dateAdded = it.getLong(dateCol),
                                duration = it.getLong(durCol),
                                thumbnail = thumbnail
                            )
                        )
                    }
                }

                LoadResult.Page(
                    data = mediaList,
                    prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0),
                    nextKey = if (mediaList.size < limit) null else offset + limit
                )
            } catch (e: Exception) {
                e.printStackTrace()
                LoadResult.Error(e)
            }
        }
    }
}