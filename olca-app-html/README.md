olca-app-html
=============
This repository contains the HTML and JavaScript resources of the openLCA 
application.  

Libraries
---------
Most of the libraries are managed via [Bower](http://bower.io/) except of the
following:

* [Chart.js](http://www.chartjs.org/): We use a 
  [patched version](https://github.com/msrocka/Chart.js). 

* For stacked bar charts we use 
  [Chart.StackedBar.js](https://github.com/Regaddi/Chart.StackedBar.js), 
  which is a plugin for Chart.js.

Build
-----
The build of the HTML package is managed by [gulp.js](http://gulpjs.com/) via
the following tasks (see [gulpfile.js](./gulpfile.js)):

* `gulp`: the default tasks creates the HTML pages and copies these pages together
  with the required libraries to the 'build' folder
* `gulp clean`: clears the 'build' folder
* `gulp zip`: creates an archive under .dist/base_html.zip wich contains all HTML 
  pages and libraries that is used in the openLCA application. 

After the build you have to copy the .dist/base_html.zip package manually to the
openLCA application directory olca-app/html/.
