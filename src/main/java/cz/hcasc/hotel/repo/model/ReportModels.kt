package cz.hcasc.hotel.repo.model

data class ReportItem(
    val id: String,
    val room: Int,
    val description: String?,
    val createdAtHuman: String,
    val thumbnailUrls: List<String> = emptyList(),
    val photoUrls: List<String> = emptyList()
)

enum class ReportCategory { FIND, ISSUE }
