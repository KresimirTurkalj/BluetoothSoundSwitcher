package com.example.android.bluetoothspeaker

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_view_holder.view.*


class ListRecyclerViewAdapter(
    private val context: Context,
    private val listOfDevices: List<BluetoothDevice>,
    private val thisView: RecyclerView,
    private val otherView: RecyclerView
    ) :
    RecyclerView.Adapter<ListRecyclerViewHolder>() {

    companion object{
        private const val NOT_SELECTED = -1
    }
    private var selectedItem = NOT_SELECTED
    private var itemPair = MutableList(listOfDevices.size) { NOT_SELECTED }
    private var BluetoothDevice.selectedPair
        get() = itemPair[listOfDevices.indexOf(this)]
        set(value) { itemPair[listOfDevices.indexOf(this)] = value }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListRecyclerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.list_view_holder,
            parent,
            false
        )
        return ListRecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listOfDevices.size
    }

    override fun onBindViewHolder(holder: ListRecyclerViewHolder, position: Int) {
        holder.itemName.text = listOfDevices[position].name
        holder.itemPair.text = "?"
        holder.itemView.setOnClickListener{
            setDevicePair(holder, position)
        }
    }

    fun retrieveSelectedDevices(): MutableList<BluetoothDevice>{
        val listOfSelectedDevices= mutableListOf<BluetoothDevice>()
        for (device in listOfDevices){
            if(device.selectedPair != NOT_SELECTED){
                listOfSelectedDevices.add(device)
            }
        }
        listOfSelectedDevices.sortBy { it.selectedPair }
        return listOfSelectedDevices
    }

    private fun setDevicePair(holder: ListRecyclerViewHolder,position: Int){
        val otherAdapter = otherView.adapter as ListRecyclerViewAdapter
        if(otherAdapter.selectedItem == NOT_SELECTED){
            when (selectedItem) {
                NOT_SELECTED -> {
                    selectedItem = position
                    changeViewHolderBackgroundColor(holder.itemView, null)
                }
                position -> {
                    selectedItem = NOT_SELECTED
                    changeViewHolderBackgroundColor(null, holder.itemView)
                }
                else -> {
                    val view = thisView.getChildAt(selectedItem)
                    selectedItem = position
                    changeViewHolderBackgroundColor(holder.itemView,view)
                }
            }
        }
        else {
            checkForSameNumber(otherAdapter.selectedItem)
            otherAdapter.checkForSameNumber(position)
            otherAdapter.deselectItem()
            insertPair(position, otherAdapter.selectedItem)
            otherAdapter.insertPair(otherAdapter.selectedItem, position)
            otherAdapter.selectedItem = NOT_SELECTED
        }
    }

    private fun checkForSameNumber(removeItem: Int) {
        listOfDevices.forEachIndexed { index, bluetoothDevice ->
            if(bluetoothDevice.selectedPair == removeItem){
                bluetoothDevice.selectedPair = NOT_SELECTED
                val view = thisView.getChildAt(index)
                view.item_pair.text = "?"
            }
        }
    }

    private fun insertPair(position: Int, pair: Int) {
        val view = thisView.getChildAt(position)
        listOfDevices[position].selectedPair = pair
        view.item_pair.text = (pair + 1).toString()
    }

    private fun deselectItem() {
        val view = thisView.getChildAt(selectedItem)
        changeViewHolderBackgroundColor(null, view)
    }

    private fun changeViewHolderBackgroundColor(newView: View?, oldView: View?){
        newView?.setBackgroundColor(getColor(context,R.color.selectedBackgroundColor))
        oldView?.setBackgroundColor(getColor(context,R.color.listBackgroundColor))
    }
}