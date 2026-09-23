package com.roomiesync.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.data.AppState
import com.roomiesync.app.databinding.ActivityMainBinding
import com.roomiesync.app.ui.ChoresFragment
import com.roomiesync.app.ui.ExpensesFragment
import com.roomiesync.app.ui.HomeFragment
import com.roomiesync.app.ui.ProfileFragment
import com.roomiesync.app.ui.SettleUpFragment
import com.roomiesync.app.ui.dialogs.AddChoreSheet
import com.roomiesync.app.ui.dialogs.AddExpenseSheet

/** Implemented by every tab fragment so MainActivity can tell it to redraw after a data change. */
interface Refreshable {
    fun refresh()
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var state: AppState
        private set

    private var currentTab: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        state = AppRepository.load(this)

        binding.bottomNav.setOnItemSelectedListener { item ->
            // Any direct tab tap clears the Settle Up back-stack entry, mirroring the web app's
            // "tapping a tab always resets to that tab" behavior.
            supportFragmentManager.popBackStack("settle", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.nav_home -> { showFragment(HomeFragment(), "home", addToBackStack = false) }
                R.id.nav_chores -> { showFragment(ChoresFragment(), "chores", addToBackStack = false) }
                R.id.nav_expenses -> { showFragment(ExpensesFragment(), "expenses", addToBackStack = false) }
                R.id.nav_profile -> { showFragment(ProfileFragment(), "profile", addToBackStack = false) }
            }
            true
        }

        binding.fab.setOnClickListener {
            when (currentTab) {
                "chores" -> AddChoreSheet().show(supportFragmentManager, "add_chore")
                else -> AddExpenseSheet().show(supportFragmentManager, "add_expense")
            }
        }

        binding.topbarBack.setOnClickListener {
            supportFragmentManager.popBackStack()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val onSettle = supportFragmentManager.findFragmentById(R.id.fragmentContainer) is SettleUpFragment
            binding.topbarBack.visibility = if (onSettle) android.view.View.VISIBLE else android.view.View.GONE
            binding.fab.visibility = if (onSettle) android.view.View.GONE else android.view.View.VISIBLE
        }

        if (savedInstanceState == null) {
            showFragment(HomeFragment(), "home", addToBackStack = false)
        }

        refreshTopBar()
    }

    private fun showFragment(fragment: Fragment, tab: String, addToBackStack: Boolean) {
        currentTab = tab
        binding.fab.visibility = if (tab == "profile") android.view.View.GONE else android.view.View.VISIBLE
        val tx = supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment)
        if (addToBackStack) tx.addToBackStack("settle")
        tx.commit()
    }

    /** Called by HomeFragment when the person taps "Settle Up ›". */
    fun openSettleUp() {
        showFragment(SettleUpFragment(), "settle", addToBackStack = true)
    }

    fun refreshTopBar() {
        binding.topbarFlatName.text = state.flatName
        binding.topbarSubtitle.text = "${state.flatmates.size} flatmates · RoomieSync"
    }

    /** Persist the current state, refresh the top bar, and tell the visible fragment to redraw. */
    fun saveAndRefresh() {
        AppRepository.save(this, state)
        refreshTopBar()
        val visible = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        (visible as? Refreshable)?.refresh()
    }

    fun resetDemoData() {
        state = AppRepository.reset(this)
        refreshTopBar()
        supportFragmentManager.popBackStack("settle", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showFragment(HomeFragment(), "home", addToBackStack = false)
    }
}
