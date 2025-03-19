package com.example.learnandroidfinal.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.learnandroidfinal.R
import com.example.learnandroidfinal.databinding.FragmentFirstBinding

class FirstFragment : BaseFragment() {

    companion object {
        const val COUNT_ARGUMENT = "COUNT_ARGUMENT"
        const val NAME_ARGUMENT = "NAME_ARGUMENT"
    }

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonGoSecond.setOnClickListener {
            parentFragmentManager.commit {
                replace<SecondFragment>(
                    containerViewId = R.id.fragment_container,
                    tag = "SecondFragment",
                    args = bundleOf(
                        COUNT_ARGUMENT to 0,
                        NAME_ARGUMENT to "duc"
                    )
                )
                setReorderingAllowed(true)
                addToBackStack("firstFragment")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
