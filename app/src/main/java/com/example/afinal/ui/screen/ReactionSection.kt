package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.afinal.data.model.Reaction
import com.example.afinal.data.model.AuthViewModel
import com.example.afinal.data.model.StoryViewModel

val reactionTypes = mapOf(
    "like" to "👍",
    "love" to "❤️",
    "haha" to "😂",
    "wow" to "😮",
    "sad" to "😢",
    "angry" to "😠"
)

@Composable
fun ReactionSection(
    storyViewModel: StoryViewModel,
    authViewModel: AuthViewModel,
    storyId: String
) {
    val reactions by storyViewModel.reactions
    val userEmail = authViewModel.userEmail

    LaunchedEffect(storyId) {
        storyViewModel.getReactions(storyId)
    }

    val userReaction = reactions.find { it.userId == userEmail }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Reactions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            reactionTypes.forEach { (type, emoji) ->
                val count = reactions.count { it.type == type }
                val isReactedByCurrentUser = userReaction?.type == type

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = {
                        if (userEmail != null) {
                            if (isReactedByCurrentUser) {
                                storyViewModel.removeReaction(storyId, userEmail)
                            } else {
                                val isNewReaction = userReaction == null
                                val reaction = Reaction(userId = userEmail, type = type)
                                storyViewModel.addReaction(storyId, reaction, isNewReaction)
                            }
                        }
                    }) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                    Text(text = count.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
