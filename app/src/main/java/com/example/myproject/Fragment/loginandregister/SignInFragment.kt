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

        // ✅ ปุ่มเข้าสู่ระบบ
        binding.btnLogin.setOnClickListener {
            val email = binding.editTextText3.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกอีเมลและรหัสผ่าน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ล็อกปุ่มไม่ให้กดซ้ำระหว่าง loading
            binding.btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user

                    if (firebaseUser == null) {
                        Toast.makeText(context, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
                        binding.btnLogin.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // ✅ บังคับยืนยันอีเมลก่อนเข้าระบบ
                    if (!firebaseUser.isEmailVerified) {
                        Toast.makeText(
                            context,
                            "กรุณายืนยันอีเมล ${firebaseUser.email} ก่อนเข้าสู่ระบบ",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        binding.btnLogin.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // ✅ ดึง role จาก Firestore
                    db.collection("users").document(firebaseUser.uid).get()
                        .addOnSuccessListener { document ->
                            binding.btnLogin.isEnabled = true

                            if (!document.exists()) {
                                Toast.makeText(context, "ไม่พบข้อมูลผู้ใช้ในระบบ", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                                return@addOnSuccessListener
                            }

                            val role = document.getString("role") ?: "user"

                            if (role == "admin") {
                                Toast.makeText(context, "เข้าสู่ระบบในโหมดผู้ดูแลระบบ", Toast.LENGTH_SHORT).show()
                                // TODO: เปิดหน้า Admin Dashboard
                            } else {
                                Toast.makeText(context, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show()
                                (activity as? MainActivity)?.replaceFragment(MainFragment.newInstance())
                            }
                        }
                        .addOnFailureListener { e ->
                            binding.btnLogin.isEnabled = true
                            auth.signOut()
                            Toast.makeText(
                                context,
                                "เกิดข้อผิดพลาดในการดึงข้อมูล: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(
                        context,
                        "เข้าสู่ระบบไม่สำเร็จ: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ✅ ปุ่มส่งอีเมลยืนยันใหม่
        binding.resendVerificationTxt.setOnClickListener {
            val email = binding.editTextText3.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรอกอีเมลและรหัสผ่านก่อนกดส่งอีเมลใหม่", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.resendVerificationTxt.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user

                    if (firebaseUser == null) {
                        binding.resendVerificationTxt.isEnabled = true
                        return@addOnSuccessListener
                    }

                    if (firebaseUser.isEmailVerified) {
                        Toast.makeText(
                            context,
                            "อีเมลนี้ยืนยันแล้ว กรุณาเข้าสู่ระบบได้เลย",
                            Toast.LENGTH_SHORT
                        ).show()
                        auth.signOut()
                        binding.resendVerificationTxt.isEnabled = true
                        return@addOnSuccessListener
                    }

                    firebaseUser.sendEmailVerification()
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "ส่งอีเมลยืนยันใหม่แล้ว กรุณาตรวจสอบกล่องจดหมาย",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                            binding.resendVerificationTxt.isEnabled = true
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "ส่งอีเมลไม่สำเร็จ: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            auth.signOut()
                            binding.resendVerificationTxt.isEnabled = true
                        }
                }
                .addOnFailureListener { e ->
                    binding.resendVerificationTxt.isEnabled = true
                    Toast.makeText(
                        context,
                        "ข้อมูลไม่ถูกต้อง: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ✅ ปุ่มสมัครสมาชิก
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