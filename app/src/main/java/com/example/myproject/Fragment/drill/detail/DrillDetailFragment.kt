package com.example.myproject.Fragment.drill.detail

import android.net.Uri
import android.os.Bundle
import com.example.myproject.corre.BaseFragment
import com.example.myproject.data.drill.drillModel
import com.example.myproject.databinding.FragmentDrillDetailBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener

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
        // ตั้งค่า YouTube Player
        val videoId = extractYoutubeVideoId(drill.videoUrl)

        lifecycle.addObserver(binding.youtubePlayerView)

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                // ใช้ cueVideo เพื่อไม่ให้เล่นอัตโนมัติ
                youTubePlayer.cueVideo(videoId, 0f)
            }
        })
        // ตั้งค่าข้อมูลอื่น ๆ
        binding.tvTitle.text = drill.title
        binding.tvDescription.text = drill.description
        binding.tvTimeRange.text = drill.timeRange
        binding.tvSetRange.text = drill.setRange
        binding.tvSteps.text = drill.step
        binding.tvFocus.text = drill.focus
        binding.tvSubtitle.text = drill.subtitle

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        binding.youtubePlayerView.release()
        super.onDestroyView()
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

    // รองรับทั้ง youtube.com และ youtu.be
    private fun extractYoutubeVideoId(url: String): String {
        val uri = Uri.parse(url)
        return when {
            uri.host == "youtu.be" -> uri.lastPathSegment ?: ""
            uri.host?.contains("youtube.com") == true -> uri.getQueryParameter("v") ?: ""
            else -> ""
        }
    }
}