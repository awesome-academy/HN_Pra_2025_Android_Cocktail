package com.example.cocktaildb.utils.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.viewbinding.ViewBinding

abstract class BaseDialog<VB : ViewBinding>(context: Context) : Dialog(context) {

    protected lateinit var viewBinding: VB

    abstract fun inflateViewBinding(): VB

    abstract fun initView()

    abstract fun initData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewBinding = inflateViewBinding()
        setContentView(viewBinding.root)
        
        initView()
        initData()
    }
}

