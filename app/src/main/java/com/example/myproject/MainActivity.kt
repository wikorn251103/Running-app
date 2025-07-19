package com.example.myproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myproject.Fragment.ArticleFragment
import com.example.myproject.Fragment.HomeFragment
import com.example.myproject.Fragment.ProfileFragment
import com.example.myproject.Fragment.TrainingScheduleFragment
import com.example.myproject.Fragment.UploadFragment
import com.example.myproject.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = binding.bottomNavigation

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.bottom_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.bottom_calendar -> {
                    replaceFragment(TrainingScheduleFragment())
                    true
                }
                R.id.bottom_article -> {
                    replaceFragment(ArticleFragment())
                    true
                }
                R.id.bottom_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                R.id.bottom_upload -> {
                    replaceFragment(UploadFragment())
                    true
                }
                else -> false
            }
        }
        replaceFragment(HomeFragment())
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fram_container,fragment).commit()
    }
}