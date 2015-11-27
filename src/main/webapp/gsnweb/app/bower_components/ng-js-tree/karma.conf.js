module.exports = function(config) {

    config.set({

        basePath: 'dist/',

        frameworks: ['jasmine'],

        // list of files / patterns to load in the browser
        files: [
            '../bower_components/jquery/dist/jquery.min.js',
            '../bower_components/angular/angular.min.js',
            '../bower_components/angular-mocks/angular-mocks.js',
            '../bower_components/jstree/dist/jstree.min.js',
            'ngJsTree.js',
            '../test/**/*.js'
        ],

        reporters: ['coverage'],

        preprocessors: {
            "ngJsTree.js": ['coverage']
        },
        coverageReporter: {
            type: "lcov",
            dir: "coverage/"
        },

        plugins: [
            'karma-coverage',
            'karma-jasmine',
            'karma-phantomjs-launcher'
        ],

        // web server port
        port: 9876,


        // enable / disable colors in the output (reporters and logs)
        colors: true,

        logLevel: config.LOG_INFO,

        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: false,

        // Start these browsers
        browsers: ['PhantomJS'],

        // If browser does not capture in given timeout [ms], kill it
        captureTimeout: 10000,

        // Continuous Integration mode
        // if true, it capture browsers, run tests and exit
        singleRun: true
    });
};