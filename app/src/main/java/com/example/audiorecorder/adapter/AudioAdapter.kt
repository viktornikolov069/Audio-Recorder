package com.example.audiorecorder.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.audiorecorder.databinding.ItemLayoutBinding
import com.example.audiorecorder.db.AudioRecord
import java.text.SimpleDateFormat
import java.util.*

class AudioAdapter(var listener: OnItemClickListener): RecyclerView.Adapter<AudioAdapter.AudioHolder>() {

    private lateinit var binding: ItemLayoutBinding
    private lateinit var context: Context
    private var editMode = false

    fun isEditMode(): Boolean {
        return editMode
    }

    fun setEditMode(mode: Boolean) {
        if (editMode != mode) {
            editMode = mode
            //notifyDataSetChanged()
        }
    }

    inner class AudioHolder: RecyclerView.ViewHolder(binding.root), View.OnClickListener,
                             View.OnLongClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        fun bind(record: AudioRecord) {
            binding.apply {

                var sdFormat = SimpleDateFormat("dd/MM/yyyy")
                var date = Date(record.timestamp)
                var strDate = sdFormat.format(date)

                tvFilename.text = record.filename
                tvMeta.text = "${record.duration} $strDate"

                if (editMode) {
                    cbChecked.visibility = View.VISIBLE
                    cbChecked.isChecked = record.isChecked
                } else {
                    cbChecked.visibility = View.GONE
                    cbChecked.isChecked = false
                }

                /*root.setOnClickListener {
                    val intent = Intent(context, DetailedActivity::class.java)
                    intent.putExtra(BUNDLE_TRANSACTION_ID, item.id)
                    context.startActivity(intent)
                }*/
            }
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClickListener(position)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemLongClickListener(position)
            }
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = ItemLayoutBinding.inflate(layoutInflater, parent, false)
        context = parent.context
        return AudioHolder()
    }

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        /* Check if position is valid */
        if (position != RecyclerView.NO_POSITION) {
            holder.bind(differ.currentList[position])
        }
    }

    override fun getItemCount(): Int = differ.currentList.size

    /* Using DiffUtil to improve recycler view performance */
    private val differCallback = object: DiffUtil.ItemCallback<AudioRecord>() {
        override fun areItemsTheSame(oldItem: AudioRecord, newItem: AudioRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AudioRecord, newItem: AudioRecord): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)
}
