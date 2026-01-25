package cz.hcasc.hotel.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cz.hcasc.hotel.R
import cz.hcasc.hotel.ui.frontdesk.FrontdeskScreen
import cz.hcasc.hotel.ui.housekeeping.HousekeepingScreen
import cz.hcasc.hotel.ui.maintenance.MaintenanceScreen

/**
 * Responsivní navigace dle specifikace:
 * - Mobil (< 600dp): bottom navigation se 3 listy
 * - Tablet (>= 600dp): navigation rail
 */
object NavScaffold {

    const val ROUTE_HOUSEKEEPING = "housekeeping"
    const val ROUTE_FRONTDESK = "frontdesk"
    const val ROUTE_MAINTENANCE = "maintenance"

    @Immutable
    data class Dest(
        val route: String,
        val label: String,
        val iconRes: Int
    )

    private val destinations = listOf(
        Dest(ROUTE_HOUSEKEEPING, "Pokojská", R.drawable.ic_tab_housekeeping),
        Dest(ROUTE_FRONTDESK, "Recepce", R.drawable.ic_tab_frontdesk),
        Dest(ROUTE_MAINTENANCE, "Údržba", R.drawable.ic_tab_maintenance),
    )

    @Composable
    fun AppNavScaffold(
        modifier: Modifier = Modifier,
        startRoute: String = ROUTE_HOUSEKEEPING,
        navController: NavHostController = rememberNavController(),
        housekeeping: @Composable () -> Unit = { HousekeepingScreen() },
        frontdesk: @Composable () -> Unit = { FrontdeskScreen() },
        maintenance: @Composable () -> Unit = { MaintenanceScreen() },
    ) {
        val config = LocalConfiguration.current
        val isTablet = config.smallestScreenWidthDp >= 600

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        if (isTablet) {
            Row(modifier = modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    destinations.forEach { d ->
                        val selected = currentRoute == d.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(d.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(d.iconRes),
                                    contentDescription = d.label
                                )
                            },
                            label = { Text(d.label) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable(ROUTE_HOUSEKEEPING) { housekeeping() }
                        composable(ROUTE_FRONTDESK) { frontdesk() }
                        composable(ROUTE_MAINTENANCE) { maintenance() }
                    }
                }
            }
        } else {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        destinations.forEach { d ->
                            val selected = currentRoute == d.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(d.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(d.iconRes),
                                        contentDescription = d.label
                                    )
                                },
                                label = { Text(d.label) },
                            )
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = startRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 8.dp),
                ) {
                    composable(ROUTE_HOUSEKEEPING) { housekeeping() }
                    composable(ROUTE_FRONTDESK) { frontdesk() }
                    composable(ROUTE_MAINTENANCE) { maintenance() }
                }
            }
        }
    }
}
