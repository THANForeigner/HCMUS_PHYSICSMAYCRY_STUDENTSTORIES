package com.example.afinal.data.repository

import com.example.afinal.data.model.Story

object StoryRepository {
    fun getAllStories(): List<Story> {
        return MockData.getAllStories()
        // TODO: Implement Firebase query
    }

    fun getStoryById(id: String): Story? {
        return MockData.getStoryById(id)
        // TODO: Implement Firebase query by ID
    }

    fun getFeaturedStory(): Story? {
        return MockData.getFeaturedStory()
        // TODO: Implement Firebase query for featured story
    }

    fun getNearbyStories(): List<Story> {
        return MockData.getNearbyStories()
        // TODO: Implement Firebase query for nearby stories
    }
}