var express = require('express');
var mysql = require('mysql');
var app = express();
var bodyParser = require('body-parser');
var jsonParser = bodyParser.json();

require('dotenv').config()

var connection = mysql.createConnection({
  host     : process.env.RDS_HOSTNAME,
  user     : process.env.RDS_USERNAME,
  password : process.env.RDS_PASSWORD,
  port     : process.env.RDS_PORT,
  database : process.env.RDS_DB
});

app.set('port', (process.env.PORT || 5000));

app.use(express.static(__dirname + '/public'));

// views is directory for all template files
app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');

app.get('/', function(request, response) {
	console.log(process.env.RDS_HOSTNAME);

  	response.render('pages/index');
});

connection.connect(function(err) {
  if (err) {
    console.error('error connecting: ' + err.stack);
    return;
  }

  console.log('connected as id ' + connection.threadId);
});

app.get('/getproduct', function(request, response) {
		connection.query('Select * from products where id = 0', function(err, results) {
			if(err) throw err;
			console.log(results);
			response.send(results);
		});
});

//just updating demo unit for now, in the future we will update it by id
//this portion requires the service to allow me to change items on the slots
app.post('/updateproduct', jsonParser, function(request, response) {
    if (!request.body) return response.sendStatus(400);

	connection.query('UPDATE products SET Product1 = ?, Product2 = ?, Product1Max= ?, Product2Max = ?, Product1Name = ?, Product2Name =? WHERE id = 0',
	[request.body.product1, request.body.product2, request.body.product1max, request.body.product2max, request.body.product1name, request.body.product2name], function(err, results) {
	    if(err) throw err;
		console.log("success");

	});

  	response.sendStatus(200);
});

//just updating demo unit for now, in the future we will update it by id
app.post('/updatevalue', jsonParser, function(request, response) {
    if (!request.body) return response.sendStatus(400);
	connection.query('UPDATE products SET Product1weight = ?, Product2weight = ? WHERE id = 0',
		[request.body.product1weight, request.body.product2weight], function(err, results) {
	                	if(err) throw err;
	                	console.log("success");
	                });
  	response.sendStatus(200);
});


app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});


