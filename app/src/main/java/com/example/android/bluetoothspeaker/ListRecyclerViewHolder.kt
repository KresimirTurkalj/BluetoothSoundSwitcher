package com.example.android.bluetoothspeaker

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListRecyclerViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val itemName = itemView.findViewById(R.id.item_name) as TextView
    val itemPair = itemView.findViewById(R.id.item_pair) as TextView
}