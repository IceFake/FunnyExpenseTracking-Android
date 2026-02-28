package com.example.funnyexpensetracking.util

import android.content.Context
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

/**
 * Markdown渲染工具
 * 使用Markwon库将Markdown文本渲染到TextView中
 * 支持：加粗、斜体、删除线、列表、代码块、表格、标题、链接等
 */
object MarkdownRenderer {

    @Volatile
    private var markwon: Markwon? = null

    /**
     * 获取或创建Markwon实例（单例，线程安全）
     */
    private fun getInstance(context: Context): Markwon {
        return markwon ?: synchronized(this) {
            markwon ?: Markwon.builder(context.applicationContext)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context.applicationContext))
                .build()
                .also { markwon = it }
        }
    }

    /**
     * 将Markdown文本渲染到TextView
     * @param textView 目标TextView
     * @param markdown Markdown格式的文本
     */
    fun render(textView: TextView, markdown: String) {
        val instance = getInstance(textView.context)
        instance.setMarkdown(textView, markdown)
    }

    /**
     * 将Markdown文本转换为Spanned（可用于其他场景）
     * @param context 上下文
     * @param markdown Markdown格式的文本
     * @return 渲染后的Spanned文本
     */
    fun toMarkdown(context: Context, markdown: String): CharSequence {
        val instance = getInstance(context)
        return instance.toMarkdown(markdown)
    }
}

