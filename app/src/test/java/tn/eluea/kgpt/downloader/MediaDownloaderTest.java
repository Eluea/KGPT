package tn.eluea.kgpt.downloader;

import org.junit.Assert;
import org.junit.Test;

import tn.eluea.kgpt.features.downloader.core.DownloadOptions;
import tn.eluea.kgpt.features.downloader.core.MediaUtils;

public class MediaDownloaderTest {

    @Test
    public void testUrlExtractionFromVariousSources() {
        // Direct YouTube URL
        String directUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        Assert.assertEquals(directUrl, MediaUtils.extractUrl(directUrl));

        // Shared text with YouTube URL
        String sharedYoutube = "Check this out! https://youtu.be/dQw4w9WgXcQ it is amazing";
        Assert.assertEquals("https://youtu.be/dQw4w9WgXcQ", MediaUtils.extractUrl(sharedYoutube));

        // Shared Instagram Reel
        String sharedInstagram = "Look at this video on Instagram https://www.instagram.com/reel/C7X8yZ9/ #reels";
        Assert.assertEquals("https://www.instagram.com/reel/C7X8yZ9/", MediaUtils.extractUrl(sharedInstagram));

        // Shared TikTok
        String sharedTiktok = "Check out this TikTok video: https://vt.tiktok.com/ZS123456/";
        Assert.assertEquals("https://vt.tiktok.com/ZS123456/", MediaUtils.extractUrl(sharedTiktok));

        // Shared Twitter / X
        String sharedTwitter = "Breaking news: https://x.com/openai/status/1234567890";
        Assert.assertEquals("https://x.com/openai/status/1234567890", MediaUtils.extractUrl(sharedTwitter));
    }

    @Test
    public void testDurationFormatting() {
        Assert.assertEquals("00:00", MediaUtils.formatDuration(0));
        Assert.assertEquals("00:45", MediaUtils.formatDuration(45));
        Assert.assertEquals("03:25", MediaUtils.formatDuration(205));
        Assert.assertEquals("1:01:05", MediaUtils.formatDuration(3665));
    }

    @Test
    public void testFileSizeFormatting() {
        Assert.assertTrue(MediaUtils.formatFileSize(1048576).contains("MB") || MediaUtils.formatFileSize(1048576).contains("1.0"));
        Assert.assertTrue(MediaUtils.formatFileSize(1073741824).contains("GB") || MediaUtils.formatFileSize(1073741824).contains("1.0"));
    }

    @Test
    public void testDownloadOptionsDefaults() {
        DownloadOptions options = new DownloadOptions("https://youtu.be/test");
        Assert.assertEquals(DownloadOptions.Type.VIDEO, options.getType());
        Assert.assertFalse(options.isAudio());
        Assert.assertEquals("1080", options.getVideoQuality());
        Assert.assertEquals("mp4", options.getVideoFormat());

        // Switch to audio
        options.setType(DownloadOptions.Type.AUDIO);
        Assert.assertTrue(options.isAudio());
        Assert.assertEquals("mp3", options.getAudioFormat());
        Assert.assertTrue(options.isEmbedThumbnail());

        // Custom options
        options.setUploader("ArtistChannel");
        Assert.assertEquals("ArtistChannel", options.getUploader());
        options.setEmbedSubtitles(true);
        Assert.assertTrue(options.isEmbedSubtitles());
        options.setSplitChapters(true);
        Assert.assertTrue(options.isSplitChapters());

        // Cut / Trim & Multi-threaded fragments
        options.setDownloadSections("*00:00:10-00:01:30");
        Assert.assertEquals("*00:00:10-00:01:30", options.getDownloadSections());
        options.setConcurrentFragments(32);
        Assert.assertEquals(32, options.getConcurrentFragments());
    }
}
