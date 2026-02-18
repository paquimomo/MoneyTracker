package com.example.moneytracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val fileManager: FileManager,
    private val onItemClick: (Expense) -> Unit,
    private val onItemLongClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val ivExpenseImage: ImageView = view.findViewById(R.id.ivExpenseImage)
        val ivHasImage: ImageView = view.findViewById(R.id.ivHasImage)
        val categoryIndicator: View = view.findViewById(R.id.categoryIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]

        holder.tvDescription.text = expense.description
        holder.tvAmount.text = String.format("$%.2f", expense.amount)
        holder.tvCategory.text = expense.category
        holder.tvDateTime.text = "${expense.date} ${expense.time}"

        val categoryColor = getCategoryColor(expense.category)
        holder.categoryIndicator.setBackgroundColor(categoryColor)
        holder.tvCategory.setTextColor(categoryColor)

        if (expense.imagePath != null) {
            val bitmap = fileManager.getImageFromInternalStorage(expense.imagePath)

            if (bitmap != null) {
                holder.ivExpenseImage.setImageBitmap(bitmap)
                holder.ivExpenseImage.visibility = View.VISIBLE
                holder.ivHasImage.visibility = View.VISIBLE
            } else {
                holder.ivExpenseImage.setImageResource(android.R.drawable.ic_menu_gallery)
                holder.ivExpenseImage.visibility = View.VISIBLE
                holder.ivHasImage.visibility = View.GONE
            }

        } else {
            holder.ivExpenseImage.visibility = View.GONE
            holder.ivHasImage.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(expense)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(expense)
            true
        }

        holder.ivExpenseImage.setOnClickListener {
            if (expense.imagePath != null) {
                showImagePreview(holder, expense.imagePath)
            }
        }
    }

    override fun getItemCount(): Int = expenses.size

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "Alimentación" -> Color.parseColor("#FF5722")
            "Transporte" -> Color.parseColor("#3F51B5")
            "Entretenimiento" -> Color.parseColor("#E91E63")
            "Salud" -> Color.parseColor("#4CAF50")
            "Compras" -> Color.parseColor("#9C27B0")
            "Servicios" -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#607D8B")
        }
    }

    private fun showImagePreview(holder: ViewHolder, imagePath: String) {
        val bitmap = fileManager.getImageFromInternalStorage(imagePath)
        if (bitmap != null) {
            val context = holder.itemView.context
            val imageView = ImageView(context).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(20, 20, 20, 20)
            }

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Vista previa del gasto")
                .setView(imageView)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }
}