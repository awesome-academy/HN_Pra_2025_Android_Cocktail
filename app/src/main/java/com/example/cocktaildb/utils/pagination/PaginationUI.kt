package com.example.cocktaildb.utils.pagination

import android.content.Context
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.cocktaildb.R

class PaginationUI(private val context: Context) {
    
    fun createPageButtons(
        container: LinearLayout,
        currentPage: Int,
        totalPages: Int,
        onPageClick: (Int) -> Unit
    ) {
        container.removeAllViews()
        
        if (totalPages <= 1) return
        
        val maxVisiblePages = 5
        val startPage: Int
        val endPage: Int
        
        when {
            totalPages <= maxVisiblePages -> {
                // Ít hơn hoặc bằng 5 trang: hiển thị tất cả
                startPage = 1
                endPage = totalPages
            }
            currentPage <= 3 -> {
                // Ở gần đầu: hiển thị 1 2 3 4 5 ...
                startPage = 1
                endPage = maxVisiblePages
            }
            currentPage >= totalPages - 2 -> {
                // Ở gần cuối: hiển thị ... (totalPages-4) (totalPages-3) (totalPages-2) (totalPages-1) totalPages
                startPage = totalPages - maxVisiblePages + 1
                endPage = totalPages
            }
            else -> {
                // Ở giữa: hiển thị ... (currentPage-2) (currentPage-1) currentPage (currentPage+1) (currentPage+2) ...
                startPage = currentPage - 2
                endPage = currentPage + 2
            }
        }

        // Thêm ellipsis đầu nếu cần
        if (startPage > 1) {
            addDots(container)
        }

        // Thêm các trang
        for (page in startPage..endPage) {
            addPageButton(container, page, currentPage, onPageClick)
        }

        // Thêm ellipsis cuối nếu cần
        if (endPage < totalPages) {
            addDots(container)
        }
    }
    
    private fun addPageButton(
        container: LinearLayout,
        page: Int,
        currentPage: Int,
        onPageClick: (Int) -> Unit
    ) {
        val buttonSize = context.resources.getDimensionPixelSize(R.dimen.dp_36)
        
        val button = TextView(context).apply {
            text = page.toString()
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            width = buttonSize
            height = buttonSize
            
            if (page == currentPage) {
                background = ContextCompat.getDrawable(context, R.drawable.current_page_bg)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                typeface = Typeface.DEFAULT_BOLD
            } else {
                background = ContextCompat.getDrawable(context, R.drawable.page_button_bg)
                setTextColor(ContextCompat.getColor(context, R.color.black))
                typeface = Typeface.DEFAULT
            }
            
            setOnClickListener { onPageClick(page) }
        }
        
        val params = LinearLayout.LayoutParams(
            buttonSize,
            buttonSize
        ).apply {
            marginEnd = context.resources.getDimensionPixelSize(R.dimen.dp_4)
            marginStart = context.resources.getDimensionPixelSize(R.dimen.dp_4)
        }
        
        container.addView(button, params)
    }
    
    private fun addDots(container: LinearLayout) {
        val buttonSize = context.resources.getDimensionPixelSize(R.dimen.dp_36)
        
        val dots = TextView(context).apply {
            text = "..."
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            width = buttonSize
            height = buttonSize
            setTextColor(ContextCompat.getColor(context, R.color.black))
        }
        
        val params = LinearLayout.LayoutParams(
            buttonSize,
            buttonSize
        ).apply {
            marginEnd = context.resources.getDimensionPixelSize(R.dimen.dp_4)
            marginStart = context.resources.getDimensionPixelSize(R.dimen.dp_4)
        }
        
        container.addView(dots, params)
    }
}

