
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.capacitorjs.plugins.googlemaps.BusesMarkerInfoWindow
import com.capacitorjs.plugins.googlemaps.CapacitorGoogleMapMarker
import com.capacitorjs.plugins.googlemaps.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer


class BusesMarkerRenderer(
        private val context: Context,
        private val map: GoogleMap,
        clusterManager: ClusterManager<CapacitorGoogleMapMarker>
) : DefaultClusterRenderer<CapacitorGoogleMapMarker>(context, map, clusterManager) {

    private val markerToItemMap = HashMap<Marker, CapacitorGoogleMapMarker>()
    private val clusterColor = Color.parseColor("#FE7C00")


    override fun onBeforeClusterItemRendered(item: CapacitorGoogleMapMarker, markerOptions: MarkerOptions) {
        val busesMarker = BusesMarker(context)
//        Log.d("BusesMarkerRenderer", "onBeforeClusterItemRendered ${item.iconUrl} ${item.title}")
        markerOptions.icon(item.iconUrl?.let { busesMarker.getMarkerIcon(item.title, it) })
        if(!item.infoIcon.equals("not_show_info_window")) {
            map?.setInfoWindowAdapter(BusesMarkerInfoWindow(context))
        }
    }

    override fun onClusterItemUpdated(item: CapacitorGoogleMapMarker, marker: Marker) {
//        Log.d("BusesMarkerRenderer", "onClusterItemUpdated ${item.iconUrl} ${item.title} {${item.infoData}}")

        marker.tag = item

        if (item.infoData?.getBoolean("showInfoIcon") == true) {
            marker.showInfoWindow()
        } else {
            marker.hideInfoWindow()
        }

        val busesMarker = BusesMarker(context)
        val newIcon = item.iconUrl?.let { busesMarker.getMarkerIcon(item.title, it) }
        marker?.setIcon(newIcon)
    }

    override fun getColor(clusterSize: Int): Int {
        // Return your specific color for all cluster icons
        return clusterColor
    }

}

class BusesMarker(private val context: Context) {
    private val customMarkerView = LayoutInflater.from(context).inflate(R.layout.bus_number_marker, null)
    private val textView: TextView = customMarkerView.findViewById(R.id.busNumberMarkerText)
    private val cardView: CardView = customMarkerView.findViewById(R.id.busNumberMarkerCardView)
    private val imageView: ImageView = customMarkerView.findViewById(R.id.busNumberMarkerImage)
    private val busAlertParent: LinearLayout = customMarkerView.findViewById(R.id.busAlertParent)

    fun getMarkerIcon(text: String, iconUrl: String): BitmapDescriptor {
        textView.text = text
        if (!iconUrl.isNullOrEmpty()) {
            if (iconUrl.contains("grey", ignoreCase = true)) {
                cardView.setCardBackgroundColor(Color.parseColor("#808080"))
                imageView.setImageResource(R.drawable.not_live_status)
                busAlertParent.setBackgroundResource(R.drawable.bg_shadow_grey_25)
            } else if(iconUrl.contains("red", ignoreCase = true)) {
                busAlertParent.setBackgroundResource(R.drawable.bg_shadow_red_25)
                cardView.setCardBackgroundColor(Color.parseColor("#c62828"))
                imageView.setImageResource(R.drawable.alert_bus_inactive_red)
            } else {
                busAlertParent.setBackgroundResource(R.drawable.bg_shadow_blue_25)
                cardView.setCardBackgroundColor(Color.parseColor("#2196f3"))
            }
            if (iconUrl.contains("selected", ignoreCase = true)) {
                // Apply a thick white border
                val borderSize = 10f
                val strokeColor = Color.WHITE
                var fillColor = Color.parseColor("#2196f3")
                busAlertParent.setBackgroundResource(R.drawable.bg_shadow_white_25)
                if(iconUrl.contains("grey", ignoreCase = true)) {
                    fillColor = Color.parseColor("#808080")
                } else if(iconUrl.contains("red", ignoreCase = true)) {
                    fillColor = Color.parseColor("#c62828")
                }
                val cornerRadius = 75f

                // Create a GradientDrawable with a stroke and fill color
                val backgroundDrawable = GradientDrawable()
                backgroundDrawable.setStroke(borderSize.toInt(), strokeColor)
                backgroundDrawable.setColor(fillColor)
                backgroundDrawable.cornerRadius = cornerRadius

                // Set the drawable as the background for the CardView
                cardView.background = backgroundDrawable
                cardView.cardElevation = 8f
            }
        }
        return getMarkerIconFromView(customMarkerView)
    }

    private fun getMarkerIconFromView(view: android.view.View): BitmapDescriptor {
        view.measure(android.view.View.MeasureSpec.UNSPECIFIED, android.view.View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.buildDrawingCache()
        val returnedBitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight,
                Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        view.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(returnedBitmap)
    }
}
