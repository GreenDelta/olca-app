// create the module and register the controller
var app = angular.module('UsagePage', []);
var Content = function ($scope) {
    $scope.items = [];
    $scope.pager = new olca.Pager(100);
    $scope.nameFilter = "";

    $scope.setData = function (items) {
        $scope.items = items;
        $scope.pager.setItems(items);
    };

    $scope.pageNumbers = function () {
        var numbers = [];
        for (var i = 1; i <= $scope.pager.pageCount; i++) {
            numbers.push(i);
        }
        return numbers;
    };

    $scope.search = function () {
        if (!($scope.nameFilter)) {
            $scope.pager.setItems($scope.items);
        } else {
            var filter = $scope.nameFilter.toLowerCase();
            var filtered = [];
            for (var i = 0; i < $scope.items.length; i++) {
                var item = $scope.items[i];
                if (!item.name)
                    continue;
                if (item.name.toLowerCase().indexOf(filter) != -1)
                    filtered.push(item);
            }
            $scope.pager.setItems(filtered);
        }
    }

};
app.controller('Content', Content);

// set the page data
function setData(descriptors) {
    var items = makeItems(descriptors);
    var element = document.getElementById("contentDiv");
    var scope = angular.element(element).scope();
    scope.$apply(function () {
        scope.setData(items)
    });
}

// Transform the input descriptors to items for the table.
function makeItems(descriptors) {
    var items = [];
    for (var i = 0; i < descriptors.length; i++) {
        var descriptor = descriptors[i];
        var item = {};
        for (var prop in descriptor) {
            if (descriptor.hasOwnProperty(prop)) {
                item[prop] = descriptor[prop];
            }
        }
        item.image = getImage(descriptor);
        item.descriptor = descriptor;
        items.push(item);
    }
    return items;
}

// calls the external method 'openModel'
function doOpenModel(item) {
    if (typeof (openModel) != 'undefined') {
        openModel(angular.toJson(item.descriptor));
    } else {
        console.log('openModel is not a registered function');
    }
}

// get the image for the
function getImage(descriptor) {
    if (descriptor.type == 'PROJECT')
        return '../images/16x16_project_blue.png';
    if (descriptor.type == 'IMPACT_METHOD')
        return '../images/16x16_impact_method_blue.png';
    if (descriptor.type == 'PROCESS')
        return '../images/16x16_process_blue.png';
    if (descriptor.type == 'FLOW')
        return '../images/16x16_flow_blue.png';
    if (descriptor.type == 'FLOW_PROPERTY')
        return '../images/16x16_flow_property_blue.png';
    if (descriptor.type == 'UNIT_GROUP')
        return '../images/16x16_unit_group_blue.png';
    if (descriptor.type == 'UNIT_GROUP')
        return '../images/16x16_unit_group_blue.png';
    if (descriptor.type == 'ACTOR')
        return '../images/16x16_actor_blue.png';
    if (descriptor.type == 'SOURCE')
        return '../images/16x16_source_blue.png';
    else
        return '../images/16x16_flow_blue.png'; // TODO: unknown image
}