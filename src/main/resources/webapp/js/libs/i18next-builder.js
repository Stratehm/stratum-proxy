/**
 * RequireJS i18next Plugin (builder)
 * 
 * @version 0.5.0
 * @copyright 2013-2014 Jacob van Mourik
 * @license MIT
 */
(function() {
    "use strict";

    var data, buildConfig;

    /**
     * Deeply extends an object with another object
     * 
     * @param {Object} obj The object to extend
     * @param {Object} src The object to extend with
     * @returns {Object} The extended object
     */
    function extend(obj, src) {
        obj = obj || {};
        for (var prop in src) {
            if (!src.hasOwnProperty(prop)) continue;
            if (Object.prototype.toString.call(src[prop]) === '[object Object]') {
                obj[prop] = extend(obj[prop], src[prop]);
            } else {
                obj[prop] = src[prop];
            }
        }
        return obj;
    }

    /**
     * Synchronously loads the contents of a file using either nodejs or rhino.
     * 
     * @param {String} path The path of the file
     * @returns {String} The contents of the file
     */
    function loadFile(path) {
        var file, stringBuffer, line, lineSeparator, input;
        if (typeof process !== "undefined" && process.versions && !!process.versions.node && !process.versions["node-webkit"]) {
            file = fs.readFileSync(path, "utf8");
            if (file.indexOf("\uFEFF") === 0) {
                return file.substring(1);
            }
            return file;
        } else {
            file = new java.io.File(path);
            lineSeparator = java.lang.System.getProperty("line.separator");
            input = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), "utf-8"));
            try {
                stringBuffer = new java.lang.StringBuffer();
                line = input.readLine();
                if (line && line.length() && line.charAt(0) === 0xfeff) {
                    line = line.substring(1);
                }
                stringBuffer.append(line);
                while ((line = input.readLine()) !== null) {
                    stringBuffer.append(lineSeparator).append(line);
                }
                return String(stringBuffer.toString());
            } finally {
                input.close();
            }
        }
    }

    /**
     * Parses a resource name into its component parts. 
     * For example: resource:namespace1,namespace2 where resource is the path to
     * the locales and the part after the : the additional namespace(s) to load.
     * 
     * @param {String} name The resource name
     * @returns {Object} Object containing resource path and namespaces
     */
    function parseName(name) {
        var splitted = name.split(":");
        return {
            resPath: splitted[0] ? splitted[0] + (/\/$/.test(splitted[0]) ? "" : "/") : "",
            namespaces: splitted[1] ? splitted[1].split(",") : []
        };
    }

    define({
        load: function(name, req, onload, config) {
            var parsedName = parseName(name),
                resPath = parsedName.resPath,
                options, supportedLngs, namespaces, url, content;

            // Skip the process if i18next inlining is disabled
            if (!config.inlineI18next) {
                return onload();
            }
            // Skip the process if i18next config is not defined
            if (!config.i18next) {
                console.log("Skipping i18next inlining, could not find i18next config.");
                return onload();
            }
            // Skip the process if supportedLngs is not defined in i18next config
            if (!config.i18next.supportedLngs) {
                console.log("Skipping i18next inlining, could not find supportedLngs option in i18next config.");
                return onload();
            }

            // Setup build config and data
            if (!buildConfig) {
                buildConfig = config;
                data = extend({}, config.i18next);
                delete data.supportedLngs;
            }

            // Setup options
            options = extend({
                ns: "translation",
                resGetPath: "locales/__lng__/__ns__.json",
                interpolationPrefix: "__",
                interpolationSuffix: "__"
            }, config.i18next);

            // Setup namespaces
            namespaces = typeof options.ns == "string" ? [options.ns] : options.ns.namespaces;
            parsedName.namespaces.forEach(function(ns) {
                if (namespaces.indexOf(ns) == -1) {
                    namespaces.push(ns);
                }
            });

            // Setup (scoped) supported languages
            supportedLngs = options.supportedLngs[resPath] || 
                    options.supportedLngs[resPath.replace(/\/$/,'')] || 
                    options.supportedLngs;

            // Load all needed resources
            data.resStore = data.resStore || {};
            Object.keys(supportedLngs).forEach(function(lng) { 
                data.resStore[lng] = data.resStore[lng] || {};
                supportedLngs[lng].forEach(function(ns) {
                    if (namespaces.indexOf(ns) !== -1) {
                        data.resStore[lng][ns] = data.resStore[lng][ns] || {};
                        url = req.toUrl(resPath + options.resGetPath
                                .replace(options.interpolationPrefix + "ns" + options.interpolationSuffix, ns)
                                .replace(options.interpolationPrefix + "lng" + options.interpolationSuffix, lng));
                        content = JSON.parse(loadFile(url));
                        extend(data.resStore[lng][ns], content);
                    }
                });
            });

            onload();
        },

        write: function(pluginName, moduleName, write) {
            if (!buildConfig) return;

            // For each module initialize i18next with the globally defined config
            write("define('"+ pluginName + "!" + moduleName + "',['i18next'],function(i18n){i18n.init(window._i18n);return i18n;});\n");
        },

        onLayerEnd: function(write) {
            if (!buildConfig) return;

            // Save the config in a globally defined variable which modules can use to initialize i18next
            if (buildConfig.modules.length > 1) {
                // For multiple module builds extend the global config with any previously defined global config
                write("window._i18n = (" + extend.toString() + ")(window._i18n," + JSON.stringify(data) + ");\n");
            } else {
                // For single module builds just define the global config
                write("window._i18n = " + JSON.stringify(data) + ";\n");
            }
        }
    });
})();