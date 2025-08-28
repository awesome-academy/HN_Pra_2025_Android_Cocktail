package com.example.cocktaildb.utils.pagination

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginationDataTest {

    @Test
    fun setData_resetsToFirstPage_andComputesTotalPages() {
        val p = PaginationData<Int>(pageSize = 5)
        p.setData((1..12).toList())

        assertEquals(1, p.getCurrentPage())
        assertEquals(3, p.totalPages)
        assertEquals(listOf(1,2,3,4,5), p.currentPageItems)
        assertTrue(p.hasNextPage)
        assertFalse(p.hasPreviousPage)
    }

    @Test
    fun nextAndPrevious_navigateWithinBounds() {
        val p = PaginationData<Int>(pageSize = 4)
        p.setData((1..10).toList())

        assertTrue(p.nextPage())
        assertEquals(2, p.getCurrentPage())
        assertEquals(listOf(5,6,7,8), p.currentPageItems)

        assertTrue(p.nextPage())
        assertEquals(3, p.getCurrentPage())
        assertEquals(listOf(9,10), p.currentPageItems)

        assertFalse(p.nextPage())

        assertTrue(p.previousPage())
        assertEquals(2, p.getCurrentPage())
        assertEquals(listOf(5,6,7,8), p.currentPageItems)

        assertTrue(p.goToPage(1))
        assertEquals(listOf(1,2,3,4), p.currentPageItems)
    }

    @Test
    fun emptyData_hasSinglePage_andNoItems() {
        val p = PaginationData<Int>(pageSize = 3)
        p.setData(emptyList())

        assertEquals(1, p.totalPages)
        assertEquals(0, p.getTotalItems())
        assertEquals(emptyList<Int>(), p.currentPageItems)
        assertFalse(p.hasNextPage)
        assertFalse(p.hasPreviousPage)
        assertFalse(p.goToPage(2))
    }
}
