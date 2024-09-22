package de.kitshn.android.ui.dialog.mealplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kitshn.android.R
import de.kitshn.android.api.tandoor.model.TandoorMealPlan
import de.kitshn.android.api.tandoor.rememberTandoorRequestState
import de.kitshn.android.ui.TandoorRequestErrorHandler
import de.kitshn.android.ui.component.icons.IconWithState
import de.kitshn.android.ui.component.model.mealplan.MealPlanDetailsCard
import de.kitshn.android.ui.dialog.AdaptiveFullscreenDialog
import de.kitshn.android.ui.dialog.recipe.RecipeLinkDialog
import de.kitshn.android.ui.dialog.recipe.rememberRecipeLinkDialogState
import de.kitshn.android.ui.state.foreverRememberNotSavable
import de.kitshn.android.ui.view.ViewParameters
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberMealPlanDetailsDialogState(): MealPlanDetailsDialogState {
    return remember {
        MealPlanDetailsDialogState()
    }
}

class MealPlanDetailsDialogState(
    val shown: MutableState<Boolean> = mutableStateOf(false),
    val linkContent: MutableState<TandoorMealPlan?> = mutableStateOf(null)
) {
    fun open(linkContent: TandoorMealPlan) {
        this.linkContent.value = linkContent
        this.shown.value = true
    }

    fun dismiss() {
        this.shown.value = false
        this.linkContent.value = null
    }
}

@Composable
fun MealPlanDetailsDialog(
    p: ViewParameters,
    state: MealPlanDetailsDialogState,
    reopenOnLaunchKey: String? = null,
    onUpdateList: () -> Unit,
    onEdit: (mealPlan: TandoorMealPlan) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val requestMealPlanDeleteState = rememberTandoorRequestState()

    if(reopenOnLaunchKey != null) {
        var reopenOnLaunch by foreverRememberNotSavable<TandoorMealPlan?>(key = reopenOnLaunchKey)
        DisposableEffect(Unit) {
            onDispose {
                reopenOnLaunch = state.shown.value.run {
                    if(!this)
                        null
                    else
                        state.linkContent.value
                }
            }
        }

        LaunchedEffect(Unit) {
            if(reopenOnLaunch == null) return@LaunchedEffect
            state.open(reopenOnLaunch!!)

            reopenOnLaunch = null
        }
    }

    if(state.linkContent.value == null) return
    val mealPlan = state.linkContent.value!!

    val bottomBar = @Composable {
        BottomAppBar(
            floatingActionButton = {
                if(mealPlan.recipe != null) FloatingActionButton(
                    onClick = {
                        p.vm.navHostController?.navigate("recipe/${mealPlan.recipe.id}/cook/${mealPlan.servings.roundToInt()}")
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        0.dp, 0.dp, 0.dp, 0.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalDining,
                        contentDescription = stringResource(R.string.action_start_cooking)
                    )
                }
            },
            actions = {
                FilterChip(
                    modifier = Modifier.padding(start = 16.dp),
                    onClick = { },
                    label = {
                        Text(
                            text = mealPlan.meal_type_name
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedLabelColor = mealPlan.meal_type.color,
                        selectedContainerColor = mealPlan.meal_type.color.copy(alpha = 0.2f)
                    ),
                    selected = true
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            onEdit(mealPlan)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit)
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val data = requestMealPlanDeleteState.wrapRequest {
                                    mealPlan.delete()
                                }

                                if(data == null) return@launch

                                state.dismiss()
                                requestMealPlanDeleteState.reset()

                                onUpdateList()
                            }
                        }
                    ) {
                        IconWithState(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.action_delete_from_meal_plan),
                            state = requestMealPlanDeleteState.state.toIconWithState()
                        )
                    }

                    if(mealPlan.recipe != null) Spacer(Modifier.width(16.dp))
                }
            }
        )
    }

    if(mealPlan.recipe != null) {
        val recipeLinkDialogState = rememberRecipeLinkDialogState()

        LaunchedEffect(state.shown.value) {
            if(state.shown.value) {
                recipeLinkDialogState.open(
                    linkContent = mealPlan.recipe,
                    overrideServings = mealPlan.servings.roundToInt()
                )
            } else {
                recipeLinkDialogState.dismiss()
            }
        }

        LaunchedEffect(recipeLinkDialogState.shown.value) {
            state.shown.value = recipeLinkDialogState.shown.value
        }

        RecipeLinkDialog(
            p = p,
            state = recipeLinkDialogState,
            leadingContent = {
                MealPlanDetailsCard(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    mealPlan = mealPlan
                )
            },
            bottomBar = {
                bottomBar()
            },
            hideFab = true
        ) {
            state.dismiss()
        }
    } else {
        AdaptiveFullscreenDialog(
            onDismiss = {
                state.dismiss()
            },
            title = { },
            bottomBar = {
                bottomBar()
            }
        ) { _, _, _ ->
            MealPlanDetailsCard(
                modifier = Modifier.padding(16.dp),
                mealPlan = mealPlan
            )
        }
    }

    TandoorRequestErrorHandler(state = requestMealPlanDeleteState)
}