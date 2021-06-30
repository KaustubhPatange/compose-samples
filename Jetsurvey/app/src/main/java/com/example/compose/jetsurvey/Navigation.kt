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

package com.example.compose.jetsurvey

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.compose.jetsurvey.signinsignup.*
import com.example.compose.jetsurvey.survey.*
import com.google.android.material.datepicker.MaterialDatePicker
import com.kpstv.navigation.compose.*
import kotlinx.parcelize.Parcelize

sealed class MainRoute : Route {
    @Parcelize @Immutable
    data class Welcome(private val noArg: String = "") : MainRoute()
    @Parcelize @Immutable
    data class SignUp(private val noArg: String = "") : MainRoute()
    @Parcelize @Immutable
    data class SignIn(private val noArg: String = "") : MainRoute()
    @Parcelize @Immutable
    data class Survey(private val noArg: String = "") : MainRoute()

    companion object { val key = MainRoute::class }
}

@Composable
fun MainScreen(navigator: ComposeNavigator) {
    navigator.Setup(key = MainRoute.key, initial = MainRoute.Welcome()) { _, dest ->
        when(dest) {
            is MainRoute.Welcome -> RouteWelcome()
            is MainRoute.SignIn -> RouteSignIn()
            is MainRoute.SignUp -> RouteSignUp()
            is MainRoute.Survey -> RouteSurvey()
        }
    }
}

@Composable
fun RouteWelcome() {
    val controller = findController(MainRoute.key)
    val viewModel = viewModel<WelcomeViewModel>(factory = WelcomeViewModelFactory())
    viewModel.navigateTo.observe(LocalLifecycleOwner.current) { event ->
        event.getContentIfNotHandled()?.let { route ->
            controller.navigateTo(route) {
                withAnimation {
                    enter = EnterAnimation.FadeIn
                    exit = ExitAnimation.FadeOut
                }
            }
        }
    }
    WelcomeScreen(
        onEvent = { event ->
            when (event) {
                is WelcomeEvent.SignInSignUp -> viewModel.handleContinue(
                    event.email
                )
                WelcomeEvent.SignInAsGuest -> viewModel.signInAsGuest()
            }
        }
    )
}

@Composable
fun RouteSignIn() {
    val controller = findController(MainRoute.key)
    val viewModel = viewModel<SignInViewModel>(factory = SignInViewModelFactory())
    viewModel.navigateTo.observe(LocalLifecycleOwner.current) { event ->
        event.getContentIfNotHandled()?.let { route ->
            controller.navigateTo(route) {
                withAnimation {
                    enter = EnterAnimation.FadeIn
                    exit = ExitAnimation.FadeOut
                }
            }
        }
    }
    SignIn(
        onNavigationEvent = { event ->
            when (event) {
                is SignInEvent.SignIn -> {
                    viewModel.signIn(event.email, event.password)
                }
                SignInEvent.SignUp -> {
                    viewModel.signUp()
                }
                SignInEvent.SignInAsGuest -> {
                    viewModel.signInAsGuest()
                }
                SignInEvent.NavigateBack -> {
                    controller.goBack()
                }
            }
        }
    )
}

@Composable
fun RouteSignUp() {
    val controller = findController(MainRoute.key)
    val viewModel = viewModel<SignUpViewModel>(factory = SignUpViewModelFactory())
    viewModel.navigateTo.observe(LocalLifecycleOwner.current) { event ->
        event.getContentIfNotHandled()?.let { route ->
            controller.navigateTo(route) {
                withAnimation {
                    enter = EnterAnimation.FadeIn
                    exit = ExitAnimation.FadeOut
                }
            }
        }
    }
    SignUp(
        onNavigationEvent = { event ->
            when (event) {
                is SignUpEvent.SignUp -> {
                    viewModel.signUp(event.email, event.password)
                }
                SignUpEvent.SignIn -> {
                    viewModel.signIn()
                }
                SignUpEvent.SignInAsGuest -> {
                    viewModel.signInAsGuest()
                }
                SignUpEvent.NavigateBack -> {
                    controller.goBack()
                }
            }
        }
    )
}

@Composable
fun RouteSurvey() {
    val controller = findController(MainRoute.key)
    val viewModel = viewModel<SurveyViewModel>(
        factory = SurveyViewModelFactory(PhotoUriManager(LocalContext.current.applicationContext))
    )

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { photoSaved ->
        if (photoSaved) {
            viewModel.onImageSaved()
        }
    }

    fun takeAPhoto() {
        takePicture.launch(viewModel.getUriToSaveImage())
    }

    fun selectContact(questionId: Int) {
        // TODO: unsupported for now
    }

    fun showDatePicker(activity: FragmentActivity, questionId: Int) {
        val date = viewModel.getCurrentDate(questionId = questionId)
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(date)
            .build()
        activity.let {
            picker.show(it.supportFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener {
                viewModel.onDatePicked(questionId, picker.headerText)
            }
        }
    }

    fun handleSurveyAction(activity: MainActivity, questionId: Int, actionType: SurveyActionType) {
        when (actionType) {
            SurveyActionType.PICK_DATE -> showDatePicker(activity, questionId)
            SurveyActionType.TAKE_PHOTO -> takeAPhoto()
            SurveyActionType.SELECT_CONTACT -> selectContact(questionId)
        }
    }

    val activity = LocalContext.current as MainActivity

    viewModel.uiState.observeAsState().value?.let { surveyState ->
        when (surveyState) {
            // The default implementation of questions makes it really hard
            // to abstract out the navigation as the destination are not physical
            // screens.
            is SurveyState.Questions -> SurveyQuestionsScreen(
                questions = surveyState,
                shouldAskPermissions = viewModel.askForPermissions,
                onAction = { id, action -> handleSurveyAction(activity, id, action) },
                onDoNotAskForPermissions = { viewModel.doNotAskForPermissions() },
                onDonePressed = { viewModel.computeResult(surveyState) },
                onBackPressed = {
                    controller.goBack()
                },
                openSettings = {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.packageName, null)
                        )
                    )
                }
            )
            is SurveyState.Result -> SurveyResultScreen(
                result = surveyState,
                onDonePressed = {
                    controller.goBack()
                }
            )
        }
    }
}