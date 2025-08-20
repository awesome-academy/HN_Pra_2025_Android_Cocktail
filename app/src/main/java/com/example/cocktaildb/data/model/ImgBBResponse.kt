package com.example.cocktaildb.data.model

import com.google.gson.annotations.SerializedName

data class ImgBBResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: ImgBBData?,
    @SerializedName("error")
    val error: ImgBBError?
)

data class ImgBBData(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("url_viewer")
    val urlViewer: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("display_url")
    val displayUrl: String,
    @SerializedName("size")
    val size: Int,
    @SerializedName("time")
    val time: String,
    @SerializedName("expiration")
    val expiration: String,
    @SerializedName("image")
    val image: ImgBBImageData,
    @SerializedName("thumb")
    val thumb: ImgBBImageData,
    @SerializedName("delete_url")
    val deleteUrl: String?
)

data class ImgBBImageData(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("mime")
    val mime: String,
    @SerializedName("extension")
    val extension: String,
    @SerializedName("url")
    val url: String
)

data class ImgBBError(
    @SerializedName("message")
    val message: String,
    @SerializedName("code")
    val code: Int
)
