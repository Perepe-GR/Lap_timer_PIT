package com.ejemplo.laptimer.editor

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

object MapboxTileSourceHelper {
    fun getSatelliteSource(): XYTileSource {
        val token = "pk.eyJ1IjoicGVyZXBlLWdybGFwdGltZXIiLCJhIjoiY212enNiMmlqMDVlazJ4czloOXp5cWJxZCJ9.jG35IPtPZDFpP6CriJ43kw"
        return object : XYTileSource(
            "MapboxSatellite", 1, 22, 256, ".png",
            arrayOf("https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/256/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                       MapTileIndex.getX(pMapTileIndex) + "/" +
                       MapTileIndex.getY(pMapTileIndex) +
                       "?access_token=" + token
            }
        }
    }

    fun isSatellite(source: ITileSource?): Boolean = source?.name() == "MapboxSatellite"

    fun toggleTileSource(current: ITileSource?): ITileSource {
        return if (isSatellite(current)) TileSourceFactory.MAPNIK else getSatelliteSource()
    }
}
