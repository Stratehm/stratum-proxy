/**
 * RequireJS i18next Plugin
 * 
 * @version 0.5.0
 * @copyright 2013-2014 Jacob van Mourik
 * @license MIT
 */
define(["i18next"], function(i18next) {
    "use strict";

    var plugin,
        supportedLngs,
        defaultNss,
        resStore = {},
        f = i18next.functions,
        o = i18next.options;

    plugin = {
        version: "0.5.0",
        pluginBuilder: "./i18next-builder",

        /**
         * Checks to see if there exists a resource with the given 
         * resource path, language and namespace in the store. 
         * 
         * @param {String} resPath The resource path
         * @param {String} lng The language
         * @param {String} ns The namespace
         * @returns {Boolean} If the resource exists in the store
         */
        resourceExists: function(resPath, lng, ns) {
            return resStore[resPath] && resStore[resPath][lng] && resStore[resPath][lng][ns];
        },

        /**
         * Adds a resource to the store (overrides existing one).
         * 
         * @param {String} resPath The resource path
         * @param {String} lng The language
         * @param {String} ns The namespace
         * @param {Object} data The resource data
         */
        addResource: function(resPath, lng, ns, data) {
            resStore[resPath] = resStore[resPath] || {};
            resStore[resPath][lng] = resStore[resPath][lng] || {};
            resStore[resPath][lng][ns] = data;
        },

        /**
         * Gets all resources by the given language and namespace.
         * 
         * @param {String} lng The language
         * @param {String} ns The namespace
         * @returns {Object} The resource data
         */
        getResources: function(lng, ns) {
            var data = {};
            f.each(resStore, function(resPath) {
                if (resStore[resPath][lng] && resStore[resPath][lng][ns]) {
                    f.extend(data, resStore[resPath][lng][ns]);
                }
            });
            return data;
        },

        /**
         * Parses a resource name into its component parts. 
         * For example: resource:namespace1,namespace2 where resource is the path to
         * the locales and the part after the : the additional namespace(s) to load.
         * 
         * @param {String} name The resource name
         * @returns {Object} Object containing resource path and namespaces
         */
        parseName: function(name) {
            var splitted = name.split(":");
            return {
                resPath: splitted[0] ? splitted[0] + (/\/$/.test(splitted[0]) ? "" : "/") : "",
                namespaces: splitted[1] ? splitted[1].split(",") : []
            };
        },

        load: function(name, req, onload, config) {
            var options = f.extend({}, config.i18next),
                parsedName = plugin.parseName(name), 
                resPath = parsedName.resPath, 
                supportedLngs, namespaces;

            // Initialize default namespaces
            if (!defaultNss) {
                if (!options.ns || typeof options.ns == "string") {
                    defaultNss = [options.ns || o.ns];
                } else {
                    defaultNss = options.ns.namespaces;
                }
            }

            // Setup namespaces
            namespaces = defaultNss.slice();
            f.each(parsedName.namespaces, function(idx, val) {
                if (namespaces.indexOf(val) == -1) {
                    namespaces.push(val);
                }
            });

            // Setup (scoped) supported languages
            if (options.supportedLngs) {
                supportedLngs = 
                    options.supportedLngs[resPath] || 
                    options.supportedLngs[resPath.replace(/\/$/,'')] || 
                    options.supportedLngs;
            }

            // Set namespaces
            if (typeof o.ns == "string") {
                options.ns = {
                    defaultNs: namespaces[0],
                    namespaces: namespaces
                };
            } else {
                options.ns = f.extend({}, o.ns);
                f.each(namespaces, function(idx, val) {
                    if (options.ns.namespaces.indexOf(val) == -1) {
                        options.ns.namespaces.push(val);
                    }
                });
            }

            // Set a custom load function
            options.customLoad = function(lng, ns, opts, done) {
                var defaultNs = opts.ns.defaultNs,
                    fetch = true;

                // Check if given namespace is requested by current module
                if (namespaces.indexOf(ns) == -1) {
                    fetch = false;
                }
                // Check for already loaded resPath
                else if (plugin.resourceExists(resPath, lng, ns)) {
                    fetch = false;
                }
                // Check for language/namespace support
                else if (supportedLngs && (!supportedLngs[lng] || supportedLngs[lng].indexOf(ns) == -1)) {
                    f.log("no locale support found for " + lng + " with namespace " + ns);
                    fetch = false;
                }

                if (!fetch) {
                    done(null, plugin.getResources(lng, ns));
                    return;
                }

                // Define resource url
                opts = f.extend({}, opts);
                opts.resGetPath = req.toUrl(resPath + opts.resGetPath);

                // Make the request
                i18next.sync._fetchOne(lng, ns, opts, function(err, data) {
                    plugin.addResource(resPath, lng, ns, data);
                    done(err, plugin.getResources(lng, ns));
                });
            };

            // Delete supported languages, they are only needed by this plugin
            delete options.supportedLngs;

            // Initialize i18next and return the i18next instance
            i18next.init(options, function() {
                onload(i18next);
            });
        }
    };

    return plugin;
});
