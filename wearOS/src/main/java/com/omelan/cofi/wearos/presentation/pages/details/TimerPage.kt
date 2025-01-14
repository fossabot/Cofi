package com.omelan.cofi.wearos.presentation.pages.details

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.omelan.cofi.share.COMBINE_WEIGHT_DEFAULT_VALUE
import com.omelan.cofi.share.DataStore
import com.omelan.cofi.share.R
import com.omelan.cofi.share.components.StepNameText
import com.omelan.cofi.share.components.TimeText
import com.omelan.cofi.share.components.TimerValue
import com.omelan.cofi.share.model.Recipe
import com.omelan.cofi.share.model.Step
import com.omelan.cofi.share.timer.Timer
import com.omelan.cofi.share.timer.TimerControllers
import com.omelan.cofi.wearos.presentation.LocalAmbientModeProvider
import com.omelan.cofi.wearos.presentation.components.ListenKeyEvents
import com.omelan.cofi.wearos.presentation.components.StartFAB
import kotlinx.coroutines.launch

@Composable
fun TimerPage(
    timerControllers: TimerControllers,
    allSteps: List<Step>,
    recipe: Recipe,
    dataStore: DataStore,
    weightMultiplier: Float,
    timeMultiplier: Float,
) {
    val (
        currentStep,
        _,
        isDone,
        isTimerRunning,
        _,
        animatedProgressValue,
        animatedProgressColor,
        pauseAnimations,
        _,
        startAnimations,
        changeToNextStep,
    ) = timerControllers

    var showDescriptionDialog by remember { mutableStateOf(false) }

    val combineWeightState by dataStore.getWeightSetting()
        .collectAsState(initial = COMBINE_WEIGHT_DEFAULT_VALUE)
    val coroutineScope = rememberCoroutineScope()
    val ambientController = LocalAmbientModeProvider.current
    val isAmbient = remember {
        derivedStateOf {
            ambientController?.isAmbient ?: false
        }
    }

    val alreadyDoneWeight by Timer.rememberAlreadyDoneWeight(
        indexOfCurrentStep = allSteps.indexOf(currentStep),
        allSteps = allSteps,
        combineWeightState = combineWeightState,
        weightMultiplier = weightMultiplier,
    )
    val startButtonOnClick: () -> Unit = {
        if (currentStep != null) {
            if (animatedProgressValue.isRunning) {
                coroutineScope.launch { pauseAnimations() }
            } else {
                coroutineScope.launch {
                    if (currentStep.time == null) {
                        changeToNextStep(false)
                    } else {
                        startAnimations()
                    }
                }
            }
        } else
            coroutineScope.launch { changeToNextStep(true) }
    }

    ListenKeyEvents { keyCode, event ->
        if (event.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1,
                KeyEvent.KEYCODE_STEM_2,
                KeyEvent.KEYCODE_STEM_3,
                -> {
                    startButtonOnClick()
                    true
                }

                else -> false
            }
        } else {
            false
        }
    }
    Dialog(
        showDialog = showDescriptionDialog,
        onDismissRequest = { showDescriptionDialog = false },
    ) {
        Alert(
            scrollState = rememberScalingLazyListState(
                initialCenterItemIndex = 0,
                initialCenterItemScrollOffset = 0,
            ),
            contentPadding = PaddingValues(18.dp),
            icon = {
                Icon(
                    painter = painterResource(id = recipe.recipeIcon.icon),
                    contentDescription = "",
                )
            },
            title = { Text(text = recipe.name) },
        ) {
            item {
                Text(text = recipe.description)
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            progress = if (isDone) 1f else animatedProgressValue.value,
            indicatorColor = if (isAmbient.value) {
                Color.White
            } else {
                animatedProgressColor.value
            },
            trackColor = if (isAmbient.value) {
                MaterialTheme.colors.onBackground.copy(alpha = 0.1f)
            } else {
                animatedProgressColor.value.copy(alpha = 0.2f)
            },
            startAngle = 300f,
            endAngle = 240f,
        )
        Column(
            modifier = Modifier
                .fillMaxRectangle()
                .animateContentSize(),
            Arrangement.SpaceBetween,
            Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = isDone, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.timer_enjoy),
                        color = MaterialTheme.colors.onSurface,
                        maxLines = 2,
                        style = MaterialTheme.typography.title1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_coffee),
                        contentDescription = "",
                    )
                }
            }
            AnimatedVisibility(
                visible = currentStep == null && !isDone,
                modifier = Modifier.weight(1f, true),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = recipe.name,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = if (recipe.description.isNotBlank()) 1 else 2,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.title1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (recipe.description.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showDescriptionDialog = true },
                                modifier = Modifier.height(ButtonDefaults.ExtraSmallButtonSize),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.recipe_details_read_description),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }
                    }
//                    Text(
//                        text = recipe.description,
//                        color = MaterialTheme.colors.onSurface,
//                        maxLines = 2,
//                        textAlign = TextAlign.Center,
//                        style = MaterialTheme.typography.title1,
//                        overflow = TextOverflow.Ellipsis,
//                    )
                }
            }
            AnimatedVisibility(
                visible = currentStep != null && !isDone,
            ) {
                Column {
                    if (currentStep != null) {
                        TimeText(
                            currentStep = currentStep,
                            animatedProgressValue = animatedProgressValue.value * timeMultiplier,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 2,
                            style = MaterialTheme.typography.title2,
                            paddingHorizontal = 2.dp,
                            showMillis = false,
                        )
                        StepNameText(
                            currentStep = currentStep,
                            timeMultiplier = timeMultiplier,
                            color = MaterialTheme.colors.onSurface,
                            style = MaterialTheme.typography.title3,
                            maxLines = 1,
                            paddingHorizontal = 2.dp,
                        )
                        TimerValue(
                            currentStep = currentStep,
                            animatedProgressValue = animatedProgressValue.value,
                            weightMultiplier = weightMultiplier,
                            alreadyDoneWeight = alreadyDoneWeight,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 1,
                            style = MaterialTheme.typography.title1,
                        )
                    }
                }
            }
            AnimatedVisibility(visible = !isAmbient.value) {
                Spacer(Modifier.height(12.dp))
                StartFAB(isTimerRunning, startButtonOnClick)
            }
        }
    }
}
