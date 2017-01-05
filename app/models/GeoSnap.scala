package models

import play.api.libs.json.{Json, OFormat}

case class GeoSnap(
                    lat: Double,
                    long: Double,
                    var speed: Option[Double] = None,
                    var direction: Option[Double] = None,
                    timestamp: Option[String]) {
  assert(lat >= -180 && lat <= 180 && long >= -90 && long <= 90, "Invalid coordinates.")
  speed match {
    case Some(x) => if (x < 0) speed = None
    case _ =>
  }
  direction match {
    case Some(x) => if (x < 0) direction = None else if (x > 360) direction = None
    case _ =>
  }
}

object GeoSnap {
  val TIME = "timestamp"
  val LATITUDE = "lat"
  val LONGITUDE = "long"
  val SPEED = "speed"
  val DIRECTION = "direction"
  implicit val geoSnapFormat: OFormat[GeoSnap] = Json.format[GeoSnap]
}
