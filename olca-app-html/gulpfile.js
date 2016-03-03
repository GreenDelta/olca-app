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
var foreach = require('gulp-foreach');
var rename = require('gulp-rename');
var fs = require('fs');
$ = require('gulp-load-plugins')()

gulp.task('default', function() {
	runSequence('clean', 'precompile', 'build', 'zip');
});
gulp.task('precompile', ['plugin_manager_scripts', 'plugin_manager_templates', 'plugin_manager_styles']);
gulp.task('build', ['html_pages', 'jade_pages', 'resources', 'libs', 'start-page-templates', 'start-page-styles']);
gulp.task('resources', ['images']);
gulp.task('libs', ['base_libs', 'bootstrap']);

gulp.task('clean', function() {
	return gulp.src([
			'build',
			'src/plugin_manager/precompiled'
		], {read: false})
		.pipe(clean());
});

gulp.task('html_pages', function() {
	return gulp.src([
			'src/report_view/report_view.html',
			'src/start_page/*.html',
			'src/devtools/*',
			'src/*.html'
		])
		.pipe(fileinclude({
			prefix: '//@',
			basepath: '@file'
		}))
		.pipe(gulp.dest('build'));
});

gulp.task('jade_pages', function() {
	return gulp.src('src/plugin_manager/jade/plugin_manager.jade')
		.pipe(jade({ locals: {} }))
		.pipe(gulp.dest('build'));
});

gulp.task('images', function() {
	return gulp.src('images/*')
		.pipe(gulp.dest('build/images'));
});

gulp.task('base_libs', function() {
	return gulp.src([
			'node_modules/angular/angular.min.js',
			'node_modules/angular-sanitize/angular-sanitize.min.js',
			'node_modules/codemirror/lib/*',
			'node_modules/codemirror/mode/javascript/javascript.js',
			'node_modules/codemirror/mode/python/python.js',
			'node_modules/d3/d3.min.js',
			'node_modules/jquery/dist/jquery.min.js',
			'other_libs/**/*.*'
		])
		.pipe(gulp.dest('build/libs'));
});

gulp.task('bootstrap', function() {
	return gulp.src(['node_modules/bootstrap/dist/**'])
		.pipe(gulp.dest('build/libs/bootstrap'));
});

gulp.task('plugin_manager_scripts', function() {
	return gulp.src('src/plugin_manager/coffeescript/*.coffee')
		.pipe(coffee({ bare: true }))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('plugin_manager_templates', function() {
	return gulp.src('src/plugin_manager/jade/templates/*.jade')
		.pipe(jadeClient('templates.js'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('plugin_manager_styles', function() {
	return gulp.src('src/plugin_manager/stylus/*.styl')
		.pipe(stylus({ use: [nib()] }))
		.pipe(concat('main.css'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

var readMessages = function(file, defaultMsg) {
	var data = fs.readFileSync(file, 'utf8');
	if (!data) {
		return {};
	}
	var msg = JSON.parse(JSON.stringify(defaultMsg));
	var lines = data.split('\n');
	for (var i = 0; i < lines.length; i++) {
		var line = lines[i];
		if (line.indexOf('#') === 0 || !line || !line.length) {
			continue; // ignore comments
		}
		var key = line.substring(0, line.indexOf('='));
		var value = line.substring(line.indexOf('=') + 1);
		var target = msg;
		var dotIndex = -1;
		while ((dotIndex = key.indexOf('.')) !== -1) {
			var subKey = key.substring(0, dotIndex);
			if (!target[subKey]) {
				target[subKey] = {};
			}
			var target = target[subKey];
			var key = key.substring(dotIndex + 1);
		}
		target[key] = value;
	}
	return msg;
};

gulp.task('start-page-templates', function() {
	var defaultMsg = readMessages(__dirname + '/src/start_page/msg/messages.properties', {});
	return gulp.src('./src/start_page/msg/*.properties')
		.pipe(foreach(function(stream, f) {
			var file = f.path;
			var dotIndex = file.indexOf('.properties');
			var suffix = '';
			if (dotIndex !== -1 && file.charAt(dotIndex - 3) === '_') {
				var suffix = file.substring(dotIndex - 3, dotIndex);
			}
			var newName = 'start_page' + suffix + '.html';
			var msg = readMessages(file, defaultMsg);
			return gulp.src('./src/start_page/start_page.jade')
				.pipe($.jade({
					locals: {msg: msg}
				}))
				.pipe(rename(newName))
				.pipe(gulp.dest('./build'));
		}));
});

gulp.task('start-page-styles', function() {
	return gulp.src('./src/start_page/start_page.styl')
		.pipe(stylus({
			use: [nib()]
		}))
		.pipe(concat('start_page.css'))
		.pipe(gulp.dest('./build'));
});

gulp.task('zip', function() {
	return gulp.src('build/**')
		.pipe(zip('base_html.zip'))
		.pipe(gulp.dest('../olca-app/html'));
});
