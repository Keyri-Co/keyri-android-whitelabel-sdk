package com.example.keyrisdk.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.keyrisdk.R
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service

class KeyriAccountsAdapter(private val onItemSelected: (PublicAccount, String, Service) -> Unit) :
    ListAdapter<PublicAccount, KeyriAccountsAdapter.AccountViewHolder>(DIFF_CALLBACK) {

    var sessionId: String? = null
    var service: Service? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_choose_account, parent, false)

        return AccountViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvItemText = itemView.findViewById<TextView>(R.id.tvItemText)
        private val rlRoot = itemView.findViewById<RelativeLayout>(R.id.rlRoot)

        fun bind(account: PublicAccount) {
            tvItemText.text = account.username
            rlRoot.setOnClickListener {
                val sessionId = sessionId
                val service = service

                if (sessionId != null && service != null) {
                    onItemSelected(account, sessionId, service)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PublicAccount>() {
            override fun areItemsTheSame(oldItem: PublicAccount, newItem: PublicAccount): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(
                oldItem: PublicAccount,
                newItem: PublicAccount
            ): Boolean = oldItem.username == newItem.username
        }
    }
}
