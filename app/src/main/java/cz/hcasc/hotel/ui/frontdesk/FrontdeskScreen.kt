package cz.hcasc.hotel.ui.frontdesk

import androidx.compose.runtime.Composable
import cz.hcasc.hotel.repo.model.ReportCategory
import cz.hcasc.hotel.ui.ReportsScreen

@Composable
fun FrontdeskScreen() {
    ReportsScreen(
        title = "Recepce",
        subtitle = "Otevřené nálezy od pokojských.",
        category = ReportCategory.FIND,
        emptyText = "Žádné otevřené nálezy."
    )
}
