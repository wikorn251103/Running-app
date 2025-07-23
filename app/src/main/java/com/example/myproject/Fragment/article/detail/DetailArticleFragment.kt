package com.example.myproject.Fragment.article.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myproject.R
import com.example.myproject.corre.BaseFragment
import com.example.myproject.databinding.FragmentDetailArticleBinding

class DetailArticleFragment : BaseFragment<FragmentDetailArticleBinding>(FragmentDetailArticleBinding::inflate) {

    override fun initViews() {

    }

    companion object {
        fun newInstance() = DetailArticleFragment()
    }
}