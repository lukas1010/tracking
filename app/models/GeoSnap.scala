package models

import play.api.libs.json.{Json, OFormat}

case class GeoSnap(
                    time: String,
                    id: String,
                    pos: String,
                    speed: Option[String] = None,
                    direction: Option[String] = None)

object GeoSnap {
  implicit val geoSnapFormat: OFormat[GeoSnap] = Json.format[GeoSnap]
}
