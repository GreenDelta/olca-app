var gulp = require('gulp');
var clean = require('gulp-clean');
var runSequence = require('run-sequence');
var fileinclude = require('gulp-file-include');
var zip = require('gulp-zip');
var concat = require('gulp-concat');
var stylus = require('gulp-stylus');
var nib = require('nib');
var jade = require('gulp-jade');
var jadeClient = require('gulp-clientjade');
var coffee = require('gulp-coffee');

gulp.task('default', function() {
	runSequence('clean', 'build', 'zip');
});
gulp.task('build', ['pages', 'resources', 'libs']);
gulp.task('pages', ['report_view', 'start_page', 'bubble_chart', 'devtools', 'plugin_manager', 'other_pages']);
gulp.task('resources', ['images']);
gulp.task('libs', ['bower_components', 'bootstrap', 'other_libs']);

gulp.task('clean', function() {
	return gulp.src(['build', 'dist', 'src/plugin_manager/precompiled'], {read: false})
		.pipe(clean());
});

gulp.task('report_view', function() {
	return gulp.src('src/report_view/report_view.html')
		.pipe(fileinclude({
			prefix: '//@',
			basepath: '@file'
		}))
		.pipe(gulp.dest('build'));
});

gulp.task('start_page', function() {
	return gulp.src('src/start_page/*.html')
		.pipe(fileinclude({
			prefix: '//@',
			basepath: '@file'
		}))
		.pipe(gulp.dest('build'));	
});

gulp.task('bubble_chart', function() {
	return gulp.src('src/bubble_chart/bubble_chart.html')
		.pipe(gulp.dest('build'));
});

gulp.task('devtools', function() {
	return gulp.src('src/devtools/*')
		.pipe(gulp.dest('build'));
});

gulp.task('other_pages', function() {
	return gulp.src('src/*.html')
		.pipe(gulp.dest('build'));
});

gulp.task('images', function() {
	return gulp.src('images/*')
		.pipe(gulp.dest('build/images'));
});

gulp.task('bower_components', function() {
	return gulp.src(['bower_components/angular/angular.min.js',
		'bower_components/angular-sanitize/angular-sanitize.min.js',
		'bower_components/codemirror/lib/*',
		'bower_components/codemirror/mode/javascript/javascript.js',
		'bower_components/codemirror/mode/python/python.js',
		'bower_components/d3/d3.min.js',
		'bower_components/jquery/dist/jquery.min.js'
	]).pipe(gulp.dest('build/libs'));
});

gulp.task('bootstrap', function() {
	return gulp.src(['bower_components/bootstrap/dist/**'])
		.pipe(gulp.dest('build/libs/bootstrap'));
});

gulp.task('other_libs', function() {
	return gulp.src(['other_libs/*.js'])
		.pipe(gulp.dest('build/libs'));
});

gulp.task('plugin_manager', ['plugin_manager_scripts', 'plugin_manager_templates', 'plugin_manager_styles'], function() {
	return gulp.src('src/plugin_manager/jade/plugin_manager.jade')
		.pipe(jade({
			locals: {}
		}))
		.pipe(gulp.dest('build'));
});

gulp.task('plugin_manager_scripts', function() {
	return gulp.src('src/plugin_manager/coffeescript/*.coffee')
		.pipe(coffee({
			bare: true
		}))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('plugin_manager_templates', function() {
	return gulp.src('src/plugin_manager/jade/templates/*.jade')
		.pipe(jadeClient('templates.js'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('plugin_manager_styles', function() {
	return gulp.src('src/plugin_manager/stylus/*.styl')
		.pipe(stylus({
			use: [nib()]
		}))
		.pipe(concat('main.css'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('zip', function() {
	return gulp.src('build/**')
		.pipe(zip('base_html.zip'))
		.pipe(gulp.dest('dist'));
});
