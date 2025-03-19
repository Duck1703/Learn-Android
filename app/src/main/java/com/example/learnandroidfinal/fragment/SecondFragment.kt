package com.example.learnandroidfinal.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.learnandroidfinal.R
import com.example.learnandroidfinal.databinding.FragmentFirstBinding
import com.example.learnandroidfinal.databinding.FragmentSencondBinding
import com.example.learnandroidfinal.fragment.FirstFragment.Companion.COUNT_ARGUMENT
import com.example.learnandroidfinal.fragment.FirstFragment.Companion.NAME_ARGUMENT

class SecondFragment : BaseFragment() {
    private var _binding: FragmentSencondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSencondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val count = requireArguments().getInt(FirstFragment.COUNT_ARGUMENT)
        val name = requireArguments().getString(FirstFragment.NAME_ARGUMENT)
        Log.d("SecondFragment", "cout = $count ; name = $name")

        binding.buttonGoSecond.setOnClickListener {
            parentFragmentManager.commit {
                replace<ThirdFragment>(containerViewId = R.id.fragment_container)
                setReorderingAllowed(true)
                addToBackStack("secondFragment")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
