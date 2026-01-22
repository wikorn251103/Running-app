package com.example.Fragment.loginandregister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myproject.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.myproject.MainActivity
import com.example.myproject.MainFragment
import com.example.myproject.R
import com.google.firebase.firestore.FirebaseFirestore

class SignInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // ดึง role จาก Firestore
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val role = document.getString("role") ?: "user"

                                if (role == "admin") {
                                    Toast.makeText(context, "เข้าสู่ระบบในโหมดผู้ดูแลระบบ", Toast.LENGTH_SHORT).show()
                                    // ✅ เปิดหน้า Admin Dashboard

                                } else {
                                    Toast.makeText(context, "เข้าสู่ระบบในโหมดผู้ใช้ทั่วไป", Toast.LENGTH_SHORT).show()
                                    (activity as? MainActivity)?.replaceFragment(MainFragment.newInstance())
                                }
                            } else {
                                Toast.makeText(context, "ไม่พบข้อมูลผู้ใช้ในระบบ", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "เกิดข้อผิดพลาดในการดึงข้อมูล: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "เข้าสู่ระบบไม่สำเร็จ: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // ปุ่มสมัครสมาชิก
        binding.registerTxt.setOnClickListener {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce)
            it.startAnimation(anim)

            it.postDelayed({
                (activity as? MainActivity)?.replaceFragment(SignUpFragment.newInstance())
            }, 100)
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