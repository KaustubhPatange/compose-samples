/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.jetnews.data.AppContainer
import com.example.jetnews.ui.theme.JetnewsTheme
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kpstv.navigation.compose.ComposeNavigator
import com.kpstv.navigation.compose.EnterAnimation
import com.kpstv.navigation.compose.ExitAnimation
import com.kpstv.navigation.compose.rememberController
import kotlinx.coroutines.launch

@Composable
fun JetnewsApp(
    appContainer: AppContainer,
    navigator: ComposeNavigator
) {
    JetnewsTheme {
        ProvideWindowInsets {
            val systemUiController = rememberSystemUiController()
            SideEffect {
                systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
            }

            val controller = rememberController<MainRoute>()
            val coroutineScope = rememberCoroutineScope()
            // This top level scaffold contains the app drawer, which needs to be accessible
            // from multiple screens. An event to open the drawer is passed down to each
            // screen that needs it.
            val scaffoldState = rememberScaffoldState()

            val currentRoute = remember { mutableStateOf(MainRoute.Home() as MainRoute) }
            Scaffold(
                scaffoldState = scaffoldState,
                drawerContent = {
                    AppDrawer(
                        currentRoute = currentRoute.value,
                        navigateToHome = { controller.goBack() },
                        navigateToInterests = {
                            controller.navigateTo(MainRoute.Interest()) {
                                withAnimation { enter = EnterAnimation.FadeIn; exit = ExitAnimation.FadeOut }
                            }
                        },
                        closeDrawer = { coroutineScope.launch { scaffoldState.drawerState.close() } }
                    )
                }
            ) {
                navigator.Setup(key = MainRoute.key, initial = MainRoute.Home(), controller = controller) { _, dest ->
                    currentRoute.value = dest

                    JetNewsNavigation(
                        appContainer = appContainer,
                        controller = controller,
                        scaffoldState = scaffoldState,
                        currentDestination = dest
                    )
                }
            }
        }
    }
}
