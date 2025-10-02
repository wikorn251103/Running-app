package com.example.myproject.Fragment.drill.detail

import android.net.Uri
import android.os.Bundle
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
        // ฝัง YouTube Video
        val videoId = extractYoutubeVideoId(drill.videoUrl)
        val html = """
            <html>
                <body style="margin:0;">
                    <iframe width="100%" height="100%"
                        src="https://www.youtube.com/embed/$videoId"
                        frameborder="0"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                </body>
            </html>
        """.trimIndent()

        with(binding.webViewVideo) {
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            loadData(html, "text/html", "utf-8")
        }

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
