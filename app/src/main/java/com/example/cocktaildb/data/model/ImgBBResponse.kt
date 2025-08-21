package com.example.cocktaildb.data.model

import org.json.JSONObject

data class ImgBBResponse(
    val success: Boolean,
    val data: ImgBBData?,
    val error: ImgBBError?
) {
    companion object {
        fun fromJson(jsonString: String): ImgBBResponse {
            val json = JSONObject(jsonString)
            return ImgBBResponse(
                success = json.getBoolean("success"),
                data = if (json.has("data") && !json.isNull("data")) {
                    ImgBBData.fromJson(json.getJSONObject("data"))
                } else null,
                error = if (json.has("error") && !json.isNull("error")) {
                    ImgBBError.fromJson(json.getJSONObject("error"))
                } else null
            )
        }
    }
}

data class ImgBBData(
    val id: String,
    val title: String,
    val urlViewer: String,
    val url: String,
    val displayUrl: String,
    val size: Int,
    val time: String,
    val expiration: String,
    val image: ImgBBImageData,
    val thumb: ImgBBImageData,
    val deleteUrl: String?
) {
    companion object {
        fun fromJson(json: JSONObject): ImgBBData {
            return ImgBBData(
                id = json.getString("id"),
                title = json.getString("title"),
                urlViewer = json.getString("url_viewer"),
                url = json.getString("url"),
                displayUrl = json.getString("display_url"),
                size = json.getInt("size"),
                time = json.getString("time"),
                expiration = json.getString("expiration"),
                image = ImgBBImageData.fromJson(json.getJSONObject("image")),
                thumb = ImgBBImageData.fromJson(json.getJSONObject("thumb")),
                deleteUrl = if (json.has("delete_url") && !json.isNull("delete_url")) {
                    json.getString("delete_url")
                } else null
            )
        }
    }
}

data class ImgBBImageData(
    val filename: String,
    val name: String,
    val mime: String,
    val extension: String,
    val url: String
) {
    companion object {
        fun fromJson(json: JSONObject): ImgBBImageData {
            return ImgBBImageData(
                filename = json.getString("filename"),
                name = json.getString("name"),
                mime = json.getString("mime"),
                extension = json.getString("extension"),
                url = json.getString("url")
            )
        }
    }
}

data class ImgBBError(
    val message: String,
    val code: Int
) {
    companion object {
        fun fromJson(json: JSONObject): ImgBBError {
            return ImgBBError(
                message = json.getString("message"),
                code = json.getInt("code")
            )
        }
    }
}
