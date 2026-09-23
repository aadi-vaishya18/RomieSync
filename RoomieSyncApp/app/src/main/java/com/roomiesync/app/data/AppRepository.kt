package com.roomiesync.app.data

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.min

object AppRepository {

    private const val PREFS_NAME = "roomiesync_prefs"
    private const val KEY_STATE = "app_state"

    fun newId(): String = UUID.randomUUID().toString().take(8)

    fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun formatDate(iso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val display = SimpleDateFormat("d MMM", Locale.US)
            display.format(parser.parse(iso)!!)
        } catch (e: Exception) {
            iso
        }
    }

    // ---------- Persistence ----------

    fun load(context: Context): AppState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STATE, null)
        if (json != null) {
            try {
                return AppState.fromJson(JSONObject(json))
            } catch (e: Exception) {
                // Corrupt or outdated data — fall through and reseed.
            }
        }
        val seeded = seedState()
        save(context, seeded)
        return seeded
    }

    fun save(context: Context, state: AppState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STATE, state.toJson().toString()).apply()
    }

    fun reset(context: Context): AppState {
        val seeded = seedState()
        save(context, seeded)
        return seeded
    }

    // ---------- Seed / demo data ----------

    fun seedState(): AppState {
        val meera = newId()
        val karan = newId()
        val devika = newId()
        val today = today()

        val flatmates = mutableListOf(
            Flatmate(meera, "Meera Iyer", "#3A5A78"),
            Flatmate(karan, "Karan Shah", "#E08E45"),
            Flatmate(devika, "Devika Nair", "#2E7D5B")
        )

        val chores = mutableListOf(
            Chore(newId(), "Take out the trash", meera, 1, false, today),
            Chore(newId(), "Wash the dishes", karan, 1, false, today),
            Chore(newId(), "Water the plants", devika, 1, true, today),
            Chore(newId(), "Deep-clean the kitchen", meera, 3, false, today)
        )

        val expenses = mutableListOf(
            Expense(newId(), "Groceries", 840.0, "Groceries", meera, mutableListOf(meera, karan, devika), today),
            Expense(newId(), "Electricity Bill", 1200.0, "Utilities", karan, mutableListOf(meera, karan, devika), today),
            Expense(newId(), "Wifi Recharge", 600.0, "Wifi", devika, mutableListOf(meera, karan, devika), today)
        )

        return AppState(
            flatName = "Flat 4B",
            currentUserId = meera,
            flatmates = flatmates,
            chores = chores,
            expenses = expenses,
            payments = mutableListOf()
        )
    }

    // ---------- Balance engine ----------

    /** Net balance per flatmate: positive = the group owes them, negative = they owe the group. */
    fun computeNet(state: AppState): Map<String, Double> {
        val net = mutableMapOf<String, Double>()
        state.flatmates.forEach { net[it.id] = 0.0 }

        state.expenses.forEach { e ->
            if (!net.containsKey(e.paidBy)) return@forEach
            net[e.paidBy] = (net[e.paidBy] ?: 0.0) + e.amount
            val parts = e.participants.filter { net.containsKey(it) }
            if (parts.isEmpty()) return@forEach
            val share = e.amount / parts.size
            parts.forEach { id -> net[id] = (net[id] ?: 0.0) - share }
        }

        state.payments.forEach { p ->
            if (net.containsKey(p.from)) net[p.from] = (net[p.from] ?: 0.0) + p.amount
            if (net.containsKey(p.to)) net[p.to] = (net[p.to] ?: 0.0) - p.amount
        }

        return net
    }

    /** Greedy min-cash-flow simplification: turns a net-balance map into a short list of who-pays-whom. */
    fun simplifyDebts(net: Map<String, Double>): List<DebtTransaction> {
        data class Mutable(val id: String, var amt: Double)

        val creditors = net.filter { it.value > 0.5 }.map { Mutable(it.key, it.value) }
            .sortedByDescending { it.amt }.toMutableList()
        val debtors = net.filter { it.value < -0.5 }.map { Mutable(it.key, -it.value) }
            .sortedByDescending { it.amt }.toMutableList()

        val tx = mutableListOf<DebtTransaction>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val amt = min(debtors[i].amt, creditors[j].amt)
            tx.add(DebtTransaction(debtors[i].id, creditors[j].id, Math.round(amt).toDouble()))
            debtors[i].amt -= amt
            creditors[j].amt -= amt
            if (debtors[i].amt < 0.5) i++
            if (creditors[j].amt < 0.5) j++
        }
        return tx
    }
}
