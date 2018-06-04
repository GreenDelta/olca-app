var gulp = require('gulp');
var clean = require('gulp-clean');
var runSequence = require('run-sequence');
var fileinclude = require('gulp-file-include');
var zip = require('gulp-zip');
var concat = require('gulp-concat');
var stylus = require('gulp-stylus');
var nib = require('nib');
var pug = require('gulp-pug');
var coffee = require('gulp-coffee');
var foreach = require('gulp-foreach');
var rename = require('gulp-rename');
var fs = require('fs');

gulp.task('default', function () {
	runSequence('clean', 'precompile', 'build', 'zip');
});

gulp.task('precompile',
	['plugin_manager_scripts',
		'plugin_manager_templates',
		'plugin_manager_styles',
		'update_manager_scripts',
		'update_manager_templates',
		'update_manager_styles']);

gulp.task('build', [
	'html_pages',
	'pug_pages',
	'resources',
	'libs',
	'start-page-templates',
	'start-page-styles']);

gulp.task('resources', ['images', 'fonts']);
gulp.task('libs', ['base_libs', 'bootstrap']);

gulp.task('clean', function () {
	return gulp.src([
		'build',
		'src/update_manager/precompiled',
		'src/plugin_manager/precompiled'
	], { read: false })
		.pipe(clean());
});

gulp.task('html_pages', function () {
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

gulp.task('pug_pages', () => {
	return gulp.src([
		'src/plugin_manager/jade/plugin_manager.jade',
		'src/update_manager/jade/update_manager.jade'])
		.pipe(pug({ locals: {}, compileDebug: false, verbose: true }))
		.pipe(gulp.dest('build'));
});

gulp.task('images', function () {
	return gulp.src('images/*')
		.pipe(gulp.dest('build/images'));
});

gulp.task('fonts', function () {
	return gulp.src('fonts/*')
		.pipe(gulp.dest('build/fonts'));
});

gulp.task('base_libs', function () {
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

gulp.task('bootstrap', function () {
	return gulp.src(['node_modules/bootstrap/dist/**'])
		.pipe(gulp.dest('build/libs/bootstrap'));
});

gulp.task('plugin_manager_scripts', function () {
	return gulp.src('src/plugin_manager/coffeescript/*.coffee')
		.pipe(coffee({ bare: true }))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('update_manager_scripts', function () {
	return gulp.src('src/update_manager/coffeescript/*.coffee')
		.pipe(coffee({ bare: true }))
		.pipe(gulp.dest('src/update_manager/precompiled'));
});

gulp.task('plugin_manager_templates', function () {
	return gulp.src('src/plugin_manager/jade/templates/*.jade')
		.pipe(pug({ client: true, compileDebug: false, verbose: true }))
		.pipe(rename('templates.js'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('update_manager_templates', function () {
	return gulp.src('src/update_manager/jade/templates/*.jade')
		.pipe(pug({ client: true, compileDebug: false, verbose: true }))
		.pipe(rename('templates.js'))
		.pipe(gulp.dest('src/update_manager/precompiled'));
});

gulp.task('plugin_manager_styles', function () {
	return gulp.src('src/plugin_manager/stylus/*.styl')
		.pipe(stylus({ use: [nib()] }))
		.pipe(concat('main.css'))
		.pipe(gulp.dest('src/plugin_manager/precompiled'));
});

gulp.task('update_manager_styles', function () {
	return gulp.src('src/update_manager/stylus/*.styl')
		.pipe(stylus({ use: [nib()] }))
		.pipe(concat('main.css'))
		.pipe(gulp.dest('src/update_manager/precompiled'));
});

const readMessages = (file, defaultMsg) => {
	const data = fs.readFileSync(file, 'utf8');
	const msg = JSON.parse(JSON.stringify(defaultMsg));
	if (!data) {
		return msg;
	}
	const lines = data.split('\n');
	for (let line of lines) {
		if (line.indexOf('#') === 0 || !line || !line.length) {
			continue; // ignore comments
		}
		let key = line.substring(0, line.indexOf('='));
		const value = line.substring(line.indexOf('=') + 1);
		let target = msg;
		let dotIndex = -1;
		while ((dotIndex = key.indexOf('.')) !== -1) {
			var subKey = key.substring(0, dotIndex);
			if (!target[subKey]) {
				target[subKey] = {};
			}
			target = target[subKey];
			key = key.substring(dotIndex + 1);
		}
		target[key] = value;
	}
	return msg;
};

gulp.task('start-page-templates', function () {
	var defaultMsg = readMessages(__dirname + '/src/start_page/msg/messages.properties', {});
	return gulp.src('./src/start_page/msg/*.properties')
		.pipe(foreach((stream, f) => {
			const file = f.path;
			console.log(`generate start page for ${file}`);
			const dotIndex = file.indexOf('.properties');
			let suffix = '';
			if (dotIndex !== -1 && file.charAt(dotIndex - 3) === '_') {
				suffix = file.substring(dotIndex - 3, dotIndex);
			}
			const newName = 'start_page' + suffix + '.html';
			const msg = readMessages(file, defaultMsg);
			return gulp.src('./src/start_page/start_page.pug')
				.pipe(pug({
					locals: { msg: msg }, compileDebug: false, verbose: true
				}))
				.pipe(rename(newName))
				.pipe(gulp.dest('./build'));
		}));
});

gulp.task('start-page-styles', function () {
	return gulp.src('./src/start_page/start_page.css')
		.pipe(gulp.dest('./build'));
});

gulp.task('zip', function () {
	return gulp.src('build/**')
		.pipe(zip('base_html.zip'))
		.pipe(gulp.dest('../olca-app/html'));
});