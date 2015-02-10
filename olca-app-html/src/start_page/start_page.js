$(document).ready(function () {
	$('#gd-logo').on('click', function () {
		callOpenUrl('http://www.greendelta.com');
	});
});

var app = angular.module('StartPage', []);

var Controller = function ($scope) {
	$scope.data = {};
};
app.controller('Controller', Controller);

function setData(data) {
	var element = document.getElementById("contentDiv");
	var scope = angular.element(element).scope();
	scope.$apply(function () {
		scope.data = data;
	});
}

// the functions openUrl and importDatabase should be registered by the 
// host editor in openLCA
function callOpenUrl(url) {
	if (typeof (openUrl) === "function") {
		openUrl(url);
	} else {
		window.open(url);
	}
}


