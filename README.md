<h2>Middleware for a vehicle tracker</h2>
<p>This is a scala middleware API, built on Play framework and using MongoDB. Receiving endpoints accept timestamped 
geotag snapshots in JSON and store them in a db. Transmitting endpoints return these snapshots based on vehicle id,
 area, and timeframe constraints, either one by one or as a route. Connections are listened to only in HTTPS and
 authorized by preset secret tokens.</p>

<h3>Endpoints</h3>

<p>PUT /api/v1/metadata?token=CYUuyVYOQzgEcl9S — accepts a JSON object in the body of the request, validates its 
structure to be of such format: 
<code>
{
  "ID01":
  {
    [unstuctured objects]
  }
}
</code>, and saves it to the db. The first field name is handled as an asset ID, field's content handled as the assset's
 metadata. If such ID already exists, then the entry is updated in such a way that keys containing <code>null</code> or 
 an empty string are removed and the rest of data is merged. For a new ID an entry in the db is created.
 <br>token — the secret 'write' token stored in configuration file, serving as authentication.</p>

<p>GET /api/v1/metadata?id=ID01&token=NGxYbW0YgxJMVUDe — returns the metadata stored in the db for the requested asset 
ID. 404 returned if no entry with such ID is found.
<br>id — The ID string, defining requested metadata entry.
<br>token — the secret 'read' token stored in configuration file, serving as authentication.</p>

<p>DELETE /api/v1/metadata?id=ID01&token=YlBh962qJCHoLZ6L — removes the metadata stored in the db for the requested 
asset ID.
<br>id — The ID string, defining metadata entry to be removed.
<br>token — the secret 'delete' token stored in configuration file, serving as authentication.</p>

<p>PUT /api/v1/snapshots?id=ID01&token=CYUuyVYOQzgEcl9S — accepts a JSON object in the body of the request, validates 
its structure to be of such format: 
<code>
{
   "[ISO 8601 datetime string]" : {
   "lat" : [WGS84 latitude coordinate],
   "long" : [WGS84 longitude coordinate],
   "speed": [optional number representing km/h],
   "direction": [optional number representing bearing, 0 to 360]
   }
}
</code>, and saves it to the db. The first field name is parsed as a datetime string and the field's value is processed
as a geolocation snapshot. Returns 400 if the data is invalid or 202 if it is validated successfully.
<br>id — The ID string of an asset.
<br>token — the secret 'write' token stored in configuration file, serving as authentication.</p>

<p>GET /api/v1/snapshots?id=ID01&long1=30&lat2=60&long2=50&lat1=50&datetime=2017-01-06T19:57:52.15&seconds=60&token=NGxYbW0YgxJMVUDe —
returns the latest within given timeframe and area snapshots for selected IDs. Ivalid constraints are ignored and all 
but token argument are optional. The returned JSON structure has such format:
<code>
{
  "[asset ID string]": {
    "location": {
      "lat": [WGS84 latitude coordinate],
      "long" : [WGS84 longitude coordinate],
      "speed": [number representing km/h, if present],
      "direction": [optional number representing bearing, if present],
      "timestamp": "[ISO 8601 datetime string]"
    }
  }
}
</code>
<br>id — The ID string, this argument may be present more than once, defining an array of IDs.
<br>lat1 long1 lat2 long2 — Numbers representing two point coordinates, that define (1) northern-western and (2)
southern-eastern corners of a rectangular area from which the results are returned.
<br>time — ISO 8601 datetime string defining the latest point in time for the search. The default value is 'now'.
<br>seconds — Timeframe in seconds for how far back in time to search for snapshots. The default is 60.
<br>token — the secret 'read' token stored in configuration file, serving as authentication.</p>

<p>GET /api/v1/route?id=ID01&long1=30&lat2=60&long2=50&lat1=50&datetime=2017-01-06T19:57:52.15&seconds=60&token=NGxYbW0YgxJMVUDe —
returns the routs within given timeframe for selected IDs and also filtered by area. Ivalid constraints are ignored and 
all but token argument are optional. The returned JSON structure has such format:
<code>
{
  "[asset ID string]": {
    "[ISO 8601 datetime string]": {
      "lat": [WGS84 latitude coordinate],
      "long" : [WGS84 longitude coordinate],
      "speed": [number representing km/h, if present],
      "direction": [optional number representing bearing, if present]
    }
  }
}
</code>
<br>id — The ID string, this argument may be present more than once, defining an array of IDs.
<br>lat1 long1 lat2 long2 — Numbers representing two point coordinates, that define (1) northern-western and (2)
southern-eastern corners of a rectangular area from which the results are returned.
<br>time — ISO 8601 datetime string defining the latest point in time for the search. The default value is 'now'.
<br>seconds — Timeframe in seconds for how far back in time to search for snapshots. The default is 60.
<br>token — the secret 'read' token stored in configuration file, serving as authentication.</p>

<h3>Deployment.</h3>

<p><b>Deployment to a Docker image.</b> Have Typesafe Activator and JDK installed and run this in the project folder: 
<code>activator docker:publishLocal</code> to compile the project and assemble it into a Docker image in the local 
Docker repository.</p>

<p><b>Spinning the Docker image into a container and running it.</b> Have the app's Docker image in a local Docker
repository. Have a MongoDB container running on the Docker machine or start a fresh one with <code>docker run --name 
some-mongo -d mongo</code>, 'some-mongo' being the name of the new container. Now spin up the app's Docker image 
into a container with <code>docker run --link some-mongo:mongo -p 1234:7007 -v /some/dir:/opt/docker/conf -d 
vehicle-tracking:1.0</code>, where '1234' is the port to listen on, 'some-mongo' is the name of a running MongoDB 
container, and '/some/dir' is a local folder already containing 'application.conf' and 'routes' configuration files.</p>

<p><b>Setting secret tokens.</b> Replace these tokens in the 'application.conf' file with your secret strings before
deploying the app's Docker image: <code>
token {
  write = "CYUuyVYOQzgEcl9S"
  read = "NGxYbW0YgxJMVUDe"
  delete = "YlBh962qJCHoLZ6L"
}
</code> .</p>

<p><b>Config files.</b> The 'application.conf' and 'routes' files can be found in the '/conf' folder of the project and
must be placed in the exposed volume of the app's container, as described above.</p> 
