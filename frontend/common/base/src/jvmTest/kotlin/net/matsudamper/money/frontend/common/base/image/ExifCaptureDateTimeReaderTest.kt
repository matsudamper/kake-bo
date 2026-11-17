package net.matsudamper.money.frontend.common.base.image

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.datetime.LocalDateTime
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

public class ExifCaptureDateTimeReaderTest : DescribeSpec(
    {
        fun tiff(order: ByteOrder, dateTime: String): ByteArray {
            val exifIfdOffset = 8 + 2 + 12 + 4
            val dateTimeOffset = exifIfdOffset + 2 + 12 + 4
            return ByteBuffer.allocate(dateTimeOffset + 20).order(order).apply {
                put((if (order == ByteOrder.LITTLE_ENDIAN) "II" else "MM").toByteArray())
                putShort(42)
                putInt(8)
                // IFD0
                putShort(1)
                putShort(0x8769.toShort()).putShort(4).putInt(1).putInt(exifIfdOffset)
                putInt(0)
                // Exif IFD
                putShort(1)
                putShort(0x9003.toShort()).putShort(2).putInt(20).putInt(dateTimeOffset)
                putInt(0)
                put(dateTime.toByteArray()).put(0)
            }.array()
        }

        fun segment(marker: Int, payload: ByteArray): ByteArray {
            val length = payload.size + 2
            return byteArrayOf(0xFF.toByte(), marker.toByte(), (length shr 8).toByte(), length.toByte()) + payload
        }

        fun jpeg(vararg segments: ByteArray): ByteArray {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
                segments.forEach { write(it) }
                write(byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, 0x02))
            }.toByteArray()
        }

        fun exifSegment(order: ByteOrder, dateTime: String): ByteArray {
            return segment(0xE1, "Exif".toByteArray() + byteArrayOf(0, 0) + tiff(order, dateTime))
        }

        describe("read") {
            it("リトルエンディアンの撮影日時を読める") {
                val bytes = jpeg(exifSegment(ByteOrder.LITTLE_ENDIAN, "2024:03:05 12:34:56"))
                ExifCaptureDateTimeReader.read(bytes) shouldBe LocalDateTime(2024, 3, 5, 12, 34, 56)
            }

            it("ビッグエンディアンの撮影日時を読める") {
                val bytes = jpeg(exifSegment(ByteOrder.BIG_ENDIAN, "2024:03:05 12:34:56"))
                ExifCaptureDateTimeReader.read(bytes) shouldBe LocalDateTime(2024, 3, 5, 12, 34, 56)
            }

            it("Exif より前に別のセグメントがあっても読める") {
                val bytes = jpeg(
                    segment(0xE0, ByteArray(14)),
                    exifSegment(ByteOrder.BIG_ENDIAN, "2024:03:05 12:34:56"),
                )
                ExifCaptureDateTimeReader.read(bytes) shouldBe LocalDateTime(2024, 3, 5, 12, 34, 56)
            }

            it("Exif が無ければ null") {
                ExifCaptureDateTimeReader.read(jpeg(segment(0xE0, ByteArray(14)))) shouldBe null
            }

            it("撮影日時が未設定の値なら null") {
                val bytes = jpeg(exifSegment(ByteOrder.LITTLE_ENDIAN, "0000:00:00 00:00:00"))
                ExifCaptureDateTimeReader.read(bytes) shouldBe null
            }

            it("途中で切れていても例外にならず null") {
                val bytes = jpeg(exifSegment(ByteOrder.LITTLE_ENDIAN, "2024:03:05 12:34:56"))
                ExifCaptureDateTimeReader.read(bytes.copyOf(40)) shouldBe null
            }

            it("JPEG 以外は null") {
                ExifCaptureDateTimeReader.read(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) shouldBe null
            }
        }
    },
)
