package cz.hcasc.hotel.ui.maintenance

import androidx.compose.runtime.Composable
import cz.hcasc.hotel.repo.model.ReportCategory
import cz.hcasc.hotel.ui.ReportsScreen

@Composable
fun MaintenanceScreen() {
    ReportsScreen(
        title = "Údržba",
        subtitle = "Otevřené závady k opravě.",
        category = ReportCategory.ISSUE,
        emptyText = "Žádné otevřené závady."
    )
}
