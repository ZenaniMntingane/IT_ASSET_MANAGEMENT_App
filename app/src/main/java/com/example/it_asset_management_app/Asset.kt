package com.example.it_asset_management_app

data class Asset(
    val assetName: String = "",
    var assetNumber: String = "",
    val serialNumber: String = "",
    val assetType: String = "",
    val condition: String = "",
    val location: String = "",
    val department: String = "",
    val assignedUser: String = "",
    val purchaseDate: String = "",
    val warrantyEndDate: String = "",
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var status: String = "Pending",
    var assignedTo: String = "",
    var createdAt: Long = 0L,
    val notes: String = "",

)
