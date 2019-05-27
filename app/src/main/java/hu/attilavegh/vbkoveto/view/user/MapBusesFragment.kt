package hu.attilavegh.vbkoveto.view.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import hu.attilavegh.vbkoveto.R
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import hu.attilavegh.vbkoveto.model.Bus
import hu.attilavegh.vbkoveto.view.MapFragmentBase
import io.reactivex.rxkotlin.addTo

class MapBusesFragment : MapFragmentBase(), GoogleMap.OnMarkerClickListener {

    private var listener: OnBusesFragmentInteractionListener? = null

    private lateinit var labelContainer: FrameLayout
    private lateinit var busNameLabel: TextView
    private lateinit var busDepartureLabel: TextView

    private var buses = listOf<Bus>()
    private var markers = arrayListOf<Marker>()
    private var selectedBus = Bus()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map_buses, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_buses) as SupportMapFragment
        mapFragment.getMapAsync(this)

        labelContainer = view.findViewById(R.id.map_buses_label_container)
        busNameLabel = view.findViewById(R.id.map_buses_name)
        busDepartureLabel = view.findViewById(R.id.map_buses_departure)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnBusesFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    interface OnBusesFragmentInteractionListener

    companion object {
        fun newInstance(): MapBusesFragment = MapBusesFragment()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener {
            selectedBus = Bus()
            labelContainer.visibility = View.INVISIBLE
            resetMarkerIcons()
        }

        firebaseDataService.getBusList(context!!).subscribe({
            handleBuses(it)
        }, {
            errorStatusUtils.show(R.string.error, R.drawable.error)
        }).addTo(disposables)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        selectedBus = buses[marker.tag as Int]

        labelContainer.visibility = View.VISIBLE
        busNameLabel.text = selectedBus.name
        busDepartureLabel.text = selectedBus.getFormattedDepartureTime()

        resetMarkerIcons()
        marker.setIcon(createCustomMarker(R.drawable.marker_selected))

        return true
    }

    private fun handleBuses(result: List<Bus>) {
        buses = result.filter { bus -> bus.active }

        if (!buses.isEmpty()) {
            showBuses(buses)
        } else {
            clearMap()
            onNoBus()
        }

        if (!buses.map { bus -> bus.id }.contains(selectedBus.id)) {
            labelContainer.visibility = View.INVISIBLE
        }
    }

    private fun onNoBus() {
        errorStatusUtils.show(R.string.no_bus, R.drawable.bus)
    }

    private fun showBuses(buses: List<Bus>) {
        clearMap()

        if (!buses.isEmpty()) {
            when (buses.size) {
                1 -> createMarker(buses[0])
                else -> createMarkers(buses)
            }
        }
    }

    private fun createMarker(bus: Bus) {
        val position = LatLng(bus.location.latitude, bus.location.longitude)
        addMarker(bus, 0, position)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, cameraZoom))
    }

    private fun createMarkers(buses: List<Bus>) {
        val boundsBuilder = LatLngBounds.builder()

        buses.forEachIndexed { index, bus ->
            val position = LatLng(bus.location.latitude, bus.location.longitude)
            boundsBuilder.include(position)

            addMarker(bus, index, position)
        }

        map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), cameraBoundPadding))
    }

    private fun addMarker(bus: Bus, index: Int, position: LatLng): Marker {
        val marker = if (bus.id == selectedBus.id) {
            map.addMarker(addCustomMarker(R.drawable.marker_selected).position(position))
        } else {
            map.addMarker(addCustomMarker().position(position))
        }

        marker.tag = index
        markers.add(marker)

        return marker
    }

    private fun resetMarkerIcons() {
        markers.forEach { it.setIcon(createCustomMarker()) }
    }

    private fun clearMap() {
        map.clear()
        markers.clear()
    }
}
