package com.example.myproject.Fragment.article.detail

import android.os.Bundle
import coil.load
import com.example.myproject.corre.BaseFragment
import com.example.myproject.data.article.ArticleModel
import com.example.myproject.databinding.FragmentDetailArticleBinding

class DetailArticleFragment : BaseFragment<FragmentDetailArticleBinding>(FragmentDetailArticleBinding::inflate) {

    private lateinit var article: ArticleModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            article = it.getParcelable("article")!!
        }
    }

    override fun initViews() {
        binding.tvTitle.text = article.title
        binding.tvDescription.text = article.description
        binding.imageView.load(article.imageUrl)

        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        fun newInstance(aricle: ArticleModel) : DetailArticleFragment {
            val fragment = DetailArticleFragment()
            val args = Bundle()
            args.putParcelable("article", aricle)
            fragment.arguments = args
            return fragment
        }
    }
}