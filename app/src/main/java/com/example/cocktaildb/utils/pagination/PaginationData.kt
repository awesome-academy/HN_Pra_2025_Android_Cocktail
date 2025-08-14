package com.example.cocktaildb.utils.pagination

class PaginationData<T>(
    private val pageSize: Int = 10
) {
    private var allItems: List<T> = emptyList()
    private var currentPage: Int = 1
    
    val totalPages: Int
        get() = if (allItems.isEmpty()) 1 else ((allItems.size + pageSize - 1) / pageSize)
    
    val currentPageItems: List<T>
        get() {
            if (allItems.isEmpty()) return emptyList()
            
            val startIndex = (currentPage - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, allItems.size)
            
            return if (startIndex < allItems.size) {
                allItems.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        }
    
    val hasNextPage: Boolean
        get() = currentPage < totalPages
    
    val hasPreviousPage: Boolean
        get() = currentPage > 1
    
    fun setData(items: List<T>) {
        allItems = items
        currentPage = 1
    }
    
    fun nextPage(): Boolean {
        return if (hasNextPage) {
            currentPage++
            true
        } else {
            false
        }
    }
    
    fun previousPage(): Boolean {
        return if (hasPreviousPage) {
            currentPage--
            true
        } else {
            false
        }
    }
    
    fun goToPage(page: Int): Boolean {
        return if (page in 1..totalPages) {
            currentPage = page
            true
        } else {
            false
        }
    }
    
    fun getCurrentPage(): Int = currentPage
    
    fun getTotalItems(): Int = allItems.size

}

