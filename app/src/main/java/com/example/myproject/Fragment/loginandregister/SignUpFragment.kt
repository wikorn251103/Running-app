package com.example.Fragment.loginandregister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // ฟัง state จาก ViewModel
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.signUpState.collectLatest { state ->
                when (state) {
                    is SignUpState.Idle -> {}
                    is SignUpState.Loading -> {
                        Toast.makeText(context, "กำลังสมัครสมาชิก...", Toast.LENGTH_SHORT).show()
                    }
                    is SignUpState.Success -> {
                        Toast.makeText(context, "สมัครสมาชิกสำเร็จ: ${state.user.name}", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.replaceFragment(SignInFragment.newInstance())
                    }
                    is SignUpState.Error -> {
                        Toast.makeText(context, "เกิดข้อผิดพลาด: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

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

            // ✅ ใช้ UserModel
            val user = UserModel(
                name = name,
                email = email,
                height = height,
                weight = weight,
                age = age,
                gender = gender,
                uid = "" // uid จะถูกเซ็ตตอน Firebase auth เสร็จ
            )

            viewModel.signUp(user, password)
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
