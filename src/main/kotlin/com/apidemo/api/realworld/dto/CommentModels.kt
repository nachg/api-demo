package com.apidemo.api.realworld.dto

import java.time.Instant

data class CommentCreateRequest(val comment: Comment) {
    data class Comment(val body: String)
}

data class CommentModel(
    val id: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val body: String,
    val author: ProfileModel,
)

data class CommentResponse(val comment: CommentModel)

data class CommentsResponse(val comments: List<CommentModel>)
