package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import java.io.File

internal fun rawImageBytesFile(context: Context, id: String): File =
    File(context.filesDir, "image_uploads/$id.raw")

internal fun previewBytesFile(context: Context, id: String): File =
    File(context.filesDir, "image_uploads/$id.preview")
