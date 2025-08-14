package com.motungi.sinnamarket.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.motungi.sinnamarket.R

class PlaceholderFragment : Fragment() {

    private var categoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryName = it.getString(ARG_CATEGORY_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_placeholder, container, false)
        val textView: TextView = view.findViewById(R.id.fragment_text)
        textView.text = "${categoryName} 카테고리 게시글 목록"
        return view
    }

    companion object {
        private const val ARG_CATEGORY_NAME = "category_name"
        @JvmStatic
        fun newInstance(categoryName: String) =
            PlaceholderFragment().apply {
                arguments = bundleOf(ARG_CATEGORY_NAME to categoryName)
            }
    }
}
