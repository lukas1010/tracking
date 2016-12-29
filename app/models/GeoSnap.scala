package models

import play.api.libs.json.{Json, OFormat}

case class GeoSnap(
                    time: Long,
                    id: String,
                    loc: String,
                    speed: Option[String] = None,
                    direction: Option[String] = None)

object GeoSnap {
  val TIME = "time"
  val ID = "id"
  val POSITION = "loc"
  val SPEED = "speed"
  val DIRECTION = "direction"
  implicit val geoSnapFormat: OFormat[GeoSnap] = Json.format[GeoSnap]
}
