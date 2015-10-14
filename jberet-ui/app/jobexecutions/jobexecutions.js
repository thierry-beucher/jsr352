'use strict';

angular.module('jberetUI.jobexecutions', ['ngRoute',
    'ngAnimate', 'ngTouch', 'ui.grid', 'ui.grid.selection', 'ui.grid.exporter'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/jobexecutions', {
            templateUrl: 'jobexecutions/jobexecutions.html',
            controller: 'JobexecutionsCtrl'
        });
    }])

    .controller('JobexecutionsCtrl', ['$scope', '$http', function ($scope, $http) {
        var cellTemp =
            '<div ng-class="col.colIndex()"><div>{{grid.getCellValue(row, col) | date:"yyyy-MM-dd HH:mm:ss"}}</div></div>';

        $scope.gridOptions = {
            enableGridMenu: true,
            enableSelectAll: true,
            exporterCsvFilename: 'job-executions.csv',

            enableFiltering: true,
            showGridFooter: true,
            minRowsToShow: 10,
            rowHeight:40,
            columnDefs: [
                {name: 'executionId', type: 'number'},
                {name: 'jobName', cellTooltip: true},
                {name: 'jobParameters', cellTooltip: true},
                {name: 'batchStatus'},
                {name: 'exitStatus', cellTooltip: true},
                {name: 'createTime', cellTemplate: cellTemp, cellTooltip: true, type: 'date'},
                {name: 'startTime', cellTemplate: cellTemp, cellTooltip: true, type: 'date'},
                {name: 'lastUpdatedTime', cellTemplate: cellTemp, cellTooltip: true, type: 'date'},
                {name: 'endTime', cellTemplate: cellTemp, cellTooltip: true, type: 'date'}
            ]
        };

        $http.get('http://localhost:8080/restAPI/api/jobexecutions')
            .then(function (responseData) {
                $scope.gridOptions.data = responseData.data;
            }, function (responseData) {
                console.log(responseData);
            });
    }]);