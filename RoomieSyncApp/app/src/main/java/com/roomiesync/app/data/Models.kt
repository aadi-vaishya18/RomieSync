package com.roomiesync.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Flatmate(
    val id: String,
    val name: String,
    val color: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("color", color)
    }

    companion object {
        fun fromJson(o: JSONObject): Flatmate = Flatmate(
            id = o.getString("id"),
            name = o.getString("name"),
            color = o.getString("color")
        )

        fun initials(name: String): String {
            val parts = name.trim().split(Regex("\\s+"))
            val first = parts.getOrNull(0)?.firstOrNull()?.uppercaseChar() ?: '?'
            val second = parts.getOrNull(1)?.firstOrNull()?.uppercaseChar()
            return if (second != null) "$first$second" else "$first"
        }
    }
}

data class Chore(
    val id: String,
    var title: String,
    var assignedTo: String,
    var weight: Int,
    var done: Boolean,
    val createdAt: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("assignedTo", assignedTo)
        put("weight", weight)
        put("done", done)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): Chore = Chore(
            id = o.getString("id"),
            title = o.getString("title"),
            assignedTo = o.getString("assignedTo"),
            weight = o.optInt("weight", 1),
            done = o.optBoolean("done", false),
            createdAt = o.optString("createdAt", "")
        )
    }
}

data class Expense(
    val id: String,
    var title: String,
    var amount: Double,
    var category: String,
    var paidBy: String,
    var participants: MutableList<String>,
    val date: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("amount", amount)
        put("category", category)
        put("paidBy", paidBy)
        put("participants", JSONArray(participants))
        put("date", date)
    }

    companion object {
        fun fromJson(o: JSONObject): Expense {
            val arr = o.getJSONArray("participants")
            val list = MutableList(arr.length()) { i -> arr.getString(i) }
            return Expense(
                id = o.getString("id"),
                title = o.getString("title"),
                amount = o.getDouble("amount"),
                category = o.optString("category", "Other"),
                paidBy = o.getString("paidBy"),
                participants = list,
                date = o.optString("date", "")
            )
        }
    }
}

data class Payment(
    val id: String,
    val from: String,
    val to: String,
    val amount: Double,
    val date: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("from", from)
        put("to", to)
        put("amount", amount)
        put("date", date)
    }

    companion object {
        fun fromJson(o: JSONObject): Payment = Payment(
            id = o.getString("id"),
            from = o.getString("from"),
            to = o.getString("to"),
            amount = o.getDouble("amount"),
            date = o.optString("date", "")
        )
    }
}

/** A simplified debt: `from` owes `to` the given `amount`. */
data class DebtTransaction(val from: String, val to: String, val amount: Double)

data class AppState(
    var flatName: String,
    var currentUserId: String,
    val flatmates: MutableList<Flatmate>,
    val chores: MutableList<Chore>,
    val expenses: MutableList<Expense>,
    val payments: MutableList<Payment>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("flatName", flatName)
        put("currentUserId", currentUserId)
        put("flatmates", JSONArray(flatmates.map { it.toJson() }))
        put("chores", JSONArray(chores.map { it.toJson() }))
        put("expenses", JSONArray(expenses.map { it.toJson() }))
        put("payments", JSONArray(payments.map { it.toJson() }))
    }

    fun flatmateById(id: String): Flatmate? = flatmates.find { it.id == id }

    companion object {
        fun fromJson(o: JSONObject): AppState {
            fun <T> arr(key: String, map: (JSONObject) -> T): MutableList<T> {
                val a = o.getJSONArray(key)
                return MutableList(a.length()) { i -> map(a.getJSONObject(i)) }
            }
            return AppState(
                flatName = o.optString("flatName", "Our Flat"),
                currentUserId = o.getString("currentUserId"),
                flatmates = arr("flatmates", Flatmate::fromJson),
                chores = arr("chores", Chore::fromJson),
                expenses = arr("expenses", Expense::fromJson),
                payments = arr("payments", Payment::fromJson)
            )
        }
    }
}
