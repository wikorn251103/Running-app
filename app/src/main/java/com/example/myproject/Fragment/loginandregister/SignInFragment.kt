package com.example.Fragment.loginandregister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.myproject.MainActivity
import com.example.myproject.MainFragment


class SignInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ปุ่มล็อกอิน
        binding.btnLogin.setOnClickListener {
            val email = binding.editTextText3.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกอีเมลและรหัสผ่าน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(context, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show()
                    // เปลี่ยน Fragment ไปหน้าโปรไฟล์
                    (activity as? MainActivity)?.replaceFragment(MainFragment.newInstance())
                }
                .addOnFailureListener {
                    Toast.makeText(context, "เข้าสู่ระบบไม่สำเร็จ: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // ปุ่มสมัครสมาชิก
        binding.registerTxt.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(SignUpFragment.newInstance())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SignInFragment()
    }
}
