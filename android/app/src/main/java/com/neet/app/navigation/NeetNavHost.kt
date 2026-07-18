package com.neet.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neet.app.data.HistoryRepository
import com.neet.app.data.MockTestRepository
import com.neet.app.data.QuestionRepository
import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.Subject
import com.neet.app.ui.mocktest.MockTestHomeScreen
import com.neet.app.ui.mocktest.MockTestResultScreen
import com.neet.app.ui.mocktest.MockTestScreen
import com.neet.app.ui.picker.TopicPickerScreen
import com.neet.app.ui.question.QuestionReviewScreen
import com.neet.app.ui.question.QuestionScreen
import com.neet.app.ui.stats.StatsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val ROUTE_PICKER = "picker"
private const val ROUTE_STATS = "stats"
private const val ROUTE_QUESTION = "question/{subject}/{topic}/{difficulty}"
private const val ROUTE_REVIEW = "review/{answerId}"
private const val ROUTE_MOCK_TEST_HOME = "mock_test_home"
private const val ROUTE_MOCK_TEST_TAKING = "mock_test/{testId}"
private const val ROUTE_MOCK_TEST_REVIEW = "mock_test_review/{testId}"
private const val ROUTE_MOCK_TEST_RESULT = "mock_test_result/{testId}"

private data class BottomNavTab(val route: String, val label: String)

private val bottomNavTabs = listOf(
    BottomNavTab(ROUTE_PICKER, "Practice"),
    BottomNavTab(ROUTE_MOCK_TEST_HOME, "Mock Test"),
    BottomNavTab(ROUTE_STATS, "Progress"),
)

@Composable
fun NeetNavHost(
    repository: QuestionRepository,
    historyRepository: HistoryRepository,
    mockTestRepository: MockTestRepository,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // Hidden only on the active taking screen — a focus-mode affordance, not a data-safety
        // measure (Room persistence means switching tabs mid-test is always safe).
        bottomBar = {
            if (currentRoute != ROUTE_MOCK_TEST_TAKING) {
                NeetBottomBar(navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_PICKER,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_PICKER) {
                TopicPickerScreen(
                    onStartQuestion = { subject, topic, difficulty ->
                        navController.navigateToQuestion(subject, topic, difficulty)
                    },
                )
            }
            composable(ROUTE_STATS) {
                StatsScreen(
                    historyRepository = historyRepository,
                    onOpenQuestion = { answerId -> navController.navigate("review/$answerId") },
                    onPracticeTopic = { subject, topic, difficulty ->
                        navController.navigateToQuestion(subject, topic, difficulty)
                    },
                )
            }
            composable(
                route = ROUTE_QUESTION,
                arguments = listOf(
                    navArgument("subject") { type = NavType.StringType },
                    navArgument("topic") { type = NavType.StringType },
                    navArgument("difficulty") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val subject = Subject.valueOf(backStackEntry.arguments?.getString("subject").orEmpty())
                val topic = URLDecoder.decode(
                    backStackEntry.arguments?.getString("topic").orEmpty(),
                    StandardCharsets.UTF_8.name(),
                )
                val difficulty = Difficulty.valueOf(backStackEntry.arguments?.getString("difficulty").orEmpty())

                QuestionScreen(
                    repository = repository,
                    historyRepository = historyRepository,
                    subject = subject,
                    topic = topic,
                    difficulty = difficulty,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = ROUTE_REVIEW,
                arguments = listOf(navArgument("answerId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val answerId = backStackEntry.arguments?.getString("answerId").orEmpty()
                QuestionReviewScreen(
                    historyRepository = historyRepository,
                    answerId = answerId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_MOCK_TEST_HOME) {
                MockTestHomeScreen(
                    repository = mockTestRepository,
                    onOpenTest = { testId -> navController.navigate("mock_test/$testId") },
                    onOpenResult = { testId -> navController.navigate("mock_test_result/$testId") },
                )
            }
            composable(
                route = ROUTE_MOCK_TEST_TAKING,
                arguments = listOf(navArgument("testId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val testId = backStackEntry.arguments?.getString("testId").orEmpty()
                MockTestScreen(
                    repository = mockTestRepository,
                    testId = testId,
                    readOnly = false,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { submittedTestId ->
                        navController.navigate("mock_test_result/$submittedTestId") {
                            popUpTo(ROUTE_MOCK_TEST_TAKING) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = ROUTE_MOCK_TEST_REVIEW,
                arguments = listOf(navArgument("testId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val testId = backStackEntry.arguments?.getString("testId").orEmpty()
                MockTestScreen(
                    repository = mockTestRepository,
                    testId = testId,
                    readOnly = true,
                    onBack = { navController.popBackStack() },
                    onSubmitted = {},
                )
            }
            composable(
                route = ROUTE_MOCK_TEST_RESULT,
                arguments = listOf(navArgument("testId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val testId = backStackEntry.arguments?.getString("testId").orEmpty()
                MockTestResultScreen(
                    repository = mockTestRepository,
                    testId = testId,
                    onBack = { navController.popBackStack() },
                    onReview = { reviewTestId -> navController.navigate("mock_test_review/$reviewTestId") },
                )
            }
        }
    }
}

private fun NavController.navigateToQuestion(subject: Subject, topic: String, difficulty: Difficulty) {
    val encodedTopic = URLEncoder.encode(topic, StandardCharsets.UTF_8.name())
    navigate("question/${subject.name}/$encodedTopic/${difficulty.name}")
}

@Composable
private fun NeetBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    val icon = when (tab.route) {
                        ROUTE_PICKER -> Icons.Filled.Edit
                        ROUTE_MOCK_TEST_HOME -> Icons.AutoMirrored.Filled.List
                        else -> Icons.Filled.CheckCircle
                    }
                    Icon(icon, contentDescription = tab.label)
                },
                label = { Text(tab.label) },
            )
        }
    }
}
