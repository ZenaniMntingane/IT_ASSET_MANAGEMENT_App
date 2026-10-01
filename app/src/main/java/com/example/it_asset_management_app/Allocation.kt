package com.example.it_asset_management_app

data class Allocation(
    var allocationId: String = "",
    var assetNumber: String = "",
    var assetName: String = "",
    var serialNumber: String = "",
    var assetType: String = "",
    var assignedUser: String = "",
    var department: String = "",
    var allocationDate: String = "")
