package com.jacopo.mygymtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.viewModel.ExerciseUIState
import com.jacopo.mygymtracker.viewModel.SetUIState


@Composable
fun ExerciseCard(
    exercise: ExerciseUIState, addNewSet: () -> Unit
) {

    val sets = exercise.sets

    Card {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = exercise.name.value, onValueChange = { exercise.name.value = it },
                placeholder = { Text(text = "New Exercise")})
            SuggestionChip(
                onClick = { /*nothing to do*/ },
                label = { Text(text = exercise.category.value.toString()) })
            Row {
                Text(
                    text = "Reps", modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
                Text(
                    text = "Kg", modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
            }
            Column {
                sets.forEach { set ->
                    SetRow(
                        set, removeSet = {
                            sets.remove(set)
                        }
                    )

                }
            }
            IconButton(
                onClick = addNewSet
            ) {
                Icon(
                    imageVector = Icons.Default.Add, contentDescription = "Add"
                )
            }
        }
    }
}


@Composable
fun SetRow(set: SetUIState, removeSet: () -> Unit) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        ValueSetRow(
            incrementValue = 1, dataValue = set.reps, modifier = Modifier.weight(1f)
        )
        ValueSetRow(incrementValue = 5, dataValue = set.weight, modifier = Modifier.weight(1f))
        IconButton(
            onClick = removeSet
        ) {
            Icon(
                imageVector = Icons.Default.Delete, contentDescription = "Delete"
            )
        }
    }
}

@Composable
fun ValueSetRow(
    dataValue: MutableState<Int>,
    incrementValue: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier, verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            shape = Shapes().small,
            onClick = {
                if (dataValue.value > 0) {
                    dataValue.value -= incrementValue
                }
            },
            modifier = Modifier
                .wrapContentSize()
                .padding(2.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "-${incrementValue}")
        }
        TextField(
            value = dataValue.value.toString(),
            onValueChange = {
                if (it.isEmpty()) {
                    dataValue.value = 0
                } else {
                    dataValue.value = it.toInt()
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = modifier
        )
        Button(
            shape = Shapes().small,
            onClick = { dataValue.value += incrementValue },
            modifier = Modifier
                .wrapContentSize()
                .padding(2.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "+$incrementValue")
        }
    }
}

@Preview
@Composable
fun ExerciseCardPreview() {
//    val exercise = ExerciseUIState(
//        sets = remember {
//            mutableStateListOf(
//                SetUIState(mutableIntStateOf(0), mutableIntStateOf(0)),
//                SetUIState(mutableIntStateOf(0), mutableIntStateOf(0)),
//                SetUIState(mutableIntStateOf(0), mutableIntStateOf(0))
//            )
//        },
//        name = remember { mutableStateOf("Bench Press") },
//        category = remember { mutableStateOf(Category.CAT1) }
//    )
//    ExerciseCard(exercise = exercise)
}