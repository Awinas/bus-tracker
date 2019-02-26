package hu.attilavegh.vbkoveto

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.Toolbar
import android.widget.ImageButton
import hu.attilavegh.vbkoveto.controller.AuthController
import hu.attilavegh.vbkoveto.utility.NotificationBarUtils
import hu.attilavegh.vbkoveto.controller.NotificationController
import hu.attilavegh.vbkoveto.utility.*

import hu.attilavegh.vbkoveto.model.*
import hu.attilavegh.vbkoveto.view.user.BusFragment
import hu.attilavegh.vbkoveto.view.user.MapBusFragment
import hu.attilavegh.vbkoveto.view.user.MapBusesFragment
import hu.attilavegh.vbkoveto.view.user.NotificationFragment
import hu.attilavegh.vbkoveto.view.user.ProfileFragment
import kotlinx.android.synthetic.main.activity_user.*

class UserActivity : AppCompatActivity(),
    BusFragment.OnBusListItemInteractionListener,
    MapBusFragment.OnBusFragmentInterActionListener,
    MapBusesFragment.OnBusesFragmentInteractionListener,
    ProfileFragment.OnProfileFragmentInteractionListener,
    NotificationFragment.OnNotificationFragmentInteractionListener {

    private lateinit var toolbar: Toolbar

    lateinit var user: UserModel

    lateinit var titleUtils: ActivityTitleUtils
    private lateinit var fragmentUtils: FragmentUtils

    private lateinit var notificationController: NotificationController
    private lateinit var notificationBarUtils: NotificationBarUtils
    private lateinit var authController: AuthController

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.bus_list_item -> {
                openFragment(R.string.title_buses, BusFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.map_item -> {
                openFragment(R.string.title_buses, MapBusesFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.profile_item -> {
                openFragment(R.string.title_profile, ProfileFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        setContentView(R.layout.activity_user)
        toolbar = findViewById(R.id.toolbar)

        titleUtils = ActivityTitleUtils(toolbar)
        fragmentUtils = FragmentUtils(supportFragmentManager)

        notificationController = NotificationController(this)
        notificationBarUtils = NotificationBarUtils(this)
        authController = AuthController(this)

        enableNotification()
        user = authController.getUser()

        navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        navigation.selectedItemId = R.id.bus_list_item
    }

    override fun onResume() {
        super.onResume()

        notificationController.removeAllNotifications()
        checkBusFromNotification()
    }

    override fun onBusSelection(bus: Bus) {
        onBusClick(bus)
    }

    override fun onFavoriteAdd(bus: Bus, button: ImageButton) {
        if (notificationController.isEnabled()) {
            notificationController.add(bus)
            button.setImageResource(R.drawable.favorite_on)
        } else {
            notificationBarUtils.show(R.string.notification_disabled)
        }
    }

    override fun onFavoriteRemove(bus: Bus, button: ImageButton) {
        notificationController.remove(bus)
        button.setImageResource(R.drawable.favorite_off)
    }

    override fun finishActivityAfterLogout() {
        finish()
    }

    override fun onBackPressed() {
        val busFragmentId: Int = R.id.bus_list_item
        val isMainFragmentActive = navigation.selectedItemId == busFragmentId

        val hasSubFragment = supportFragmentManager.backStackEntryCount > 0

        if (isMainFragmentActive || hasSubFragment) {
            super.onBackPressed()

            if (hasSubFragment) {
                titleUtils.setPrevious()
            }
        } else {
            navigation.selectedItemId = busFragmentId
            titleUtils.set(getString(R.string.title_buses))
        }
    }


    private fun openFragment(titleId: Int, fragment: Fragment, bundle: Bundle = Bundle.EMPTY) {
        titleUtils.set(getString(titleId))
        fragmentUtils.switchTo(R.id.user_fragment_container, fragment, bundle)
    }

    private fun onBusClick(bus: Bus) {
        checkBus(bus)
    }

    private fun checkBus(bus: Bus) {
        when (bus.active) {
            true -> initCheckBusView(bus)
            false -> notificationBarUtils.showError(R.string.inactive_bus_message)
        }
    }

    private fun initCheckBusView(bus: Bus) {
        val argument = Bundle()
        argument.putString("id", bus.id)

        val mapFragment = MapBusFragment.newInstance()
        fragmentUtils.switchTo(R.id.user_fragment_container, mapFragment, FragmentTagName.BUS_LOCATION.name, argument)

        titleUtils.set(bus.name)
    }

    private fun checkBusFromNotification() {
        val isNotification = intent.getBooleanExtra("notification", false)
        val busId = intent.getStringExtra("busId")
        val busName = intent.getStringExtra("busName")

        if (isNotification) {
            val bus = Bus(busId, busName)
            initCheckBusView(bus)

            intent.removeExtra("notification")
        }
    }

    private fun enableNotification() {
        if (notificationController.isFirstStart()) {
            notificationController.markFirstStart()
            notificationController.enable()
        }
    }
}
