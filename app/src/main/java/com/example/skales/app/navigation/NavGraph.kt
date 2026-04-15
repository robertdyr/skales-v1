package com.example.skales.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.skales.player.PianoSoundPlayer
import com.example.skales.player.ScaleAutoPlayer
import com.example.skales.player.ScaleStepper
import com.example.skales.storage.ScaleRepository
import com.example.skales.app.screens.ScaleEditorScreen
import com.example.skales.app.screens.ScaleListScreen
import com.example.skales.app.screens.ScalePlayerScreen
import com.example.skales.app.viewmodel.ScaleListViewModel
import com.example.skales.app.viewmodel.ScaleEditorViewModel
import com.example.skales.app.viewmodel.ScalePlayerViewModel

object Routes {
    const val scaleList = "scales"
    const val scaleEditor = "editor"
    const val scalePlayer = "player"
    const val scaleIdArg = "scaleId"

    fun editorRoute(scaleId: String?): String {
        return if (scaleId.isNullOrBlank()) {
            scaleEditor
        } else {
            "$scaleEditor?$scaleIdArg=$scaleId"
        }
    }

    fun playerRoute(scaleId: String): String = "$scalePlayer/$scaleId"
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
                onOpenScale = { scaleId -> navController.navigate(Routes.playerRoute(scaleId)) },
                onEditScale = { scaleId -> navController.navigate(Routes.editorRoute(scaleId)) },
            )
        }

        composable(
            route = "${Routes.scalePlayer}/{${Routes.scaleIdArg}}",
            arguments = listOf(
                navArgument(Routes.scaleIdArg) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val scaleId = backStackEntry.arguments?.getString(Routes.scaleIdArg).orEmpty()
            val viewModel: ScalePlayerViewModel = viewModel(
                factory = ScalePlayerViewModel.factory(
                    scaleRepository = scaleRepository,
                    pianoSoundPlayer = pianoSoundPlayer,
                    scaleStepper = scaleStepper,
                    scaleAutoPlayer = scaleAutoPlayer,
                    scaleId = scaleId,
                ),
            )
            ScalePlayerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditScale = { editScaleId -> navController.navigate(Routes.editorRoute(editScaleId)) },
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
