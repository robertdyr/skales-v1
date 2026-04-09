package com.example.skales.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.skales.audio.PianoSoundPlayer
import com.example.skales.audio.ScaleAutoPlayer
import com.example.skales.audio.ScaleStepper
import com.example.skales.data.repository.ScaleRepository
import com.example.skales.ui.screens.ScaleEditorScreen
import com.example.skales.ui.screens.ScaleListScreen
import com.example.skales.viewmodel.ScaleEditorViewModel
import com.example.skales.viewmodel.ScaleListViewModel

object Routes {
    const val scaleList = "scales"
    const val scaleEditor = "editor"
    const val scaleIdArg = "scaleId"

    fun editorRoute(scaleId: String?): String {
        return if (scaleId.isNullOrBlank()) {
            scaleEditor
        } else {
            "$scaleEditor?$scaleIdArg=$scaleId"
        }
    }
}

@Composable
fun SkalesNavGraph(
    scaleRepository: ScaleRepository,
    pianoSoundPlayer: PianoSoundPlayer,
    scaleStepper: ScaleStepper,
    scaleAutoPlayer: ScaleAutoPlayer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.scaleList,
        modifier = modifier,
    ) {
        composable(route = Routes.scaleList) {
            val viewModel: ScaleListViewModel = viewModel(
                factory = ScaleListViewModel.factory(scaleRepository),
            )
            ScaleListScreen(
                viewModel = viewModel,
                onCreateScale = { navController.navigate(Routes.editorRoute(null)) },
                onEditScale = { scaleId -> navController.navigate(Routes.editorRoute(scaleId)) },
            )
        }

        composable(
            route = "${Routes.scaleEditor}?${Routes.scaleIdArg}={${Routes.scaleIdArg}}",
            arguments = listOf(
                navArgument(Routes.scaleIdArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val scaleId = backStackEntry.arguments?.getString(Routes.scaleIdArg)
            val viewModel: ScaleEditorViewModel = viewModel(
                factory = ScaleEditorViewModel.factory(
                    scaleRepository = scaleRepository,
                    pianoSoundPlayer = pianoSoundPlayer,
                    scaleStepper = scaleStepper,
                    scaleAutoPlayer = scaleAutoPlayer,
                    scaleId = scaleId,
                ),
            )
            ScaleEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
