<h2>Middleware for a vehicle tracker</h2>
<p>This is a scala middleware API, built on Play framework and MongoDB. Receiving end accepts JSON timestamped 
snapshots with a vehicle geotag and stores them in a DB. Sending end returns same snapshots based on vehicle id and 
a timeframe, either one by one or as a route.</p>

<p>POST /api/snapshots — reads a JSON object in the body of a request, validates its structure to be of this format: 
<code>{ “id” : ”some_name_string”, ”time” : ”[timestamp_iso_string]”, ”lat” : [latitude_EPSG:4326],
"lng" : [longitude_EPSG:4326], “speed” : [optional_speed_number], “direction” : [optional_direction_string] }</code> , 
and saves it to the db. The last two keys are optional and may be absent. Response is 202 with validated json in the 
body or 400 if the data in request was of incorrect format.</p>
<p>GET /api/snapshot/[id_string]?time=[ISO_time_string]&window=[number_of_seconds] — returns 200 with the nearest to 
the specified time snapshot for the specified id, searching in the timeframe of +/- half the specified window in 
seconds, in the same json format in the body of the response. The last ‘window’ argument is optional and defaults to 
1.</p>
<p>GET /api/routes/[ID_string]?start=[ISO_time_string]&finish=[ISO_time_string] — returns 200 with all the snapshots 
for the specified id, of timestamps between start and finish, as a json array in the body of the response. returns 404 
if no snapshots were found.</p>
