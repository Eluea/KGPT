package tn.eluea.kgpt.features.downloader.core;

import java.io.Serializable;

/**
 * Data model for media items (YouTube search results or extracted links).
 */
public class MediaSearchItem implements Serializable {
    private String id;
    private String title;
    private String url;
    private String thumbnailUrl;
    private String duration;
    private String uploader;
    private String viewCount;
    private String platform;
    private boolean isSelected;

    public MediaSearchItem() {
    }

    public MediaSearchItem(String id, String title, String url, String thumbnailUrl, String duration, String uploader, String platform) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.uploader = uploader;
        this.platform = platform;
        this.isSelected = true; // default selected in multi-link selection
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null && !title.isEmpty() ? title : "مقطع وسائط";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        if ((thumbnailUrl == null || thumbnailUrl.isEmpty()) && url != null) {
            return MediaUtils.getThumbnailUrl(url);
        }
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDuration() {
        return duration != null ? duration : "";
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getUploader() {
        return uploader != null ? uploader : "";
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getViewCount() {
        return viewCount != null ? viewCount : "";
    }

    public void setViewCount(String viewCount) {
        this.viewCount = viewCount;
    }

    public String getPlatform() {
        return platform != null ? platform : (url != null ? MediaUtils.getPlatformName(url) : "YouTube");
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
