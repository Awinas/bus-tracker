package hu.attilavegh.vbkoveto.view.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import hu.attilavegh.vbkoveto.R
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import hu.attilavegh.vbkoveto.model.Bus
import hu.attilavegh.vbkoveto.view.CAMERA_ZOOM
import hu.attilavegh.vbkoveto.view.MapFragmentBase
import io.reactivex.rxkotlin.addTo

class MapBusFragment : MapFragmentBase() {

    private var listener: OnBusFragmentInterActionListener? = null

    private lateinit var selectedBusId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map_bus, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_bus) as SupportMapFragment
        mapFragment.getMapAsync(this)

        selectedBusId = arguments!!.getString("id") ?: ""

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnBusFragmentInterActionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnBusFragmentInterActionListener

    companion object {
        fun newInstance(): MapBusFragment = MapBusFragment()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)

        firebaseService.getBus(selectedBusId)
            .doOnNext { onBusCheck(it) }
            .doOnError { errorStatusUtils.show(R.string.error, R.drawable.error) }
            .subscribe()
            .addTo(disposables)
    }

    private fun onBusCheck(bus: Bus) {
        if (bus.active) {
            positionMarker(bus)
        } else {
            map.clear()
            errorStatusUtils.show(R.string.bus_became_inactive, R.drawable.bus)
        }
    }

    private fun positionMarker(bus: Bus) {
        map.clear()

        val position = LatLng(bus.location.latitude, bus.location.longitude)
        map.addMarker(addCustomMarker().position(position))

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(bus.location.latitude, bus.location.longitude),
                CAMERA_ZOOM
            )
        )
    }
}
