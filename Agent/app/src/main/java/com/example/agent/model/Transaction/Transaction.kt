package com.example.agent.model.Transaction

import androidx.room.Entity
import androidx.room.PrimaryKey
enum class Classification(val label: String) {
    FOOD("餐饮"),
    TRANSPORT("交通"),
    SHOPPING("购物"),
    EDUCATION("教育"),
    ENTERTAINMENT("娱乐"),
    MEDICAL("医疗"),
    HOUSING("住房"),
    UTILITIES("水电气"),
    COMMUNICATION("通讯"),
    TRAVEL("旅游"),
    INVESTMENT("理财"),
    INCOME("收入"),
    OTHER("其他")
}
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Float,
    val merchant: String = "",
    val note: String = "",
    val classification: Classification = Classification.OTHER,
    val time: String,
    val timeMillis: Long
)