@file:OptIn(ExperimentalMaterial3Api::class)

package com.omelan.cofi.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

@Composable
fun Material3BottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    BackHandler {
        coroutineScope.launch {
            sheetState.hide()
            onDismissRequest()
        }
    }
    val modalBottomSheetShapeDp by animateDpAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Hidden) 0.dp else 28.dp,
        animationSpec = tween(),
        label = "Bottom Sheet top radii",
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Hidden) 0f else 0.32f,
        animationSpec = tween(),
        label = "scrim alpha",
    )
    val modalBottomSheetShape =
        RoundedCornerShape(topEnd = modalBottomSheetShapeDp, topStart = modalBottomSheetShapeDp)
    // TODO: remove me when material 3 starts respecting navigation bar
    val scrimColor = MaterialTheme.colorScheme.scrim
    val contentColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    val systemUiController = rememberSystemUiController()
    val window = (LocalContext.current as Activity).window
    DisposableEffect(sheetState.currentValue) {
        val oldNavColor = Color(window.navigationBarColor)
        if (sheetState.currentValue != SheetValue.Hidden) {
            systemUiController.setNavigationBarColor(contentColor)
        } else {
            systemUiController.setNavigationBarColor(oldNavColor)
        }
        onDispose {
            systemUiController.setNavigationBarColor(oldNavColor)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(3f)
            .drawBehind {
                drawRect(color = scrimColor, alpha = scrimAlpha)
            },
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            shape = modalBottomSheetShape,
            sheetState = sheetState,
            content = content,
            scrimColor = Color.Transparent,
        )
    }
}
