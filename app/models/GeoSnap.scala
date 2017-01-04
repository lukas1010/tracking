package models

import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

case class GeoSnap(
                    id: String,
                    time: String,
                    var millis: Option[Long],
                    lat: Double,
                    lng: Double,
                    var speed: Option[Double] = None,
                    var direction: Option[Double] = None) {
  assert(lat >= -180 && lat <= 180 && lng >= -90 && lng <= 90, "Invalid coordinates.")
  millis = Some(DateTime parse time getMillis)
  speed match {
    case Some(x) => if (x < 0) speed = Some(0.0)
    case _ =>
  }
  direction match {
    case Some(x) => if (x < 0) direction = Some(0.0) else if (x > 360) direction = Some(360.0)
    case _ =>
  }
}

object GeoSnap {
  val ID = "id"
  val TIME = "time"
  val MILLIS = "millis"
  val LATITUDE = "lat"
  val LONGITUDE = "lng"
  val SPEED = "speed"
  val DIRECTION = "direction"
  implicit val geoSnapFormat: OFormat[GeoSnap] = Json.format[GeoSnap]
}
