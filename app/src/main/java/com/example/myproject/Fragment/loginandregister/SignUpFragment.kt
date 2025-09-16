package com.example.Fragment.loginandregister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.button.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val email = binding.editTextText5.text.toString().trim()
            val password = binding.editTextText6.text.toString().trim()
            val height = binding.editTextText10.text.toString().toIntOrNull()
            val weight = binding.editTextText11.text.toString().toDoubleOrNull()
            val age = binding.editTextText7.text.toString().toIntOrNull()
            val gender = when (binding.radioGroupGender.checkedRadioButtonId) {
                R.id.radioMale -> "ชาย"
                R.id.radioFemale -> "หญิง"
                else -> ""
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
                height == null || weight == null || age == null || gender.isEmpty()
            ) {
                Toast.makeText(context, "กรอกข้อมูลให้ครบทุกช่อง", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    val userData = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "height" to height,
                        "weight" to weight,
                        "age" to age,
                        "gender" to gender
                    )

                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "สมัครสมาชิกสำเร็จ", Toast.LENGTH_SHORT).show()
                            // เปลี่ยนไปหน้า LoginFragment (แก้ตามที่คุณต้องการ)
                            (activity as? MainActivity)?.replaceFragment(SignInFragment.newInstance())
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึกข้อมูล", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "สมัครไม่สำเร็จ: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.logintxt.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(SignInFragment.newInstance())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SignUpFragment()
    }
}
