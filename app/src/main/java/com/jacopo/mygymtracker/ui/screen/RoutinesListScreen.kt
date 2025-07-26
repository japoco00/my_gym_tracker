package com.jacopo.mygymtracker.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import com.jacopo.mygymtracker.ui.components.RoutineOverviewCard
import com.jacopo.mygymtracker.viewModel.RoutinesListVM

@Composable
fun RoutinesListScreen(
    onNavigateToDetails: (Int) -> Unit = { /* No-op */ },
    vm: RoutinesListVM
) {

//    val factory = remember {
//        RoutinesListVMFactory(
//            routineRepository = appContainer.routineRepository,
//        )
//    }
//    val vm: RoutinesListVM = viewModel(factory = factory)
    val routines by vm.routines.collectAsState()
    val coroutineScope = rememberCoroutineScope()


    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn {
            items(routines) { routine ->
                RoutineOverviewCard(
                    routine,
                    onClick = { onNavigateToDetails(routine.id!!) },
                    onDelete = { vm.deleteRoutine(routine) })
            }
        }
        Button(onClick = { onNavigateToDetails(-1) })
        {
            Text(text = "Create a Routine")
        }
    }
}

