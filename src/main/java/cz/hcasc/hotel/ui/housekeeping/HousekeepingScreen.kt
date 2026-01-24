package cz.hcasc.hotel.ui.housekeeping

import androidx.compose.runtime.Composable
import cz.hcasc.hotel.repo.model.ReportCategory
import cz.hcasc.hotel.ui.ReportsScreen

@Composable
fun HousekeepingScreen() {
    ReportsScreen(
        title = "Pokojská",
        subtitle = "Otevřené nálezy čekající na zpracování.",
        category = ReportCategory.FIND,
        emptyText = "Žádné otevřené nálezy."
    )
}
