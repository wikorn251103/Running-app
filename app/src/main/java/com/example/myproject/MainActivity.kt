package com.example.myproject

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myproject.Fragment.article.list.ArticlesFragment
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.Fragment.ProfileFragment
import com.example.myproject.Fragment.TrainingScheduleFragment
import com.example.myproject.databinding.ActivityMainBinding
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(MainFragment.newInstance())
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.containerMain.id, fragment)
            .commit()
    }
}