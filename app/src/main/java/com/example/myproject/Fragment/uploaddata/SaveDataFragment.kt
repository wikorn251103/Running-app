import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.myproject.R
import com.example.myproject.databinding.FragmentSaveDataBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.*

class SaveDataFragment : Fragment() {

    private var _binding: FragmentSaveDataBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    /*private val storage = FirebaseStorage.getInstance()*/
    private var imageUri: Uri? = null
    private var capturedImageFile: File? = null

    // Modern Activity Result APIs
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            updateImageView()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            capturedImageFile?.let {
                imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    it
                )
                updateImageView()
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "ต้องการสิทธิ์ในการใช้กล้องเพื่อถ่ายรูป",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaveDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        setupValidation()
    }

    private fun setupUI() {
        // เปิดใช้งานปุ่มบันทึกเมื่อข้อมูลครบถ้วน
        updateSaveButtonState()
    }

    private fun setupListeners() {
        binding.saveBtn.setOnClickListener {
            if (validateInputs()) {
                saveRunningDataToFirestore()
            }
        }

        binding.resetBtn.setOnClickListener {
            showClearConfirmationDialog()
        }

        // อัปเดต binding สำหรับการคลิกอัปโหลดรูป
        binding.layoutPhotoUpload.setOnClickListener {
            showImageSourceDialog()
        }

        // เพิ่ม listener สำหรับการคำนวณ pace อัตโนมัติ
        setupPaceCalculation()
    }

    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSaveButtonState()
            }
        }

        binding.editDistance.addTextChangedListener(textWatcher)
        binding.etHour.addTextChangedListener(textWatcher)
        binding.etMinute.addTextChangedListener(textWatcher)
        binding.etSecond.addTextChangedListener(textWatcher)

        // Chip selection listener
        binding.chipGroup.setOnCheckedStateChangeListener { _, _ ->
            updateSaveButtonState()
        }
    }

    private fun setupPaceCalculation() {
        val paceCalculator = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateAndDisplayPace()
            }
        }

        binding.editDistance.addTextChangedListener(paceCalculator)
        binding.etHour.addTextChangedListener(paceCalculator)
        binding.etMinute.addTextChangedListener(paceCalculator)
        binding.etSecond.addTextChangedListener(paceCalculator)
    }

    private fun calculateAndDisplayPace() {
        val distance = binding.editDistance.text.toString().toDoubleOrNull() ?: 0.0
        val hours = binding.etHour.text.toString().toIntOrNull() ?: 0
        val minutes = binding.etMinute.text.toString().toIntOrNull() ?: 0
        val seconds = binding.etSecond.text.toString().toIntOrNull() ?: 0

        if (distance > 0 && (hours > 0 || minutes > 0 || seconds > 0)) {
            val totalSeconds = hours * 3600 + minutes * 60 + seconds
            val paceInSeconds = totalSeconds / distance
            val paceMinutes = (paceInSeconds / 60).toInt()
            val paceSecondsRemainder = (paceInSeconds % 60).toInt()

            binding.tvCalculatedPace.text = String.format(
                "%d:%02d นาที/กม.",
                paceMinutes,
                paceSecondsRemainder
            )
        } else {
            binding.tvCalculatedPace.text = "--:-- นาที/กม."
        }
    }

    private fun updateSaveButtonState() {
        val hasDistance = binding.editDistance.text.toString().toDoubleOrNull()?.let { it > 0 } ?: false
        val hasTime = (binding.etHour.text.toString().toIntOrNull() ?: 0) > 0 ||
                (binding.etMinute.text.toString().toIntOrNull() ?: 0) > 0 ||
                (binding.etSecond.text.toString().toIntOrNull() ?: 0) > 0
        val hasTrainingType = binding.chipGroup.checkedChipId != -1

        val isFormValid = hasDistance && hasTime && hasTrainingType
        binding.saveBtn.isEnabled = isFormValid
        binding.saveBtn.alpha = if (isFormValid) 1.0f else 0.6f
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("เลือกจากแกลเลอรี", "ถ่ายรูปด้วยกล้อง")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("เลือกแหล่งที่มาของภาพ")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openGallery()
                1 -> checkCameraPermissionAndOpen()
            }
        }
        builder.show()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            capturedImageFile = File(
                requireContext().getExternalFilesDir(null),
                "temp_photo_${System.currentTimeMillis()}.jpg"
            )

            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                capturedImageFile!!
            )

            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "เกิดข้อผิดพลาดในการเปิดกล้อง: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateImageView() {
        binding.ivCamera.setImageURI(imageUri)
        binding.ivCamera.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
    }

    private fun validateInputs(): Boolean {
        val distance = binding.editDistance.text.toString().toDoubleOrNull()
        val hours = binding.etHour.text.toString().toIntOrNull() ?: 0
        val minutes = binding.etMinute.text.toString().toIntOrNull() ?: 0
        val seconds = binding.etSecond.text.toString().toIntOrNull() ?: 0

        when {
            distance == null || distance <= 0 -> {
                binding.editDistance.error = "กรุณาระบุระยะทางที่ถูกต้อง"
                return false
            }
            distance > 200 -> {
                binding.editDistance.error = "ระยะทางต้องไม่เกิน 200 กิโลเมตร"
                return false
            }
            hours == 0 && minutes == 0 && seconds == 0 -> {
                binding.etHour.error = "กรุณาระบุเวลา"
                return false
            }
            hours > 23 -> {
                binding.etHour.error = "ชั่วโมงต้องไม่เกิน 23"
                return false
            }
            minutes > 59 -> {
                binding.etMinute.error = "นาทีต้องไม่เกิน 59"
                return false
            }
            seconds > 59 -> {
                binding.etSecond.error = "วินาทีต้องไม่เกิน 59"
                return false
            }
            binding.chipGroup.checkedChipId == -1 -> {
                Snackbar.make(binding.root, "กรุณาเลือกประเภทการซ้อม", Snackbar.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun saveRunningDataToFirestore() {
        // แสดง loading
        binding.saveBtn.isEnabled = false
        binding.saveBtn.text = "กำลังบันทึก..."

        /*lifecycleScope.launch {
            try {
                val data = prepareDataForSave()

                // อัปโหลดรูปภาพก่อน (ถ้ามี)
                val imageUrl = imageUri?.let { uploadImageToFirebase(it) }
                if (imageUrl != null) {
                    data["imageUrl"] = imageUrl
                }

                // บันทึกข้อมูลลง Firestore
                db.collection("running_logs")
                    .add(data)
                    .await()

                Toast.makeText(requireContext(), "บันทึกสำเร็จ! 🎉", Toast.LENGTH_SHORT).show()
                clearForm()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.saveBtn.isEnabled = true
                binding.saveBtn.text = "บันทึกการซ้อม"
            }
        }*/
    }

    private fun prepareDataForSave(): HashMap<String, Any> {
        val distance = binding.editDistance.text.toString().toDoubleOrNull() ?: 0.0
        val hours = binding.etHour.text.toString().toIntOrNull() ?: 0
        val minutes = binding.etMinute.text.toString().toIntOrNull() ?: 0
        val seconds = binding.etSecond.text.toString().toIntOrNull() ?: 0

        val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        val totalTimeInSeconds = hours * 3600 + minutes * 60 + seconds
        val noteText = binding.etNote.text.toString().trim()

        val selectedChipId = binding.chipGroup.checkedChipId
        val trainingType = if (selectedChipId != -1) {
            binding.chipGroup.findViewById<Chip>(selectedChipId).text.toString()
        } else {
            "ไม่ระบุ"
        }

        // คำนวณ pace
        val paceInSeconds = if (distance > 0) totalTimeInSeconds / distance else 0.0
        val paceFormatted = if (paceInSeconds > 0) {
            val paceMinutes = (paceInSeconds / 60).toInt()
            val paceSecondsRemainder = (paceInSeconds % 60).toInt()
            String.format("%d:%02d", paceMinutes, paceSecondsRemainder)
        } else "0:00"

        return hashMapOf(
            "distance" to distance,
            "time" to timeFormatted,
            "totalTimeInSeconds" to totalTimeInSeconds,
            "pace" to paceFormatted,
            "paceInSeconds" to paceInSeconds,
            "trainingType" to trainingType,
            "note" to noteText,
            "timestamp" to Timestamp.now(),
            "deviceInfo" to android.os.Build.MODEL
        )
    }

    /*อัพโหลดลง Storage*/
    /*private suspend fun uploadImageToFirebase(imageUri: Uri): String {
        return try {
            val fileName = "running_images/${System.currentTimeMillis()}.jpg"
            val imageRef = storage.reference.child(fileName)

            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("อัปโหลดรูปภาพไม่สำเร็จ: ${e.message}")
        }
    }*/

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ยืนยันการล้างข้อมูล")
            .setMessage("คุณต้องการล้างข้อมูลทั้งหมดหรือไม่?")
            .setPositiveButton("ใช่") { _, _ ->
                clearForm()
                Toast.makeText(requireContext(), "ล้างข้อมูลเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("ไม่", null)
            .show()
    }

    private fun clearForm() {
        binding.editDistance.setText("")
        binding.etHour.setText("")
        binding.etMinute.setText("")
        binding.etSecond.setText("")
        binding.etNote.setText("")
        binding.chipGroup.clearCheck()
        binding.ivCamera.setImageResource(R.drawable.photo_camera_24px)
        binding.ivCamera.scaleType = android.widget.ImageView.ScaleType.CENTER
        binding.tvCalculatedPace.text = "--:-- นาที/กม."

        // ล้าง errors
        binding.editDistance.error = null
        binding.etHour.error = null
        binding.etMinute.error = null
        binding.etSecond.error = null

        imageUri = null
        capturedImageFile = null
        updateSaveButtonState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SaveDataFragment {
            return SaveDataFragment()
        }
    }
}