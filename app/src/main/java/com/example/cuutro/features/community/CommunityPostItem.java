package com.example.cuutro.features.community;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class CommunityPostItem {

    private final String id;
    @DrawableRes
    private final int authorAvatarResId;
    private final String authorName;
    private final boolean verified;
    private final String location;
    @DrawableRes
    private final int imageResId;
    private final String mediaCounter;
    private final String likedByText;
    private final String captionText;
    private final String postDate;

    public CommunityPostItem(
            @NonNull String id,
            @DrawableRes int authorAvatarResId,
            @NonNull String authorName,
            boolean verified,
            @NonNull String location,
            @DrawableRes int imageResId,
            @NonNull String mediaCounter,
            @NonNull String likedByText,
            @NonNull String captionText,
            @NonNull String postDate
    ) {
        this.id = id;
        this.authorAvatarResId = authorAvatarResId;
        this.authorName = authorName;
        this.verified = verified;
        this.location = location;
        this.imageResId = imageResId;
        this.mediaCounter = mediaCounter;
        this.likedByText = likedByText;
        this.captionText = captionText;
        this.postDate = postDate;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public int getAuthorAvatarResId() {
        return authorAvatarResId;
    }

    @NonNull
    public String getAuthorName() {
        return authorName;
    }

    public boolean isVerified() {
        return verified;
    }

    @NonNull
    public String getLocation() {
        return location;
    }

    public int getImageResId() {
        return imageResId;
    }

    @NonNull
    public String getMediaCounter() {
        return mediaCounter;
    }

    @NonNull
    public String getLikedByText() {
        return likedByText;
    }

    @NonNull
    public String getCaptionText() {
        return captionText;
    }

    @NonNull
    public String getPostDate() {
        return postDate;
    }
}
