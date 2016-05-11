const requestProxy = require('express-request-proxy')
const path = require('path')

module.exports = (app) => {
  require('./auth')(app)
  require('./holds')(app)
  require('./profile')(app)

  app.all('/services/*', requestProxy({
    url: 'http://services:8005/*'
  }))

  app.get('*', (request, response) => {
    response.sendFile(path.resolve(__dirname, '..', '..', '..', 'public', 'dist', 'index.html'))
  })
}