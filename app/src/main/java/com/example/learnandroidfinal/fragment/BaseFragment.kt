package com.example.learnandroidfinal.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
    private val logTag by lazy { this::class.java.simpleName }

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        log("onAttach: context = $context")
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate: savedInstanceState = $savedInstanceState")
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        log("onViewCreated: view = $view, savedInstanceState = $savedInstanceState")
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        log("onStart")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        log("onResume")
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        log("onPause")
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        log("onStop")
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        log("onDestroyView")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
    }

    @CallSuper
    override fun onDetach() {
        super.onDetach()
        log("onDetach")
    }

    protected fun log(message: String) {
        Log.d(logTag, "$this: $message")
    }
}