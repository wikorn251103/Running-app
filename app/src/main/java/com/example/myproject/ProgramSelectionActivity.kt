package com.example.myproject

import android.os.Bundle
import android.text.TextUtils.replace
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.example.myproject.databinding.ActivityProgramSelectionBinding

class ProgramSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgramSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ผูก layout กับ binding
        binding = ActivityProgramSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // แสดง TargetDistanceFragment เป็นหน้าแรก
        if (savedInstanceState == null) {
            replaceFragment(TargetDistanceFragment.newInstance())
        }
    }

    /**
     * ฟังก์ชันสำหรับเปลี่ยน Fragment
     */
    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(binding.containerProgram.id, fragment)
        }
    }

    /**
     * ฟังก์ชันสำหรับเปิด Fragment แบบซ้อน (มี Back Stack)
     */
    fun addFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(binding.containerProgram.id, fragment)
            addToBackStack(null)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}