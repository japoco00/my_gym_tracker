package com.jacopo.mygymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SetTypeTest {

    @Test
    fun normalSetNumbers_countsOnlyNormalSets() {
        val types = listOf(SetType.WARM_UP, SetType.NORMAL, SetType.NORMAL, SetType.DROP, SetType.FAILURE, SetType.NORMAL)

        assertEquals(listOf(null, 1, 2, null, null, 3), types.normalSetNumbers())
    }

    @Test
    fun normalSetNumbers_ofNoSets_isEmpty() {
        assertEquals(emptyList<Int?>(), emptyList<SetType>().normalSetNumbers())
    }
}
