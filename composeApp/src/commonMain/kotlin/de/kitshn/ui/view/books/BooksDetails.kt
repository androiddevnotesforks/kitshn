package de.kitshn.ui.view.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import de.kitshn.api.tandoor.model.TandoorKeywordOverview
import de.kitshn.api.tandoor.model.TandoorRecipeBook
import de.kitshn.api.tandoor.model.TandoorRecipeBookEntry
import de.kitshn.api.tandoor.model.recipe.TandoorRecipeOverview
import de.kitshn.api.tandoor.rememberTandoorRequestState
import de.kitshn.isScrollingUp
import de.kitshn.reachedBottom
import de.kitshn.ui.TandoorRequestErrorHandler
import de.kitshn.ui.component.alert.FullSizeAlertPane
import de.kitshn.ui.component.model.recipe.RecipeCard
import de.kitshn.ui.component.model.recipe.RecipeCardInfoTag
import de.kitshn.ui.dialog.recipeBook.RecipeBookCreationAndEditDialog
import de.kitshn.ui.dialog.recipeBook.rememberRecipeBookCreationDialogState
import de.kitshn.ui.dialog.recipeBook.rememberRecipeBookEditDialogState
import de.kitshn.ui.dialog.select.SelectRecipeDialog
import de.kitshn.ui.dialog.select.rememberSelectRecipeDialogState
import de.kitshn.ui.selectionMode.rememberSelectionModeState
import de.kitshn.ui.view.ViewParameters
import kitshn.composeapp.generated.resources.Res
import kitshn.composeapp.generated.resources.action_add
import kitshn.composeapp.generated.resources.recipe_book_empty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBooksDetails(
    p: ViewParameters,
    book: TandoorRecipeBook,
    isFavoriteBook: Boolean,
    onClickKeyword: (keyword: TandoorKeywordOverview) -> Unit = {},
    onClick: (recipeOverview: TandoorRecipeOverview) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val lazyStaggeredGridState = rememberLazyStaggeredGridState()

    val selectionModeState = rememberSelectionModeState<Int>()

    val createEntryRequestState = rememberTandoorRequestState()
    val deleteRequestState = rememberTandoorRequestState()

    val selectRecipeDialogState = rememberSelectRecipeDialogState()
    val creationDialogState =
        rememberRecipeBookCreationDialogState(key = "RouteMainSubrouteBooks/creationDialogState")
    val editDialogState =
        rememberRecipeBookEditDialogState(key = "RouteMainSubrouteBooks/editDialogState")

    val recipes = remember { mutableStateListOf<TandoorRecipeBookEntry>() }
    LaunchedEffect(book.entries.toList()) {
        recipes.clear()
        recipes.addAll(book.entries)

        try {
            val favoritesRecipeBookId = p.vm.favorites.getFavoritesRecipeBookId()
            if(book.id == favoritesRecipeBookId) recipes.reverse()
        } catch(e: Error) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) { book.listEntries() }

    val listFilterRecipesRequestState = rememberTandoorRequestState()
    var nextPageExists by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(1) }

    val filterRecipes = remember { mutableStateListOf<TandoorRecipeOverview>() }

    val reachedBottom by remember { derivedStateOf { lazyStaggeredGridState.reachedBottom() } }

    var fetchNewItems by remember { mutableStateOf(true) }
    LaunchedEffect(reachedBottom) {
        if(reachedBottom) {
            fetchNewItems = true
        }
    }

    LaunchedEffect(fetchNewItems) {
        while(fetchNewItems && nextPageExists) {
            listFilterRecipesRequestState.wrapRequest {
                book.listFilterEntries(
                    page = currentPage
                )
            }?.let {
                nextPageExists = it.next != null
                currentPage++

                it.results.forEach { recipe -> filterRecipes.add(recipe) }

                if(!reachedBottom) fetchNewItems = false
            }

            delay(500)
        }
    }

    Scaffold(
        topBar = {
            ViewBooksDetailsTopAppBar(
                book = book,
                isFavoriteBook = isFavoriteBook,

                scrollBehavior = scrollBehavior,

                selectionModeState = selectionModeState,
                editDialogState = editDialogState,
                deleteRequestState = deleteRequestState,

                onBack = p.back
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = lazyStaggeredGridState.isScrollingUp(),
                icon = {
                    Icon(Icons.Rounded.Add, stringResource(Res.string.action_add))
                },
                text = {
                    Text(stringResource(Res.string.action_add))
                },
                onClick = {
                    selectRecipeDialogState.open()
                }
            )
        }
    ) { pv ->
        if(book.entries.size == 0) {
            Box(
                Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                FullSizeAlertPane(
                    imageVector = Icons.Rounded.Receipt,
                    contentDescription = stringResource(Res.string.recipe_book_empty),
                    text = stringResource(Res.string.recipe_book_empty)
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(pv),
                state = lazyStaggeredGridState,
                columns = StaggeredGridCells.Adaptive(minSize = 200.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if(book.description.isNotBlank() && !isFavoriteBook) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(text = book.description)
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                items(recipes.size, key = { recipes[it].id }) { index ->
                    val entry = recipes[index]

                    RecipeCard(
                        recipeOverview = entry.recipe_content,
                        selectionState = selectionModeState,
                        onClickKeyword = onClickKeyword,
                        onClick = { onClick(entry.recipe_content) }
                    )
                }

                items(filterRecipes.size, key = { "filter-${filterRecipes[it].id}" }) { index ->
                    val entry = filterRecipes[index]

                    RecipeCard(
                        recipeOverview = entry,
                        onClickKeyword = onClickKeyword,
                        additionalCardInfoTagsEnd = {
                            RecipeCardInfoTag(
                                hazeState = it
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(16.dp),
                                    imageVector = Icons.Rounded.FilterAlt,
                                    contentDescription = ""
                                )
                            }
                        },
                        onClick = { onClick(entry) }
                    )
                }
            }
        }
    }

    if(p.vm.tandoorClient != null) {
        RecipeBookCreationAndEditDialog(
            client = p.vm.tandoorClient!!,
            creationState = creationDialogState,
            editState = editDialogState
        ) { p.back?.let { it() } }

        SelectRecipeDialog(
            client = p.vm.tandoorClient!!,
            state = selectRecipeDialogState
        ) {
            coroutineScope.launch {
                createEntryRequestState.wrapRequest { book.createEntry(it.id) }

                delay(100)
                createEntryRequestState.reset()
            }
        }
    }

    TandoorRequestErrorHandler(state = createEntryRequestState)
    TandoorRequestErrorHandler(state = deleteRequestState)
}