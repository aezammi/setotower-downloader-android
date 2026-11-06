package com.setotower.downloader

import com.google.gson.JsonParser
import com.setotower.downloader.util.SetotowerApi
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        Assert.assertEquals(4, (2 + 2).toLong())
    }

    @Test
    fun cinebInfoFallsBackToBestWhenNoVideoFormatsAreReturned() {
        val result = SetotowerApi.parseCinebInfo(
            "https://cineb.setotower.com/movie/example-1/",
            JsonParser.parseString("""{"title":"Example","uploader":"Cineb"}""").asJsonObject
        )

        Assert.assertEquals("Example", result.title)
        Assert.assertEquals("Cineb", result.author)
        Assert.assertEquals(1, result.formats.size)
        Assert.assertEquals("best", result.formats.single().format_id)
    }

    @Test
    fun cinebInfoPreservesAdvertisedQualityOptions() {
        val result = SetotowerApi.parseCinebInfo(
            "https://cineb.setotower.com/movie/example-1/",
            JsonParser.parseString(
                """{"title":"Example","video":[{"value":"1080p","label":"1080p MP4"},{"value":"720p","label":"720p MP4"}]}"""
            ).asJsonObject
        )

        Assert.assertEquals(listOf("1080p", "720p"), result.formats.map { it.format_id })
        Assert.assertEquals("1080p MP4", result.formats.first().format_note)
    }

    @Test
    fun cinebDownloadRejectsPlaylistsAndHtmlButAllowsBinaryMediaPrefixes() {
        Assert.assertTrue(SetotowerApi.isPlaylistOrHtmlPrefix("#EXTM3U\n".toByteArray()))
        Assert.assertTrue(SetotowerApi.isPlaylistOrHtmlPrefix("  <html>".toByteArray()))
        Assert.assertFalse(SetotowerApi.isPlaylistOrHtmlPrefix(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)))
    }
}
