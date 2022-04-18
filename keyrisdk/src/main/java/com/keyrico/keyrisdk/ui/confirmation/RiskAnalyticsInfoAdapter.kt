package com.keyrico.keyrisdk.ui.confirmation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keyrico.keyrisdk.databinding.ItemRiskInfoBinding

class RiskAnalyticsInfoAdapter :
    ListAdapter<RiskAnalyticsItem, RiskAnalyticsInfoAdapter.RiskAnalyticsInfoViewHolder>(
        DIFF_CALLBACK
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiskAnalyticsInfoViewHolder {
        val binding =
            ItemRiskInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return RiskAnalyticsInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiskAnalyticsInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RiskAnalyticsInfoViewHolder(private val binding: ItemRiskInfoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(info: RiskAnalyticsItem) {
            with(binding) {
                tvTitle.text = info.title
                tvInfo.text = info.info
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RiskAnalyticsItem>() {
            override fun areItemsTheSame(
                oldItem: RiskAnalyticsItem,
                newItem: RiskAnalyticsItem
            ): Boolean = oldItem.title == newItem.title

            override fun areContentsTheSame(
                oldItem: RiskAnalyticsItem,
                newItem: RiskAnalyticsItem
            ): Boolean = oldItem == newItem
        }
    }
}
