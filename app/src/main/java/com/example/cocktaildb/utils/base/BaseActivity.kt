package com.example.cocktaildb.utils.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<viewBinding : ViewBinding> : AppCompatActivity() {

    private lateinit var _viewBinding: viewBinding
    protected val viewBinding get() = _viewBinding

    abstract fun inflateViewBinding(): viewBinding

    abstract fun initData()

    abstract fun initView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewBinding = inflateViewBinding()
        setContentView(viewBinding.root)
        initView()
        initData()
    }
} 