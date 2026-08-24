package tn.eluea.kgpt.features.downloader.core;

import java.io.File;
import java.io.Serializable;

public class DownloadOptions implements Serializable {
    public enum Type {
        VIDEO,
        AUDIO
    }

    private String url;
    private Type type = Type.VIDEO;
    private String videoQuality = "1080"; // "2160", "1440", "1080", "720", "480", "360", "best"
    private String videoFormat = "mp4"; // "mp4", "mkv", "webm"
    private String audioFormat = "mp3"; // "mp3", "m4a", "flac", "opus", "wav"
    private String audioQuality = "0"; // "0" (best 320kbps), "5" (192kbps), "9" (128kbps)
    private boolean embedThumbnail = true;
    private boolean embedMetadata = true;
    private boolean embedSubtitles = false;
    private boolean splitChapters = false;
    private String uploader = null;
    private File customDownloadDir = null;
    private String customFileName = null;

    public DownloadOptions(String url) {
        this.url = url;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public boolean isEmbedSubtitles() {
        return embedSubtitles;
    }

    public void setEmbedSubtitles(boolean embedSubtitles) {
        this.embedSubtitles = embedSubtitles;
    }

    public boolean isSplitChapters() {
        return splitChapters;
    }

    public void setSplitChapters(boolean splitChapters) {
        this.splitChapters = splitChapters;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isAudio() {
        return type == Type.AUDIO;
    }

    public String getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(String videoQuality) {
        this.videoQuality = videoQuality;
    }

    public String getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }

    public String getAudioQuality() {
        return audioQuality;
    }

    public void setAudioQuality(String audioQuality) {
        this.audioQuality = audioQuality;
    }

    public boolean isEmbedThumbnail() {
        return embedThumbnail;
    }

    public void setEmbedThumbnail(boolean embedThumbnail) {
        this.embedThumbnail = embedThumbnail;
    }

    public boolean isEmbedMetadata() {
        return embedMetadata;
    }

    public void setEmbedMetadata(boolean embedMetadata) {
        this.embedMetadata = embedMetadata;
    }

    public File getCustomDownloadDir() {
        return customDownloadDir;
    }

    public void setCustomDownloadDir(File customDownloadDir) {
        this.customDownloadDir = customDownloadDir;
    }

    public String getCustomFileName() {
        return customFileName;
    }

    public void setCustomFileName(String customFileName) {
        this.customFileName = customFileName;
    }

    private String downloadSections = null; // e.g. "*00:00:10-00:01:30"
    private int concurrentFragments = 16;   // 16 or 32 concurrent threads
    private String estimatedFileSize = null;
    private String processId = null;
    private String preferredFormatId = null; // exact yt-dlp format id picked from real available formats
    private String extractorArgs = null;     // extractor-args used when info was fetched (keeps format ids valid)
    private boolean keepOriginalAudio = false; // true = download the source codec as-is, no transcode
    private boolean burnSubtitles = false;
    private boolean embedChapters = false;
    private boolean recodeVideo = false;
    private boolean compatibleH264 = false;
    private boolean saveSeparateThumbnail = false;
    private String extraCommands = null;
    private String customTreeUri = null; // SAF tree URI for custom download dir

    public boolean isKeepOriginalAudio() {
        return keepOriginalAudio;
    }

    public void setKeepOriginalAudio(boolean keepOriginalAudio) {
        this.keepOriginalAudio = keepOriginalAudio;
    }

    public String getPreferredFormatId() {
        return preferredFormatId;
    }

    public void setPreferredFormatId(String preferredFormatId) {
        this.preferredFormatId = preferredFormatId;
    }

    public String getExtractorArgs() {
        return extractorArgs;
    }

    public boolean isBurnSubtitles() { return burnSubtitles; }
    public void setBurnSubtitles(boolean v) { this.burnSubtitles = v; }
    public boolean isEmbedChapters() { return embedChapters; }
    public void setEmbedChapters(boolean v) { this.embedChapters = v; }
    public boolean isRecodeVideo() { return recodeVideo; }
    public void setRecodeVideo(boolean v) { this.recodeVideo = v; }
    public boolean isCompatibleH264() { return compatibleH264; }
    public void setCompatibleH264(boolean v) { this.compatibleH264 = v; }
    public boolean isSaveSeparateThumbnail() { return saveSeparateThumbnail; }
    public void setSaveSeparateThumbnail(boolean v) { this.saveSeparateThumbnail = v; }
    public String getExtraCommands() { return extraCommands; }
    public void setExtraCommands(String v) { this.extraCommands = v; }
    public String getCustomTreeUri() { return customTreeUri; }
    public void setCustomTreeUri(String v) { this.customTreeUri = v; }

    public void setExtractorArgs(String extractorArgs) {
        this.extractorArgs = extractorArgs;
    }

    public String getDownloadSections() {
        return downloadSections;
    }

    public void setDownloadSections(String downloadSections) {
        this.downloadSections = downloadSections;
    }

    public int getConcurrentFragments() {
        return concurrentFragments;
    }

    public void setConcurrentFragments(int concurrentFragments) {
        this.concurrentFragments = concurrentFragments;
    }

    public String getEstimatedFileSize() {
        return estimatedFileSize;
    }

    public void setEstimatedFileSize(String estimatedFileSize) {
        this.estimatedFileSize = estimatedFileSize;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }
}
