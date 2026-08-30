package network.protocol.reaction;

public record ReactionReceivedDto(String matchId, String senderUsername, ReactionId reactionId) {
}
