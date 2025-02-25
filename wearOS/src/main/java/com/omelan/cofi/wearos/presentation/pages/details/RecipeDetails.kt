@file:OptIn(ExperimentalFoundationApi::class)

package com.omelan.cofi.wearos.presentation.pages.details


import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.composable
import com.omelan.cofi.share.*
import com.omelan.cofi.share.model.*
import com.omelan.cofi.share.pages.Destinations
import com.omelan.cofi.share.timer.Timer
import com.omelan.cofi.share.utils.toStringDuration
import com.omelan.cofi.wearos.R
import com.omelan.cofi.wearos.presentation.LocalAmbientModeProvider
import kotlin.math.roundToInt

fun NavGraphBuilder.recipeDetails(
    edgeSwipeToDismissBoxState: SwipeToDismissBoxState,
    window: Window,
) {
    composable(
        Destinations.RECIPE_DETAILS,
        arguments = listOf(navArgument("recipeId") { type = NavType.IntType }),
    ) {
        var edgeSwipeWidth by remember { mutableStateOf(0.dp) }
        val id = it.arguments?.getInt("recipeId")
            ?: throw Exception("Expected recipe id, got Null")
        RecipeDetails(
            modifier = Modifier.edgeSwipeToDismiss(
                edgeSwipeToDismissBoxState,
                edgeSwipeWidth,
            ),
            recipeId = id,
            onTimerRunning = { isTimerRunning ->
                val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                if (isTimerRunning) {
                    window.addFlags(flag)
                } else {
                    window.clearFlags(flag)
                }
            },
            canSwipeToClose = { canSwipeToClose ->
                edgeSwipeWidth =
                    if (canSwipeToClose) {
                        SwipeToDismissBoxDefaults.EdgeWidth
                    } else {
                        0.dp
                    }
            },
        )
    }
}

const val PAGE_COUNT = 3

@Composable
fun RecipeDetails(
    modifier: Modifier = Modifier, recipeId: Int,
    onTimerRunning: (Boolean) -> Unit, canSwipeToClose: (Boolean) -> Unit,
) {
    val recipeViewModel: RecipeViewModel = viewModel()
    val stepsViewModel: StepsViewModel = viewModel()
    val recipe by recipeViewModel.getRecipe(recipeId).observeAsState(initial = Recipe(name = ""))
    val steps by stepsViewModel.getAllStepsForRecipe(recipeId).observeAsState(listOf())
    RecipeDetails(
        modifier = modifier,
        recipe = recipe,
        steps = steps,
        onTimerRunning = onTimerRunning,
        canSwipeToClose = canSwipeToClose,
    )
}

@Composable
fun RecipeDetails(
    modifier: Modifier = Modifier,
    recipe: Recipe,
    steps: List<Step>,
    onTimerRunning: (Boolean) -> Unit,
    canSwipeToClose: (Boolean) -> Unit,
) {
    val dataStore = DataStore(LocalContext.current)

    val ambientController = LocalAmbientModeProvider.current
    var weightMultiplier by remember { mutableStateOf(1.0f) }
    var timeMultiplier by remember { mutableStateOf(1.0f) }
    val combinedTime by remember(steps) {
        derivedStateOf {
            steps.sumOf { it.time ?: 0 }
        }
    }
    val combinedWaterWeight by remember(steps) {
        derivedStateOf {
            steps.sumOf {
                if (it.type == StepType.WATER) {
                    it.value?.toDouble() ?: 0.0
                } else {
                    0.0
                }
            }
        }
    }
    val combinedCoffeeWeight by remember(steps) {
        derivedStateOf {
            steps.sumOf {
                if (it.type == StepType.ADD_COFFEE) {
                    it.value?.toDouble() ?: 0.0
                } else {
                    0.0
                }
            }
        }
    }
    val pagerState = rememberPagerState()
    val animatedSelectedPage by animateFloatAsState(
        targetValue = pagerState.currentPage.toFloat(),
        animationSpec = TweenSpec(durationMillis = 500),
        label = "Selected page",
    )
    val pageIndicatorState: PageIndicatorState = remember {
        object : PageIndicatorState {
            override val pageOffset: Float
                get() = animatedSelectedPage - pagerState.currentPage
            override val selectedPage: Int
                get() = pagerState.currentPage
            override val pageCount: Int
                get() = PAGE_COUNT
        }
    }
    val timerControllers = Timer.createTimerControllers(
        steps = steps,
        onRecipeEnd = { },
        dataStore = dataStore,
        doneTrackColor = MaterialTheme.colors.primary,
        timeMultiplier = timeMultiplier,
    )
    val context = LocalContext.current
    val ambientEnabled: Boolean = remember(LocalLifecycleOwner.current) {
        try {
            Settings.Global.getInt(context.contentResolver, "ambient_enabled") == 1
        } catch (e: SettingNotFoundException) {
            false
        }
    }

    LaunchedEffect(timerControllers.isTimerRunning) {
        if (ambientEnabled) {
            ambientController?.setAmbientOffloadEnabled(timerControllers.isTimerRunning)
        }
        onTimerRunning(timerControllers.isTimerRunning)
    }
    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            ambientController?.setAmbientOffloadEnabled(false)
        }
    }
    LaunchedEffect(ambientController?.isAmbient) {
        if (ambientController?.isAmbient == true) {
            pagerState.scrollToPage(0)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            canSwipeToClose(true)
        } else {
            if (timerControllers.isTimerRunning) {
                timerControllers.pauseAnimations()
            }
            canSwipeToClose(false)
        }
    }
    LaunchedEffect(timerControllers.currentStep) {
        timerControllers.progressAnimation(Unit)
    }

    Scaffold(
        timeText = {
            TimeText()
        },
        pageIndicator = {
            HorizontalPageIndicator(
                pageIndicatorState = pageIndicatorState,
                modifier = Modifier.padding(10.dp),
            )
        },
    ) {
        HorizontalPager(pageCount = PAGE_COUNT, modifier = modifier, state = pagerState) { page ->
            when (page) {
                0 -> TimerPage(
                    timerControllers = timerControllers,
                    allSteps = steps,
                    recipe = recipe,
                    dataStore = dataStore,
                    weightMultiplier = weightMultiplier,
                    timeMultiplier = timeMultiplier,
                )

                1 -> Row {
                    MultiplierPage(
                        multiplier = weightMultiplier,
                        changeMultiplier = { weightMultiplier = it },
                        requestFocus = pagerState.currentPage == 1,
                    ) {
                        Text(text = "x$it")
                        ParamWithIcon(
                            iconRes = R.drawable.ic_water,
                            value = "${(combinedWaterWeight * it).roundToInt()}g",
                        )
                        ParamWithIcon(
                            iconRes = R.drawable.ic_coffee,
                            value = "${(combinedCoffeeWeight * it).roundToInt()}g",
                        )
                    }
                }

                2 -> MultiplierPage(
                    multiplier = timeMultiplier,
                    changeMultiplier = { timeMultiplier = it },
                    requestFocus = pagerState.currentPage == 2,
                ) {
                    Text(text = "x$it")
                    ParamWithIcon(
                        iconRes = R.drawable.ic_timer,
                        value = (combinedTime * it).roundToInt().toStringDuration(),
                    )
                }
            }
        }
    }
}
