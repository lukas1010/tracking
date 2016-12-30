<h2>Middleware for a vehicle tracker</h2>
<p>This is a scala middleware API, built on Play framework and MongoDB. Receiving end accepts JSON timestamped 
snapshots with a vehicle geotag and stores them in a DB. Sending end returns same snapshots based on vehicle id and 
a timeframe, either one by one or as a route.

<p>POST /api/snapshots — reads a json object in the body of a request, checks its structure to correspond to predefined 
one like {”time” : ”(timestamp_number_in_milliseconds)”, “id” : ”some_name_string”, ”loc” : ”coordinates_string”, 
“speed” : “optional_speed_string”, “direction” : “optionsl_direction_string”} , and saves it to the db. the last two 
keys are optional and may be absent. response is 202 with validated json in the body or 400 if the snapshot in request 
was of incorrect format.</p>
<p>GET /api/snapshot/:id?time=24553663&window=1 — returns 200 with the nearest to the specified time snapshot for the 
specified id, searching in the timeframe of +/- half the specified window in seconds, in the same json format in the 
body of the response. the last ‘window’ argument is optional and defaults to 1.</p>
<p>GET /api/routes/:id?start=24628457&finish=2458742 — returns 200 with all the snapshots for the specified id, of 
timestamps between start and finish, as a json array in the body of the response. returns 404 if no snapshots were 
found.</p>
