package net.matsudamper.money.frontend.common.base.image

import kotlinx.datetime.LocalDateTime

/**
 * JPEG の Exif から撮影日時(DateTimeOriginal)を読む。
 * Exif の撮影日時はタイムゾーンを持たない現地時刻なので、変換せずそのまま返す。
 */
public object ExifCaptureDateTimeReader {
    private const val JPEG_MARKER_PREFIX = 0xFF
    private const val JPEG_START_OF_IMAGE = 0xD8
    private const val JPEG_APP1 = 0xE1
    private const val JPEG_START_OF_SCAN = 0xDA
    private const val SEGMENT_LENGTH_SIZE = 2
    private const val TIFF_HEADER_SIZE = 8
    private const val IFD_ENTRY_SIZE = 12
    private const val TAG_EXIF_IFD_POINTER = 0x8769
    private const val TAG_DATE_TIME_ORIGINAL = 0x9003
    private const val TYPE_ASCII = 2
    private const val DATE_TIME_LENGTH = 19

    private val exifIdentifier = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
    private val dateTimeRegex = Regex("""(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})""")

    public fun read(bytes: ByteArray): LocalDateTime? {
        val tiff = findTiff(bytes) ?: return null
        val exifIfdOffset = tiff.findEntry(ifdOffset = tiff.firstIfdOffset() ?: return null, tag = TAG_EXIF_IFD_POINTER)
            ?.valueOrOffset
            ?: return null
        val dateTimeEntry = tiff.findEntry(ifdOffset = exifIfdOffset, tag = TAG_DATE_TIME_ORIGINAL) ?: return null
        if (dateTimeEntry.type != TYPE_ASCII || dateTimeEntry.count < DATE_TIME_LENGTH) return null
        val text = tiff.readAscii(offset = dateTimeEntry.valueOrOffset, length = DATE_TIME_LENGTH) ?: return null
        return parseDateTime(text)
    }

    private fun findTiff(bytes: ByteArray): Tiff? {
        if (unsignedAt(bytes, 0) != JPEG_MARKER_PREFIX || unsignedAt(bytes, 1) != JPEG_START_OF_IMAGE) return null

        var segmentStart = 2
        while (true) {
            if (unsignedAt(bytes, segmentStart) != JPEG_MARKER_PREFIX) return null
            val marker = unsignedAt(bytes, segmentStart + 1) ?: return null
            if (marker == JPEG_START_OF_SCAN) return null
            val lengthHigh = unsignedAt(bytes, segmentStart + 2) ?: return null
            val lengthLow = unsignedAt(bytes, segmentStart + 3) ?: return null
            val segmentLength = (lengthHigh shl 8) or lengthLow
            if (segmentLength < SEGMENT_LENGTH_SIZE) return null
            val payloadStart = segmentStart + 2 + SEGMENT_LENGTH_SIZE
            val segmentEnd = segmentStart + 2 + segmentLength

            if (marker == JPEG_APP1 && startsWith(bytes = bytes, prefix = exifIdentifier, offset = payloadStart)) {
                val tiffStart = payloadStart + exifIdentifier.size
                return createTiff(bytes = bytes, start = tiffStart, end = minOf(segmentEnd, bytes.size))
            }
            segmentStart = segmentEnd
        }
    }

    private fun createTiff(bytes: ByteArray, start: Int, end: Int): Tiff? {
        if (end - start < TIFF_HEADER_SIZE) return null
        val isLittleEndian = when (bytes.decodeToString(start, start + 2)) {
            "II" -> true
            "MM" -> false
            else -> return null
        }
        return Tiff(bytes = bytes, start = start, end = end, isLittleEndian = isLittleEndian)
    }

    private fun parseDateTime(text: String): LocalDateTime? {
        val values = dateTimeRegex.matchEntire(text)?.groupValues?.drop(1)?.map { it.toInt() } ?: return null
        return runCatching {
            LocalDateTime(values[0], values[1], values[2], values[3], values[4], values[5])
        }.getOrNull()
    }

    private fun unsignedAt(bytes: ByteArray, index: Int): Int? {
        return bytes.getOrNull(index)?.let { it.toInt() and 0xFF }
    }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray, offset: Int): Boolean {
        if (offset + prefix.size > bytes.size) return false
        return prefix.indices.all { bytes[offset + it] == prefix[it] }
    }

    private class IfdEntry(val type: Int, val count: Int, val valueOrOffset: Int)

    /** オフセットはすべて TIFF ヘッダ先頭からの相対位置 */
    private class Tiff(
        private val bytes: ByteArray,
        private val start: Int,
        private val end: Int,
        private val isLittleEndian: Boolean,
    ) {
        fun firstIfdOffset(): Int? = readInt(4)

        fun findEntry(ifdOffset: Int, tag: Int): IfdEntry? {
            val entryCount = readShort(ifdOffset) ?: return null
            return (0 until entryCount).firstNotNullOfOrNull { index ->
                val entryOffset = ifdOffset + 2 + index * IFD_ENTRY_SIZE
                if (readShort(entryOffset) == tag) {
                    IfdEntry(
                        type = readShort(entryOffset + 2) ?: return null,
                        count = readInt(entryOffset + 4) ?: return null,
                        valueOrOffset = readInt(entryOffset + 8) ?: return null,
                    )
                } else {
                    null
                }
            }
        }

        fun readAscii(offset: Int, length: Int): String? {
            if (!contains(offset, length)) return null
            return bytes.decodeToString(start + offset, start + offset + length)
        }

        private fun readShort(offset: Int): Int? = readUnsigned(offset, 2)

        private fun readInt(offset: Int): Int? = readUnsigned(offset, 4)

        private fun readUnsigned(offset: Int, length: Int): Int? {
            if (!contains(offset, length)) return null
            val order = if (isLittleEndian) (length - 1 downTo 0) else (0 until length)
            val value = order.fold(0L) { acc, index ->
                (acc shl 8) or (bytes[start + offset + index].toLong() and 0xFF)
            }
            return value.takeIf { it <= Int.MAX_VALUE }?.toInt()
        }

        private fun contains(offset: Int, length: Int): Boolean {
            return offset >= 0 && offset <= end - start - length
        }
    }
}
