package com.keyri.accounts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.keyrisdk.entity.PublicAccount
import com.keyri.R
import kotlinx.android.synthetic.main.item_account.view.*

class AccountsAdapter(
    context: Context,
    private val listener: (PublicAccount) -> Unit
) : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var items = emptyList<PublicAccount>()

    fun setItems(items: List<PublicAccount>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(inflater.inflate(R.layout.item_account, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.apply {
            tvName.text = item.username
        }
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                listener.invoke(items[adapterPosition])
            }
        }
    }
}
