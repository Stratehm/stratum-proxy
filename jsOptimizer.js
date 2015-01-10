({
    appDir: "${basedir}/src/main/resources/webapp",
    baseUrl: "js",
    dir: "${project.build.directory}/webapp-optimizer",
    mainConfigFile: "${basedir}/src/main/resources/webapp/js/main.js",
    modules: [{
	name: "main",
	include: ['controllers/abstractPageController', 'controllers/poolsPage', 'controllers/usersPage', 'controllers/connectionsPage', 'controllers/settingsPage', 'controllers/logsPage']
    }],
    optimizeCss: "standard",
    optimize: "uglify2",
    generateSourceMaps: true,
    preserveLicenseComments: false
})