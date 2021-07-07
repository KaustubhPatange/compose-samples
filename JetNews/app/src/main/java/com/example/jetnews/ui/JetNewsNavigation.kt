/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.jetnews.data.AppContainer
import com.example.jetnews.ui.article.ArticleScreen
import com.example.jetnews.ui.home.HomeScreen
import com.example.jetnews.ui.interests.InterestsScreen
import com.kpstv.navigation.compose.ComposeNavigator
import com.kpstv.navigation.compose.Fade
import com.kpstv.navigation.compose.Route
import com.kpstv.navigation.compose.SlideRight
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Destinations used in the ([JetnewsApp]).
 */
sealed class MainRoute : Route {
    @Parcelize @Immutable
    data class Home(private val noArg: String = "") : MainRoute()
    @Parcelize @Immutable
    data class Interest(private val noArg: String = "") : MainRoute()
    @Parcelize @Immutable
    data class Article(val postId: String): MainRoute()

    companion object { val key = MainRoute::class }
}

@Composable
fun JetNewsNavigation(
    appContainer: AppContainer,
    controller: ComposeNavigator.Controller<MainRoute>,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    currentDestination: MainRoute
) {
    val actions = remember(controller) { MainActions(controller) }
    val coroutineScope = rememberCoroutineScope()
    val openDrawer: () -> Unit = { coroutineScope.launch { scaffoldState.drawerState.open() } }

    when(currentDestination) {
        is MainRoute.Home -> HomeScreen(
            postsRepository = appContainer.postsRepository,
            navigateToArticle = actions.navigateToArticle,
            openDrawer = openDrawer
        )
        is MainRoute.Interest -> InterestsScreen(
            interestsRepository = appContainer.interestsRepository,
            openDrawer = openDrawer
        )
        is MainRoute.Article ->  ArticleScreen(
            postId = currentDestination.postId,
            onBack = actions.upPress,
            postsRepository = appContainer.postsRepository
        )
    }
}

/**
 * Models the navigation actions in the app.
 */
class MainActions(controller: ComposeNavigator.Controller<MainRoute>) {
    val navigateToArticle: (String) -> Unit = { postId: String ->
        controller.navigateTo(MainRoute.Article(postId)) {
            withAnimation {
                target = SlideRight
                current = Fade
            }
        }
    }
    val upPress: () -> Unit = {
        controller.goBack()
    }
}
