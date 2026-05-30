package net.matsudamper.money.frontend.common.viewmodel.root.settings.timezone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimezoneSettingViewModelTest {
    @Test
    fun `有効なオフセット範囲の上限は840分`() {
        assertTrue(840 in TimezoneSettingViewModel.validTimezoneOffsetRange)
        assertFalse(841 in TimezoneSettingViewModel.validTimezoneOffsetRange)
    }

    @Test
    fun `有効なオフセット範囲の下限はマイナス720分`() {
        assertTrue(-720 in TimezoneSettingViewModel.validTimezoneOffsetRange)
        assertFalse(-721 in TimezoneSettingViewModel.validTimezoneOffsetRange)
    }

    @Test
    fun `有効なオフセット範囲内の代表値が含まれる`() {
        assertTrue(0 in TimezoneSettingViewModel.validTimezoneOffsetRange)
        assertTrue(540 in TimezoneSettingViewModel.validTimezoneOffsetRange)
        assertTrue(-300 in TimezoneSettingViewModel.validTimezoneOffsetRange)
    }

    @Test
    fun `UTC+09_00のオフセットテキストを正しくフォーマットする`() {
        assertEquals(
            "UTC+09:00（540分）",
            TimezoneSettingViewModel.formatOffsetText(540),
        )
    }

    @Test
    fun `UTCゼロのオフセットテキストを正しくフォーマットする`() {
        assertEquals(
            "UTC+00:00（0分）",
            TimezoneSettingViewModel.formatOffsetText(0),
        )
    }

    @Test
    fun `負のオフセットテキストを正しくフォーマットする`() {
        assertEquals(
            "UTC-05:00（-300分）",
            TimezoneSettingViewModel.formatOffsetText(-300),
        )
    }

    @Test
    fun `UTC+05_30のような30分単位のオフセットテキストを正しくフォーマットする`() {
        assertEquals(
            "UTC+05:30（330分）",
            TimezoneSettingViewModel.formatOffsetText(330),
        )
    }

    @Test
    fun `UTC+14_00のオフセットテキストを正しくフォーマットする`() {
        assertEquals(
            "UTC+14:00（840分）",
            TimezoneSettingViewModel.formatOffsetText(840),
        )
    }

    @Test
    fun `UTC-12_00のオフセットテキストを正しくフォーマットする`() {
        assertEquals(
            "UTC-12:00（-720分）",
            TimezoneSettingViewModel.formatOffsetText(-720),
        )
    }
}
