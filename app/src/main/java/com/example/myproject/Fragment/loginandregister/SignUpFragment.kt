package com.example.Fragment.loginandregister

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myproject.Fragment.loginandregister.SignUpState
import com.example.myproject.Fragment.loginandregister.SignUpViewModel
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.data.signup.UserModel
import com.example.myproject.databinding.FragmentSignUpBinding
import kotlinx.coroutines.flow.collectLatest

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Real-time ตรวจสอบรหัสผ่านขณะพิมพ์
        binding.editTextText6.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePasswordConditions(s.toString())
            }
        })

        // ✅ ฟัง state จาก ViewModel
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.signUpState.collectLatest { state ->
                when (state) {
                    is SignUpState.Idle -> {}

                    is SignUpState.Loading -> {
                        binding.button.isEnabled = false
                        binding.button.text = "กำลังสมัครสมาชิก..."
                    }

                    is SignUpState.Success -> {
                        binding.button.isEnabled = true
                        binding.button.text = "สมัครสมาชิก"
                        Toast.makeText(
                            context,
                            "สมัครสมาชิกสำเร็จ!\nกรุณายืนยันอีเมล ${state.user.email} ก่อนเข้าสู่ระบบ",
                            Toast.LENGTH_LONG
                        ).show()
                        (activity as? MainActivity)?.replaceFragment(SignInFragment.newInstance())
                    }

                    is SignUpState.Error -> {
                        binding.button.isEnabled = true
                        binding.button.text = "สมัครสมาชิก"
                        Toast.makeText(
                            context,
                            "เกิดข้อผิดพลาด: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // ✅ ปุ่มสมัครสมาชิก
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

            if (!validateInput(name, email, password, height, weight, age, gender)) return@setOnClickListener

            val user = UserModel(
                name = name,
                email = email,
                height = height!!,
                weight = weight!!,
                age = age!!,
                gender = gender,
                uid = ""
            )

            viewModel.signUp(user, password)
        }

        binding.logintxt.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(SignInFragment.newInstance())
        }
    }

    // ✅ อัปเดตสีเงื่อนไขรหัสผ่านแบบ real-time
    private fun updatePasswordConditions(password: String) {
        val hasLength = password.length >= 8
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        setCondition(binding.conditionLength, hasLength, "อย่างน้อย 8 ตัวอักษร")
        setCondition(binding.conditionUpper, hasUpper, "ตัวพิมพ์ใหญ่ (A-Z)")
        setCondition(binding.conditionLower, hasLower, "ตัวพิมพ์เล็ก (a-z)")
        setCondition(binding.conditionDigit, hasDigit, "ตัวเลข (0-9)")
    }

    private fun setCondition(textView: TextView, isPassed: Boolean, label: String) {
        if (isPassed) {
            textView.text = "✓  $label"
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            textView.text = "✗  $label"
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grayText))
        }
    }

    // ✅ Validate ทุกช่อง
    private fun validateInput(
        name: String,
        email: String,
        password: String,
        height: Int?,
        weight: Double?,
        age: Int?,
        gender: String
    ): Boolean {
        if (name.isEmpty()) {
            showError("กรุณากรอกชื่อ-นามสกุล")
            return false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("กรุณากรอกอีเมลให้ถูกต้อง")
            return false
        }
        if (!isPasswordValid(password)) {
            showError("รหัสผ่านต้องมีอย่างน้อย 8 ตัว มีตัวพิมพ์ใหญ่ พิมพ์เล็ก และตัวเลข")
            return false
        }
        if (height == null || height <= 0 || height > 300) {
            showError("กรุณากรอกส่วนสูงให้ถูกต้อง")
            return false
        }
        if (weight == null || weight <= 0 || weight > 500) {
            showError("กรุณากรอกน้ำหนักให้ถูกต้อง")
            return false
        }
        if (age == null || age <= 0 || age > 120) {
            showError("กรุณากรอกอายุให้ถูกต้อง")
            return false
        }
        if (gender.isEmpty()) {
            showError("กรุณาเลือกเพศ")
            return false
        }
        return true
    }

    // ✅ ตรวจสอบเงื่อนไขรหัสผ่านครบทุกข้อ
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SignUpFragment()
    }
}