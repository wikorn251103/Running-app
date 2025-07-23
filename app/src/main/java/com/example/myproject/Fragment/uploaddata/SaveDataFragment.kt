import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myproject.R
import com.example.myproject.databinding.FragmentSaveDataBinding
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class SaveDataFragment : Fragment() {

    private lateinit var binding: FragmentSaveDataBinding
    private val db = FirebaseFirestore.getInstance()
    private val IMAGE_PICK_CODE = 1000
    private val CAMERA_REQUEST_CODE = 1001
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSaveDataBinding.inflate(inflater, container, false)
        setupListeners()
        return binding.root
    }

    private fun setupListeners() {
        binding.saveBtn.setOnClickListener {
            saveRunningDataToFirestore()
        }

        binding.resetBtn.setOnClickListener {
            clearForm()
        }

        // เพิ่มการคลิกเพื่ออัปโหลดภาพ
        binding.imageView7.setOnClickListener {
            showImageSourceDialog()
        }
    }

    // เปิดแกลเลอรีหรือกล้อง
    private fun showImageSourceDialog() {
        val options = arrayOf("เลือกจากแกลเลอรี", "ถ่ายรูปด้วยกล้อง")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("เลือกแหล่งที่มาของภาพ")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openGallery() // เลือกแกลเลอรี
                1 -> openCamera() // ถ่ายรูป
            }
        }
        builder.show()
    }

    // เปิดแกลเลอรี
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // เปิดกล้อง
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    // รับผลลัพธ์จากแกลเลอรีหรือกล้อง
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    imageUri = data?.data
                    binding.imageView7.setImageURI(imageUri)  // อัพเดท ImageView
                }
                CAMERA_REQUEST_CODE -> {
                    imageUri = data?.extras?.get("data") as Uri
                    binding.imageView7.setImageURI(imageUri)  // อัพเดท ImageView
                }
            }
        }
    }

    private fun saveRunningDataToFirestore() {
        val distance = binding.editDistance.text.toString().toDoubleOrNull() ?: 0.0
        val h = binding.etHour.text.toString().toIntOrNull() ?: 0
        val m = binding.etMinute.text.toString().toIntOrNull() ?: 0
        val s = binding.etSecond.text.toString().toIntOrNull() ?: 0
        val timeFormatted = String.format("%02d:%02d:%02d", h, m, s)
        val noteText = binding.etNote.text.toString()

        val selectedChipId = binding.chipGroup.checkedChipId
        val trainingType = if (selectedChipId != -1) {
            binding.chipGroup.findViewById<Chip>(selectedChipId).text.toString()
        } else {
            "ไม่ระบุ"
        }

        val data = hashMapOf(
            "distance" to distance,
            "time" to timeFormatted,
            "trainingType" to trainingType,
            "note" to noteText,
            "timestamp" to Timestamp.now()
        )

        db.collection("running_logs")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearForm() {
        binding.editDistance.setText("")
        binding.etHour.setText("")
        binding.etMinute.setText("")
        binding.etSecond.setText("")
        binding.etNote.setText("")
        binding.chipGroup.clearCheck()
        binding.imageView7.setImageResource(R.drawable.photo_camera_24px)
    }

    companion object {
        fun newInstance(): SaveDataFragment {
            return SaveDataFragment()
        }
    }
}
