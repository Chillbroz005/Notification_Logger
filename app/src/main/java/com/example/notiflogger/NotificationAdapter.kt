package com.example.notiflogger

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (NotificationEntity) -> Unit,
    private val onImageClick: ((NotificationEntity) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ListItem> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
        private const val THUMB_SIZE_PX = 150
    }

    fun submitList(newItems: List<ListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.Row -> TYPE_ROW
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.textHeaderLabel)
    }

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.imageThumbnail)
        val appName: TextView = view.findViewById(R.id.textAppName)
        val title: TextView = view.findViewById(R.id.textTitle)
        val text: TextView = view.findViewById(R.id.textBody)
        val time: TextView = view.findViewById(R.id.textTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false))
        } else {
            RowViewHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> {
                (holder as HeaderViewHolder).label.text = "${item.label} (${item.count})"
            }
            is ListItem.Row -> {
                val rowHolder = holder as RowViewHolder
                val entity = item.entity
                rowHolder.appName.text = entity.appName
                rowHolder.title.text = entity.title
                rowHolder.text.text = entity.text
                rowHolder.time.text = dateFormat.format(Date(entity.timestamp))
                rowHolder.itemView.setOnClickListener { onItemClick(entity) }

                if (entity.imagePath != null && File(entity.imagePath).exists()) {
                    rowHolder.thumbnail.visibility = View.VISIBLE
                    rowHolder.thumbnail.setImageBitmap(decodeSampledBitmap(entity.imagePath, THUMB_SIZE_PX))
                    rowHolder.thumbnail.setOnClickListener {
                        onImageClick?.invoke(entity) ?: onItemClick(entity)
                    }
                } else {
                    rowHolder.thumbnail.visibility = View.GONE
                    rowHolder.thumbnail.setImageBitmap(null)
                    rowHolder.thumbnail.setOnClickListener(null)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun decodeSampledBitmap(path: String, targetSizePx: Int): android.graphics.Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, boundsOptions)

            var sampleSize = 1
            var width = boundsOptions.outWidth
            var height = boundsOptions.outHeight
            while (width / 2 >= targetSizePx && height / 2 >= targetSizePx) {
                width /= 2
                height /= 2
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }
}
