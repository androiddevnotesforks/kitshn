package de.kitshn.android.ui.route.recipe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import de.kitshn.android.R
import de.kitshn.android.api.tandoor.rememberTandoorRequestState
import de.kitshn.android.ui.TandoorRequestErrorHandler
import de.kitshn.android.ui.component.alert.FullSizeAlertPane
import de.kitshn.android.ui.component.buttons.BackButton
import de.kitshn.android.ui.route.RouteParameters
import de.kitshn.android.ui.view.ViewParameters
import de.kitshn.android.ui.view.recipe.details.ViewRecipeDetails

@Composable
fun RouteRecipeView(
    p: RouteParameters
) {
    val recipeId = p.bse.arguments?.getString("recipeId")
    if(recipeId == null) {
        FullSizeAlertPane(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = stringResource(R.string.recipe_not_found),
            text = stringResource(R.string.recipe_not_found)
        )

        return
    }

    val client = p.vm.tandoorClient ?: return

    // retrieve shared recipe
    val requestState = rememberTandoorRequestState()
    LaunchedEffect(recipeId) {
        requestState.wrapRequest {
            val recipe = client.recipe.get(id = recipeId.toInt())
            client.container.recipe[recipe.id] = recipe
            client.container.recipeOverview[recipe.id] = recipe.toOverview()
        }
    }

    ViewRecipeDetails(
        p = ViewParameters(p.vm, p.onBack),

        navigationIcon = {
            BackButton(onBack = p.onBack, overlay = true)
        },

        recipeId = recipeId.toInt(),
        client = client,

        onClickKeyword = {
            p.vm.searchKeyword(id = it.id)
        }
    )

    TandoorRequestErrorHandler(requestState)
}