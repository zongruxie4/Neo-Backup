package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun StatefulAnimatedVisibility(
    currentState: Boolean = false,
    enterPositive: EnterTransition,
    exitPositive: ExitTransition,
    enterNegative: EnterTransition,
    exitNegative: ExitTransition,
    expandedView: @Composable (AnimatedVisibilityScope.() -> Unit),
    collapsedView: @Composable (AnimatedVisibilityScope.() -> Unit),
) {
    AnimatedVisibility(
        visible = currentState,
        enter = enterPositive,
        exit = exitPositive,
        content = expandedView
    )
    AnimatedVisibility(
        visible = !currentState,
        enter = enterNegative,
        exit = exitNegative,
        content = collapsedView
    )
}

@Composable
fun HorizontalExpandingVisibility(
    expanded: Boolean = false,
    expandedView: @Composable (AnimatedVisibilityScope.() -> Unit),
    collapsedView: @Composable (AnimatedVisibilityScope.() -> Unit),
) = StatefulAnimatedVisibility(
    currentState = expanded,
    enterPositive = expandHorizontally(expandFrom = Alignment.End),
    exitPositive = shrinkHorizontally(shrinkTowards = Alignment.End),
    enterNegative = expandHorizontally(expandFrom = Alignment.Start),
    exitNegative = shrinkHorizontally(shrinkTowards = Alignment.Start),
    collapsedView = collapsedView,
    expandedView = expandedView
)

@Composable
fun VerticalFadingVisibility(
    expanded: Boolean = false,
    expandedView: @Composable (AnimatedVisibilityScope.() -> Unit),
    collapsedView: @Composable (AnimatedVisibilityScope.() -> Unit),
) = StatefulAnimatedVisibility(
    currentState = expanded,
    enterPositive = fadeIn() + expandVertically(expandFrom = Alignment.Top),
    exitPositive = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    enterNegative = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
    exitNegative = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
    collapsedView = collapsedView,
    expandedView = expandedView
)

@Composable
fun ExpandingFadingVisibility(
    expanded: Boolean = false,
    expandedView: @Composable (AnimatedVisibilityScope.() -> Unit),
    collapsedView: @Composable (AnimatedVisibilityScope.() -> Unit),
) {
    val initColor = FloatingActionButtonDefaults.containerColor
    val expandedColor = MaterialTheme.colorScheme.surface
    val bgColor = remember {
        Animatable(initColor)
    }
    LaunchedEffect(key1 = expanded) {
        bgColor.animateTo(if (expanded) expandedColor else initColor)
    }
    Column(
        modifier = Modifier
            .shadow(
                elevation = 6.dp,
                MaterialTheme.shapes.large
            )
            .background(
                bgColor.value,
                MaterialTheme.shapes.large
            )
    ) {
        StatefulAnimatedVisibility(
            currentState = expanded,
            enterPositive = fadeIn() + expandIn(),
            exitPositive = fadeOut() + shrinkOut(),
            enterNegative = fadeIn() + expandIn(),
            exitNegative = fadeOut() + shrinkOut(),
            collapsedView = collapsedView,
            expandedView = expandedView
        )
    }
}