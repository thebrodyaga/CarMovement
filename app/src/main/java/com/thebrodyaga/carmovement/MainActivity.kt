package com.thebrodyaga.carmovement

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpDelegate

class MainActivity : MvpAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(AnimatedPathView(this).apply { init(mvpDelegate) })
    }
}
