package com.arkhe.sunmiv1s.utils

data class ReceiptData(
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val items: List<ReceiptItem>,
    val total: Int,
    val cash: Int,
    val change: Int,
    val dateTime: String
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Int
)