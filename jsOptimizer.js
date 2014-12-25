({
    appDir: "${basedir}/src/main/resources/webapp",
    baseUrl: "js",
    dir: "${project.build.directory}/webapp-optimizer",
    mainConfigFile: "${basedir}/src/main/resources/webapp/js/main.js",
    modules: [{
	name: "main"
    }]
})