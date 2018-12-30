package com.gb.canibuyit.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.gb.canibuyit.R

class SpendingEditorActivity : AppCompatActivity() {

    private lateinit var spendingEditorFragment: SpendingEditorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_editor)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            spendingEditorFragment = SpendingEditorFragment().apply {
                if (intent.hasExtra(EXTRA_SPENDING_ID)) {
                    arguments = Bundle().apply {
                        putInt(EXTRA_SPENDING_ID, intent.getIntExtra(EXTRA_SPENDING_ID, 0))
                    }
                }
            }
            supportFragmentManager
                    ?.beginTransaction()
                    ?.add(R.id.spending_editor_container, spendingEditorFragment)
                    ?.commit()
        } else {
            spendingEditorFragment = supportFragmentManager.findFragmentById(R.id.spending_editor_container) as SpendingEditorFragment
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            spendingEditorFragment.onFragmentClose(this@SpendingEditorActivity::finish)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        spendingEditorFragment.onFragmentClose { super.onBackPressed() }
    }

    companion object {

        fun getIntentForUpdate(context: Context, spendingId: Int): Intent {
            return getIntentForCreate(context).apply {
                putExtra(EXTRA_SPENDING_ID, spendingId)
            }
        }

        fun getIntentForCreate(context: Context): Intent {
            return Intent(context, SpendingEditorActivity::class.java)
        }
    }
}
