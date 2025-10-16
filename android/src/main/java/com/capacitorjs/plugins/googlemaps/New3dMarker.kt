package com.capacitorjs.plugins.googlemaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class New3dMarker(private val context: Context) {

    fun getMarkerIcon(iconUrl: String): BitmapDescriptor {
        val resourceId = getResourceIdFromIconUrl(iconUrl)

        if (resourceId != 0) {
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            val scaledBitmap = getScaledBitmap(bitmap, iconUrl)
            return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        }

        // Fallback to default marker if resource not found
        return BitmapDescriptorFactory.defaultMarker()
    }

    private fun getResourceIdFromIconUrl(iconUrl: String): Int {
        return context.resources.getIdentifier(iconUrl, "drawable", context.packageName)
    }

    private fun getScaledBitmap(bitmap: Bitmap, iconUrl: String): Bitmap {
        val new3DHeight = context.resources.getDimension(R.dimen.new_3d_marker_height).toInt()
        val new3DWidth = context.resources.getDimension(R.dimen.new_3d_marker_width).toInt()

        return Bitmap.createScaledBitmap(bitmap, new3DWidth, new3DHeight, false)
    }

    // Helper method to check if an icon URL is a 3D marker
    fun is3dMarker(iconUrl: String?): Boolean {
        return iconUrl?.contains("new_3d_marker") == true ||
                iconUrl?.contains("new_3d_image") == true
    }
}