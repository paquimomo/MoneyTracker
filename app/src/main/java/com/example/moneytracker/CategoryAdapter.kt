package com.example.moneytracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: List<Category>,
    private val onItemClick: (Category) -> Unit,
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private val categorySpent = mutableMapOf<String, Double>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvMonthlyLimit: TextView = view.findViewById(R.id.tvMonthlyLimit)
        val tvSpent: TextView = view.findViewById(R.id.tvSpent)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentage)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val colorIndicator: View = view.findViewById(R.id.colorIndicator)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val spent = categorySpent[category.id] ?: 0.0

        holder.tvCategoryName.text = category.name
        holder.tvMonthlyLimit.text = "Límite: $${String.format("%.2f", category.monthlyLimit)}"
        holder.tvSpent.text = "Gastado: $${String.format("%.2f", spent)}"

        val percentage = if (category.monthlyLimit > 0) {
            ((spent / category.monthlyLimit) * 100).toInt()
        } else {
            0
        }
        holder.tvPercentage.text = "$percentage%"
        holder.progressBar.progress = percentage

        try {
            holder.colorIndicator.setBackgroundColor(Color.parseColor(category.color))
        } catch (e: Exception) {
            holder.colorIndicator.setBackgroundColor(Color.GRAY)
        }

        holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
            when {
                percentage < 50 -> Color.parseColor("#4CAF50")  // Verde
                percentage < 80 -> Color.parseColor("#FF9800")  // Naranja
                else -> Color.parseColor("#F44336")  // Rojo
            }
        )

        holder.itemView.setOnClickListener {
            onItemClick(category)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(category)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(category)
        }

        if (category.isDefault) {
            holder.btnDelete.isEnabled = false
            holder.btnDelete.alpha = 0.3f
        } else {
            holder.btnDelete.isEnabled = true
            holder.btnDelete.alpha = 1.0f
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategorySpent(categoryId: String, spent: Double) {
        categorySpent[categoryId] = spent
        notifyDataSetChanged()
    }
}