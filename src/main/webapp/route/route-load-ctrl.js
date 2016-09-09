angular.module('maritimeweb.route')
/*******************************************************************
 * Controller that handles uploading an RTZ route for a vessel
 *  and generate the needed open-layers features.
 *******************************************************************/
    .controller('RouteLoadCtrl', ['$scope', '$rootScope', '$http', '$routeParams', '$window',  'growl', 'timeAgo', '$filter', 'Upload', '$timeout', 'fileReader', '$log',
        function ($scope, $rootScope, $http, $routeParams, $window, growl, timeAgo, $filter, Upload, $timeout, fileReader,$log) {
            'use strict';
            console.log("RouteLoadCtrl routeParams.mmsi=" + $routeParams.mmsi);
            $scope.instantiateListsforCharts = function () {
                var charts = {};
                charts.listMinSpeed = []; // speed
                charts.listMinSpeedLabels = [];  // speed labels
                charts.listRadius = [];
                charts.listETA = [];
                charts.listID = [];
                return charts;
            };

            $scope.activeWayPoint = 0;

            var charts =  $scope.instantiateListsforCharts();


            // debug menu starts collapsed.
            $scope.xmlCollapsed = true;
            $scope.jsonCollapsed = true;
            $scope.jsonFeatCollapsed = true;


            $scope.sampleRTZdata = [
                {id: 'ExamplefileworkswithENSI.rtz', name: 'Talin - Helsinki'},
              /*  {id: 'muugaPRVconsprnt.rtz', name: 'Talin - Helsinki'},*/
                {id: 'hesastofuru.rtz', name: 'Helsinki - Stockholm'},
                {id: 'kielPRV.rtz', name: 'Helsinki - Kiel'}
            ];
            $scope.sampleFile = $scope.sampleRTZdata[0].id;
            var resetChartArrays = function () {
                charts.listMinSpeed.splice(0, charts.listMinSpeed.length);
                charts.listMinSpeedLabels.splice(0, charts.listMinSpeedLabels.length);
                charts.listRadius.splice(0, charts.listRadius.length);
                charts.listETA.splice(0, charts.listETA.length);
                charts.listID.splice(0, charts.listID.length);
            };
            /**
             * Adds the feature placeholder to all relevant chart lists.
             * @param feature
             */
            var addFeatureToCharts = function (feature) {
                charts.listMinSpeed.push(feature.speed);
                charts.listMinSpeedLabels.push(feature.wayname);
                charts.listRadius.push(feature.radius);
                charts.listETA.push(feature.eta);
                charts.listID.push(feature.id);
            };
            /**
             * Generate a openlayers features array and ol points array from the transformed RTZ JSON.
             * @param json_result transformed RTZ JSON from an RTZ xml
             */
            var createOpenLayersFeatFromRTZ = function (json_result) {
                $scope.rtzJSON = json_result; // used for debugging.
                $scope.rtzName = json_result.route.routeInfo._routeName;

                $scope.oLfeatures = [];
                $scope.oLpoints = [];
                resetChartArrays();

                angular.forEach(json_result.route.waypoints.waypoint, function (way_value, key) {
                    angular.forEach(json_result.route.schedules.schedule.calculated.sheduleElement, function (schedule_value, key) {
                        if (way_value._id == schedule_value._waypointId) { // pairing schedule events with waypoints
                            var feature = {
                                id: way_value._id,
                                wayname: way_value._name,
                                radius: way_value._radius,
                                position: way_value.position,
                                leg: way_value.leg,
                                speed: schedule_value._speed,
                                eta: schedule_value._eta
                            };
                            addFeatureToCharts(feature);
                            $scope.oLpoints.push( ol.proj.transform([parseFloat(way_value.position._lon), parseFloat(way_value.position._lat)], 'EPSG:4326', 'EPSG:900913'));
                            $scope.oLfeatures.push($scope.createWaypointFeature(feature));
                        }
                    });
                });

            };

            /**
             * convience method for loading a sample rtz route
             */
            $scope.autoPreloadRTZfile = function(){
                $http.get('/route/sample-rtz-files/' + $scope.sampleFile, {
                    transformResponse: function (data, headers) {
                        $scope.rtzXML = data;
                        $scope.rtzJSON = fileReader.transformRtzXMLtoJSON(data);
                        return $scope.rtzJSON;
                    }
                }).then(function(result){
                    createOpenLayersFeatFromRTZ(result.data);
                });
            };

            $scope.getFile = function () {
                $scope.progress = 0;
                fileReader.readAsDataUrl($scope.file, $scope)
                    .then(function(result) {
                        $scope.rtzXML = result;
                        $scope.rtzJSON = fileReader.transformRtzXMLtoJSON(result);
                        createOpenLayersFeatFromRTZ($scope.rtzJSON);
                    });
            };

            $scope.$on("fileProgress", function(e, progress) {
                $scope.progress = progress.loaded / progress.total;
            });

            /** Create a waypoint feature, with  lat,lon,. */
            $scope.createWaypointFeature = function (waypoint) {
                var markerStyle = new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: 3,
                        stroke: new ol.style.Stroke({
                            color: 'red',
                            width: 2
                        }),
                       fill: new ol.style.Fill({
                            color: [255, 0, 0, 0.5]
                        })
                    })
                });

                var waypointPosition = new ol.geom.Point(ol.proj.transform([parseFloat(waypoint.position._lon), parseFloat(waypoint.position._lat)], 'EPSG:4326', 'EPSG:900913'));

                var markWaypoint = new ol.Feature({
                    geometry: waypointPosition,
                    name: waypoint.wayname,
                    wayname: waypoint.wayname,
                    id: waypoint.id,
                    radius: waypoint.radius,
                    eta: waypoint.eta,
                    speed: waypoint.speed,
                    leg: waypoint.leg,
                    //ts: $filter('date')(value.ts, 'yyyy-MM-dd HH:mm:ss Z', 'UTC') + ' UTC',
                    //tsTimeAgo: $filter('timeAgo')(value.ts),
                    position: $scope.toLonLat(waypoint.position._lon, waypoint.position._lat)
                });
                markWaypoint.setStyle(markerStyle);
                return markWaypoint;
            };

            // while watch if a new RTZ route has been uploaded
            $scope.$watch("sampleFile", function(newValue, oldValue) {
                if (newValue){
                    $log.log("sample file uploaded" + $scope.sampleFile);

                    $scope.autoPreloadRTZfile(); // TODO: disable the auto load later on

                    $window.scrollTo(0, 0);
                }
            }, true);
           // $scope.autoPreloadRTZfile(); // TODO: disable the auto load later on

            // SPEED Charts
            // Chart.js with speed-over-ground

           /* angular.forEach(response.data, function (value, key) {
                listMinSpeed.push(value.sog);
                listMinSpeedLabels.push($filter('timeAgo')(value.ts) + ' - ' + $filter('date')(value.ts, 'yyyy-MM-dd HH:mm:ss Z', 'UTC') + ' UTC');
            });
*/
            $scope.sogChartlabels = charts.listMinSpeedLabels;
            $scope.sogChartseries = ['Minimum speed ', ' radius'];
            $scope.sogChartdata = [charts.listMinSpeed,charts.listRadius];
            $scope.onClick = function (points, evt) {
               // console.log(points, evt);
                //$log.info(points[0]._index);
                angular.forEach(points, function (value, key) {
                    //console.log("#" + value._index);
                    if(value._index != null && value._index >= 0){
           //             $scope.activeWayPoint = points[0]._index;
                        $rootScope.activeWayPoint = points[0]._index;
                    }
                    //value._index;
                });
                console.log("#" + $rootScope.activeWayPoint);
           //     $scope.$apply();
                $rootScope.$apply();


                /*if(points[0]._index){
                    $scope.activeWayPoint = points[0]._index;
                }*/


            };

            $scope.sogChartdatasetOverride = [{
                yAxisID: 'y-axis-1',
                /*yAxisID: 'y-axis-2',*/
                borderJoinStyle: 'round',
                pointRadius: 1,
                pointHitRadius: 10,
                pointBorderColor: "rgba(0,0,0,1)",
                pointBackgroundColor: "#fff",
                pointBorderWidth: 1
            }];
            $scope.sogChartoptions = {
                responsive: true,
                showLines: true,

                responsiveAnimationDuration: 1500,
                scales: {
                    yAxes: [
                        {
                            id: 'y-axis-1',
                            type: 'linear',
                            display: true,
                            position: 'left'
                        }
/*                        ,
                        {
                            id: 'y-axis-2',
                            type: 'linear',
                            display: true,
                            position: 'left'
                        }*/
                    ],
                    xAxes: [{
                        display: true
                    }]
                }
            };


        }]);

