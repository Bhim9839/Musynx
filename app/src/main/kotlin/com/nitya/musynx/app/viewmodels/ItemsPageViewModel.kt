@file:Suppress("UnusedImport")

package com.nitya.musynx.app.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.innertube.utils.plus

class ItemsPageViewModel<T : Innertube.Item> : ViewModel() {
    var itemsMap: MutableMap<String, Innertube.ItemsPage<T>?> = mutableStateMapOf()

    fun setItems(
        tag: String,
        items: Innertube.ItemsPage<T>?
    ) {
        if (!itemsMap.containsKey(tag)) itemsMap[tag] = items
        else itemsMap[tag] += items!!
    }
}