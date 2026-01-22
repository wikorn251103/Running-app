package com.example.myproject.Fragment.drill.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.example.myproject.corre.BaseFragment
import com.example.myproject.data.drill.drillModel
import com.example.myproject.databinding.FragmentDrillDetailBinding

class DrillDetailFragment :
    BaseFragment<FragmentDrillDetailBinding>(FragmentDrillDetailBinding::inflate) {

    private lateinit var drill: drillModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            drill = it.getParcelable("drill")!!
        }
    }

    override fun initViews() {
        // ตั้งค่าข้อมูล
        binding.tvTitle.text = drill.title
        binding.tvDescription.text = drill.description
        binding.tvTimeRange.text = drill.timeRange
        binding.tvSetRange.text = drill.setRange
        binding.tvSteps.text = drill.step
        binding.tvFocus.text = drill.focus
        binding.tvSubtitle.text = drill.subtitle

        // เมื่อคลิกที่วิดีโอ ให้เปิด YouTube
        binding.videoThumbnailContainer.setOnClickListener {
            openYoutubeVideo(drill.videoUrl)
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openYoutubeVideo(url: String) {
        try {
            val videoId = extractYoutubeVideoId(url)
            val isShorts = url.contains("/shorts/")

            // สร้าง URL ที่ถูกต้องตามประเภทวิดีโอ
            val youtubeUrl = if (isShorts) {
                "https://www.youtube.com/shorts/$videoId"
            } else {
                "https://www.youtube.com/watch?v=$videoId"
            }

            // พยายามเปิดใน YouTube App ก่อน
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                startActivity(appIntent)
            } catch (e: Exception) {
                // ถ้าไม่มี YouTube App ให้เปิดในเบราว์เซอร์
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "ไม่สามารถเปิดวิดีโอได้", Toast.LENGTH_SHORT).show()
        }
    }

    // รองรับ youtube.com, youtu.be และ YouTube Shorts
    private fun extractYoutubeVideoId(url: String): String {
        val uri = Uri.parse(url)
        return when {
            // YouTube Shorts: https://youtube.com/shorts/VIDEO_ID
            uri.path?.contains("/shorts/") == true -> {
                uri.pathSegments.lastOrNull() ?: ""
            }
            // Short URL: https://youtu.be/VIDEO_ID
            uri.host == "youtu.be" -> {
                uri.lastPathSegment ?: ""
            }
            // Normal URL: https://www.youtube.com/watch?v=VIDEO_ID
            uri.host?.contains("youtube.com") == true -> {
                uri.getQueryParameter("v") ?: ""
            }
            else -> ""
        }
    }

    companion object {
        fun newInstance(drill: drillModel): DrillDetailFragment {
            val fragment = DrillDetailFragment()
            val args = Bundle()
            args.putParcelable("drill", drill)
            fragment.arguments = args
            return fragment
        }
    }
}