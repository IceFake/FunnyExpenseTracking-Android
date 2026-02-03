package com.example.funnyexpensetracking

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.funnyexpensetracking.ui.calendar.CalendarFragment
import com.example.funnyexpensetracking.ui.fixedincome.FixedIncomeFragment
import com.example.funnyexpensetracking.ui.investment.InvestmentFragment
import com.example.funnyexpensetracking.ui.transaction.TransactionFragment
import com.example.funnyexpensetracking.ui.usercenter.UserCenterFragment
import com.example.funnyexpensetracking.worker.AssetSnapshotWorker
import com.example.funnyexpensetracking.worker.StockPriceSyncWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    // 缓存 Fragment 实例
    private var transactionFragment: TransactionFragment? = null
    private var calendarFragment: CalendarFragment? = null
    private var fixedIncomeFragment: FixedIncomeFragment? = null
    private var investmentFragment: InvestmentFragment? = null
    private var userCenterFragment: UserCenterFragment? = null
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNavigation = findViewById(R.id.bottomNavigation)
        setupBottomNavigation()

        // 加载默认Fragment
        if (savedInstanceState == null) {
            transactionFragment = TransactionFragment()
            activeFragment = transactionFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, transactionFragment!!, "home")
                .commit()
        }

        // 初始化后台任务
        setupWorkers()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(getOrCreateTransactionFragment())
                    true
                }
                R.id.nav_calendar -> {
                    switchFragment(getOrCreateCalendarFragment())
                    true
                }
                R.id.nav_fixed -> {
                    switchFragment(getOrCreateFixedIncomeFragment())
                    true
                }
                R.id.nav_investment -> {
                    switchFragment(getOrCreateInvestmentFragment())
                    true
                }
                R.id.nav_user_center -> {
                    switchFragment(getOrCreateUserCenterFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun getOrCreateTransactionFragment(): Fragment {
        if (transactionFragment == null) {
            transactionFragment = TransactionFragment()
        }
        return transactionFragment!!
    }

    private fun getOrCreateCalendarFragment(): Fragment {
        if (calendarFragment == null) {
            calendarFragment = CalendarFragment()
        }
        return calendarFragment!!
    }


    private fun getOrCreateFixedIncomeFragment(): Fragment {
        if (fixedIncomeFragment == null) {
            fixedIncomeFragment = FixedIncomeFragment()
        }
        return fixedIncomeFragment!!
    }

    private fun getOrCreateInvestmentFragment(): Fragment {
        if (investmentFragment == null) {
            investmentFragment = InvestmentFragment()
        }
        return investmentFragment!!
    }

    private fun getOrCreateUserCenterFragment(): Fragment {
        if (userCenterFragment == null) {
            userCenterFragment = UserCenterFragment()
        }
        return userCenterFragment!!
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment === targetFragment) return

        val transaction = supportFragmentManager.beginTransaction()

        // 隐藏当前 Fragment
        activeFragment?.let { transaction.hide(it) }

        // 显示目标 Fragment
        if (targetFragment.isAdded) {
            transaction.show(targetFragment)
        } else {
            transaction.add(R.id.fragmentContainer, targetFragment)
        }

        transaction.commit()
        activeFragment = targetFragment
    }

    /**
     * 设置后台定时任务
     */
    private fun setupWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        // 每15分钟保存一次资产快照
        val assetSnapshotRequest = PeriodicWorkRequestBuilder<AssetSnapshotWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            AssetSnapshotWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            assetSnapshotRequest
        )

        // 每15分钟同步一次股票价格（股票交易时间内）
        val stockSyncRequest = PeriodicWorkRequestBuilder<StockPriceSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            StockPriceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            stockSyncRequest
        )
    }
}