package org.fossasia.openevent.general.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_search_location.search
import kotlinx.android.synthetic.main.fragment_search_location.view.locationProgressBar
import kotlinx.android.synthetic.main.fragment_search_location.view.search
import kotlinx.android.synthetic.main.fragment_search_location.view.currentLocation
import org.fossasia.openevent.general.MainActivity
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.utils.Utils
import org.koin.androidx.viewmodel.ext.android.viewModel

const val LOCATION_PERMISSION_REQUEST = 1000

class SearchLocationFragment : Fragment() {
    private lateinit var rootView: View
    private val searchLocationViewModel by viewModel<SearchLocationViewModel>()
    private val geoLocationViewModel by viewModel<GeoLocationViewModel>()
    private var fromSearchFragment: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_search_location, container, false)

        val thisActivity = activity
        if (thisActivity is AppCompatActivity) {
            thisActivity.supportActionBar?.title = ""
            thisActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        setHasOptionsMenu(true)

        fromSearchFragment = arguments?.getBoolean(FROM_SEARCH) ?: false

        geoLocationViewModel.currentLocationVisibility.observe(this, Observer {
            rootView.currentLocation.visibility = View.GONE
        })

        geoLocationViewModel.configure(null)

        rootView.currentLocation.setOnClickListener {
            checkLocationPermission()
            geoLocationViewModel.configure(activity)
            rootView.locationProgressBar.visibility = View.VISIBLE
        }

        geoLocationViewModel.location.observe(this, Observer { location ->
            searchLocationViewModel.saveSearch(location)
            redirectToMain()
        })

        rootView.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchLocationViewModel.saveSearch(query)
                redirectToMain()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        return rootView
    }

    override fun onResume() {
        search.requestFocus()
        Utils.showSoftKeyboard(context, rootView)
        super.onResume()
    }

    private fun checkLocationPermission() {
        val permission =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun redirectToMain() {
        val startMainActivity = Intent(activity, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (fromSearchFragment) {
            val searchBundle = Bundle()
            searchBundle.putBoolean(TO_SEARCH, true)
            startMainActivity.putExtras(searchBundle)
        }
        activity?.startActivity(startMainActivity)
        activity?.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        activity?.finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    geoLocationViewModel.configure(activity)
                } else {
                    Snackbar.make(rootView, "Cannot fetch location!", Snackbar.LENGTH_SHORT).show()
                    rootView.locationProgressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
