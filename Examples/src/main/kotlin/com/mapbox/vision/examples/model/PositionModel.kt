package com.mapbox.vision.examples.model

/**
 * @author shenfei.wang@g42.ai
 * @description
 * @createtime 11/15/2021 11:39
 */
class PositionModel() {
    var title: String? = null
    var subTitle: String? = null
    var lat = 0.0
    var lng = 0.0
   constructor(lat: Double, lng: Double) : this() {
       this.lat = lat
       this.lng = lng
   }
}