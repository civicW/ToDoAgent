data class Task(
    val listId: String,
    val userId: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val content: String,
    val isImportant: Boolean = false,
    val isCompleted: Boolean = false,
    val syncStatus: Int,
    val lastModified: String?
) 