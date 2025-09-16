package com.example.myproject.Fragment.article.list

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.article.detail.DetailArticleFragment
import com.example.myproject.Fragment.article.list.adapter.ArticleAdapter
import com.example.myproject.Fragment.article.list.adapter.ArticleListener
import com.example.myproject.R
import com.example.myproject.data.article.ArticleModel
import com.example.myproject.data.article.ArticleRepositoryImpl
import com.example.myproject.data.article.ArticleServiceImpl
import com.example.myproject.databinding.FragmentArticleBinding

class ArticlesFragment : Fragment(), ArticleListener {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArticleAdapter
    private val viewModel: ArticlesViewModel by viewModels {
        ArticlesViewModelFactory(ArticleRepositoryImpl(ArticleServiceImpl()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //สร้าง Adapter โดยไม่ต้องส่งข้อมูลเริ่มต้น เพราะ ListAdapter จะจัดการเอง
        adapter = ArticleAdapter(this)

        //กำหนด LayoutManager และ Adapter ให้กับ RecyclerView
        binding.articlesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.articlesRecyclerView.adapter = adapter

        //เรียกใช้ ViewModel เพื่อนำข้อมูลมาจาก Firebase หรือ Repository
        viewModel.getArticles()

        //อัพเดท UI เมื่อ articles เปลี่ยนแปลง
        viewModel.articles.observe(viewLifecycleOwner) { articles ->
            // เมื่อข้อมูลโหลดเสร็จแล้ว ซ่อน progressBar และอัพเดทข้อมูลใน adapter
            binding.progressBar.visibility = View.GONE
            adapter.submitList(articles) // submitList ใช้กับ ListAdapter
        }

        //แสดง progressBar ขณะโหลดข้อมูล
        binding.progressBar.visibility = View.VISIBLE
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onArticleClicked(article: ArticleModel) {
        Log.d("ArticleFragment", "onArticleClicked: $article")
        val detailFragment = DetailArticleFragment.newInstance(article)

        parentFragmentManager.beginTransaction()
            .replace(R.id.container_main, detailFragment)
            .addToBackStack(null)
            .commit()
    }
    companion object{
        fun newInstance() = ArticlesFragment()
    }
}