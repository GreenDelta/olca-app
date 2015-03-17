var gulp = require('gulp');
var del = require('del');
var runSequence = require('run-sequence');
var fileinclude = require('gulp-file-include');
var zip = require('gulp-zip');
var concat = require('gulp-concat')
var stylus = require('gulp-stylus')
var nib = require('nib')
var jade = require('gulp-jade')
var jadeClient = require('gulp-clientjade')
var coffee = require('gulp-coffee')

gulp.task('default', [], function(callback) {
	runSequence('libs', 'pages', 'precompilePluginManager', 'pluginManager', callback);
});
// compile the html pages and copy them to the build directory
gulp.task('pages', function() {
	gulp.src('src/report_view/report_view.html')
			.pipe(fileinclude({
				prefix: '//@',
				basepath: '@file'
			}))
			.pipe(gulp.dest('build'));
	gulp.src('src/start_page/*.html')
			.pipe(fileinclude({
				prefix: '//@',
				basepath: '@file'
			}))
			.pipe(gulp.dest('build'));	
	gulp.src('src/bubble_chart/bubble_chart.html').pipe(gulp.dest('build'));
	gulp.src('src/*.html').pipe(gulp.dest('build'));
	gulp.src('src/devtools/*').pipe(gulp.dest('build'));
	gulp.src('images/*').pipe(gulp.dest('build/images'));
});

gulp.task('precompilePluginManager', function() {
	gulp.src('src/plugin_manager/coffeescript/*.coffee')
		.pipe(coffee({
			bare: true
		}))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
	gulp.src('src/plugin_manager/jade/templates/*.jade')
		.pipe(jadeClient('templates.js'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
	gulp.src('src/plugin_manager/stylus/*.styl')
		.pipe(stylus({
			use: [nib()]
		}))
		.pipe(concat('main.css'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('pluginManager', function() {
	gulp.src('src/plugin_manager/jade/plugin_manager.jade')
		.pipe(jade({
			locals: {}
		}))
		.pipe(gulp.dest('build'));
});

// copy the libraries to the build/lib folder
gulp.task('libs', function() {
	
	// min-files
	gulp.src(['./bower_components/angular/angular.min.js',
		'./bower_components/angular-sanitize/angular-sanitize.min.js',
		'./bower_components/codemirror/lib/*',
		'./bower_components/codemirror/mode/javascript/javascript.js',
		'./bower_components/codemirror/mode/python/python.js',
		'./bower_components/d3/d3.min.js',
		'./bower_components/jquery/dist/jquery.min.js'
	]).pipe(gulp.dest('./build/libs'));
	
	// bootstrap folders
	gulp.src(['./bower_components/bootstrap/dist/**'])
			.pipe(gulp.dest('./build/libs/bootstrap'));
	
	// other libs
	gulp.src(['./other_libs/*.js']).pipe(gulp.dest("./build/libs"));
	
});

gulp.task('clean', function(cb) {
	del(['build/*'], cb);
});

gulp.task('zip', function() {
	return gulp.src('build/**')
        .pipe(zip('base_html.zip'))
        .pipe(gulp.dest('dist'));	
});