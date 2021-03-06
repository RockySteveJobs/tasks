package org.tasks.gtasks

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.tasks.R
import org.tasks.data.LocationDao
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class PlayServices @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val locationDao: LocationDao) {

    fun check(activity: Activity?): Disposable {
        return Single.fromCallable(locationDao::geofenceCount)
                .map { it == 0 || refreshAndCheck() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { success: Boolean ->
                    if (!success && !preferences.getBoolean(R.string.warned_play_services, false)) {
                        preferences.setBoolean(R.string.warned_play_services, true)
                        resolve(activity)
                    }
                }
    }

    fun refreshAndCheck(): Boolean {
        refresh()
        return isPlayServicesAvailable
    }

    val isPlayServicesAvailable: Boolean
        get() = result == ConnectionResult.SUCCESS

    private fun refresh() {
        val instance = GoogleApiAvailability.getInstance()
        val googlePlayServicesAvailable = instance.isGooglePlayServicesAvailable(context)
        preferences.setInt(R.string.play_services_available, googlePlayServicesAvailable)
        if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
            preferences.setBoolean(R.string.warned_play_services, false)
        }
        Timber.d(status)
    }

    fun resolve(activity: Activity?) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val error = preferences.getInt(R.string.play_services_available, -1)
        if (googleApiAvailability.isUserResolvableError(error)) {
            googleApiAvailability.getErrorDialog(activity, error, REQUEST_RESOLUTION).show()
        } else {
            Toast.makeText(activity, status, Toast.LENGTH_LONG).show()
        }
    }

    private val status: String
        get() = GoogleApiAvailability.getInstance().getErrorString(result)

    private val result: Int
        get() = preferences.getInt(R.string.play_services_available, -1)

    companion object {
        private const val REQUEST_RESOLUTION = 10000
    }
}