package com.example.myproject

import android.view.View
import androidx.fragment.app.Fragment
import com.example.Fragment.profile.ProfileFragment
import com.example.myproject.Fragment.training.TrainingScheduleFragment
import com.example.myproject.Fragment.article.list.ArticlesFragment
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.corre.BaseFragment
import com.example.myproject.databinding.FragmentMainBinding

class MainFragment: BaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {
    override fun initViews() {
        replaceFragment(HomeFragment.newInstance())
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.bottom_home -> {
                    replaceFragment(HomeFragment.newInstance())
                    true
                }
                R.id.bottom_calendar -> {
                    replaceFragment(TrainingScheduleFragment())
                    true
                }
                R.id.bottom_article -> {
                    replaceFragment(ArticlesFragment())
                    true
                }
                R.id.bottom_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                R.id.bottom_upload -> {
                    replaceFragment(SaveDataFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }
    //method
    fun setBottomNavVisible(isVisible: Boolean) {
        binding.bottomNavigation.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.container.id, fragment)
            .addToBackStack(null)
            .commit()
    }
    companion object {
        fun newInstance() = MainFragment()
    }
}