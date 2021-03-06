var express = require('express')
var path = require('path')
var logger = require('morgan')
var axios = require('axios')
var compileSass = require('express-compile-sass')
var app = express()
var requestProxy = require('express-http-proxy')
require('./workflow_config')(app)
require('./random_test_data')(app)
var babelify = require('express-babelify-middleware')
var url = require('url')
require('ejs')

if (app.get('env') === 'development') {
  var livereload = require('express-livereload')
  livereload(app, {})

  app.use(require('connect-livereload')({
    port: 35729
  }))
}
var Server

app.use(logger('dev'))
app.use(express.static(path.join(__dirname, '/../public')))

app.set('views', './views')
app.set('view-engine', 'ejs')

app.get('/js/bundle.js',
  babelify([{ './client/src/bootstrap': { run: true } }])
)

app.get('/js/bundle_for_old.js',
  babelify([{ './client/src/bootstrap_old': {run: true} }]))

app.get('/css/vendor/:cssFile', function (request, response) {
  response.sendFile(request.params.cssFile, { root: path.resolve(path.join(__dirname, '/../node_modules/select2/dist/css/')) })
})

function newResource (type) {
  return axios.post(process.env.SERVICES_PORT + '/' + type, {}, {
    headers: {
      Accept: 'application/ld+json',
      'Content-Type': 'application/ld+json'
    }
  })
    .catch(function (response) {
      if (response instanceof Error) {
        // Something happened in setting up the request that triggered an Error
        console.log('Error', response.message)
      } else {
        // The request was made, but the server responded with a status code
        // that falls out of the range of 2xx
        console.log(response.data)
        console.log(response.status)
        console.log(response.headers)
        console.log(response.config)
      }
    })
}

app.get('/', function (req, res) {
  res.redirect('/cataloguing')
})

app.get('/cataloguing_old/*', function (req, res, next) {
  res.sendFile('main_old.html', { title: 'Katalogisering', root: path.join(__dirname, '/../public/') })
})

app.get('/cataloguing', function (req, res, next) {
  res.sendFile('main.html', { title: 'Katalogisering', root: path.join(__dirname, '/../public/') })
})

app.get('/:type(person|work|publication|place|serial|corporation|subject|genre)', function (req, res, next) {
  newResource(req.params.type).then(function (response) {
    res.redirect('/cataloguing_old/' + req.params.type + '?resource=' + response.headers.location)
  })
})

app.get('/version', function (request, response) {
  response.json({ 'buildTag': process.env.BUILD_TAG, 'gitref': process.env.GITREF })
})

app.get('/valueSuggestions/demo_:source/:isbn', function (req, res, next) {
  res.sendFile(`${req.params.source}_${req.params.isbn}.json`, { root: path.join(__dirname, '/../public/example_data') })
})

var services = (process.env.SERVICES_PORT || 'http://services:8005').replace(/^tcp:\//, 'http:/')

app.use('/services', requestProxy(services, {
  forwardPath: function (req, res) {
    return url.parse(req.url).path
  }
}))

app.get('/valueSuggestions/:source/:type/:id', requestProxy(services, {
  forwardPath: function (req, res) {
    var reqUrl = services + '/datasource/' + req.params.source + '/' + req.params.type + '/' + req.params.id.replace(/-/g, '')
    console.log(reqUrl)
    return url.parse(reqUrl).path
  }
}))

app.use('/style', compileSass({
  root: path.join(__dirname, '/../client/scss'),
  sourceMap: true, // Includes Base64 encoded source maps in output css
  sourceComments: false, // Includes source comments in output css
  watchFiles: true, // Watches sass files and updates mtime on main files for each change
  logToConsole: true // If true, will log to console.error on errors
}))

// catch 404 and forward to error handler
app.use(function (req, res, next) {
  var err = new Error('Not Found')
  err.status = 404
  next(err)
})

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use(function (err, req, res, next) {
    console.error(err.message)
    if (res.headersSent) {
      return next(err)
    }
    res.status(500)
    res.render('error', { error: err })
  })
}

// production error handler
// no stacktraces leaked to user
app.use(function (err, req, res, next) {
  console.error(err.message)
  if (res.headersSent) {
    return next(err)
  }
  res.status(500)
  res.render('error', { error: err })
})

Server = app.listen(process.env.BIND_PORT || 8010, process.env.BIND_IP, function () {
  var host = Server.address().address
  var port = Server.address().port
  console.log('Catalinker server listening at http://%s:%s', host, port)
})

module.exports = app
