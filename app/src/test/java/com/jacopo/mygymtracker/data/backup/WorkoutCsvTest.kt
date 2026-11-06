package com.jacopo.mygymtracker.data.backup

import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class WorkoutCsvTest {

    @Test
    fun writesOneRowPerSet() {
        val workout = WorkoutSession(
            routineId = 1,
            routineName = "Push",
            startedAt = Instant.parse("2024-05-01T16:30:00Z"),
            exercises = listOf(
                WorkoutExercise(
                    exercise = newExercise("Squat"),
                    sets = listOf(
                        LoggedSet(reps = 5, weightKg = 40.0, type = SetType.WARM_UP),
                        LoggedSet(reps = 5, weightKg = 102.5, rpe = 8.5),
                    ),
                ),
                WorkoutExercise(newExercise("Plank"), listOf(LoggedSet(durationSeconds = 60))),
            ),
        )

        val csv = workoutsToCsv(listOf(workout), ZoneOffset.ofHours(2))

        assertEquals(
            "date,workout,exercise,set,type,weight_kg,reps,duration_s,rpe\r\n" +
                "2024-05-01 18:30,Push,Squat,1,WARM_UP,40.0,5,0,\r\n" +
                "2024-05-01 18:30,Push,Squat,2,NORMAL,102.5,5,0,8.5\r\n" +
                "2024-05-01 18:30,Push,Plank,1,NORMAL,0.0,0,60,\r\n",
            csv,
        )
    }

    @Test
    fun quotesFieldsWithCommasAndQuotes() {
        val workout = WorkoutSession(
            routineId = null,
            routineName = "Legs, \"heavy\"",
            startedAt = Instant.parse("2024-05-01T16:30:00Z"),
            exercises = listOf(WorkoutExercise(newExercise("Squat"), listOf(LoggedSet(reps = 5)))),
        )

        val row = workoutsToCsv(listOf(workout), ZoneOffset.UTC).split("\r\n")[1]

        assertEquals("2024-05-01 16:30,\"Legs, \"\"heavy\"\"\",Squat,1,NORMAL,0.0,5,0,", row)
    }
}
