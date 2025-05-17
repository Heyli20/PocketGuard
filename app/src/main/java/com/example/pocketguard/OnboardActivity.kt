package com.example.pocketguard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboard)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = OnboardAdapter(this)
        viewPager.adapter = adapter
    }
}
