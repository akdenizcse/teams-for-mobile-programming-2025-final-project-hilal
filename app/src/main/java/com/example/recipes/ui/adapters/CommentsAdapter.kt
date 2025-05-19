// app/src/main/java/com/example/recipes/ui/adapters/CommentsAdapter.kt
package com.example.recipes.ui.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.Comment

class CommentsAdapter(
    private val onReply: (Comment) -> Unit
) : ListAdapter<Comment, CommentsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(a: Comment, b: Comment) = a.id == b.id
            override fun areContentsTheSame(a: Comment, b: Comment) = a == b
        }

        // 32dp in pixels, we'll compute it once:
        private fun View.dpToPx(dp: Int): Int =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
    }

    // Holds each comment's computed depth
    private val depthMap = mutableMapOf<String, Int>()

    override fun submitList(list: MutableList<Comment>?) {
        super.submitList(list?.let { ArrayList(it) })
        computeDepths()
    }

    private fun computeDepths() {
        depthMap.clear()
        // for quick lookup by id
        val byId = currentList.associateBy { it.id }
        currentList.forEach { c ->
            var d = 0
            var parent = c.parentId
            // climb the parent chain
            while (parent != null) {
                d++
                parent = byId[parent]?.parentId
            }
            depthMap[c.id] = d
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView   = view.findViewById(R.id.commentUserName)
        val text: TextView       = view.findViewById(R.id.commentText)
        val reply: TextView      = view.findViewById(R.id.commentReply)
        val container: View      = view.findViewById(R.id.commentContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
    )

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = getItem(pos)
        h.userName.text = c.userName
        h.text.text     = c.text

        // indent by depth * 32dp
        val d = depthMap[c.id] ?: 0
        h.container.setPadding(
            h.container.dpToPx(32 * d),
            h.container.paddingTop,
            h.container.paddingRight,
            h.container.paddingBottom
        )

        h.reply.setOnClickListener { onReply(c) }
    }
}
