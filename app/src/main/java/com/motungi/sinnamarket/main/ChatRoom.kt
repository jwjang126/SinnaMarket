data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val itemId: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "" // 추가
)
