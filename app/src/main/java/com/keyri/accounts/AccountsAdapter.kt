package com.keyri.accounts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyri.R
import com.keyri.databinding.ItemAccountBinding

class AccountsAdapter(context: Context, private val listener: (PublicAccount) -> Unit) :
    ListAdapter<PublicAccount, AccountsAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(inflater.inflate(R.layout.item_account, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val binding = ItemAccountBinding.bind(itemView)

        fun bind(item: PublicAccount) {
            binding.tvName.text = item.username

            binding.vRoot.setOnClickListener {
                listener.invoke(item)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PublicAccount>() {
            override fun areItemsTheSame(oldItem: PublicAccount, newItem: PublicAccount): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: PublicAccount,
                newItem: PublicAccount
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
