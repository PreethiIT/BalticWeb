
angular.module("maritimeweb", ['ngAnimate', 'ngSanitize', 'ui.bootstrap',
    'maritimeweb.map', 'maritimeweb.maps_and_layers', 'maritimeweb.location.service','maritimeweb.vessel.layer','maritimeweb.vessel.service','maritimeweb.nw-nm.layer'])
    .controller("MapController", function($scope, $http, $timeout, vesselService, locationService, balticWebMap,vesselLayer) {
/*        vesselService.details(249453000).then(function(response) {
            console.log("mmsi details" + response.status);
            console.log("mmsi details" + response.data);


        });

*/
        console.log("Dette er en test = " + locationService.detteerentest);
        var x = balticWebMap.EPSG900913.toString;
        console.log("balticWebMap.EPSG900913= " + x);

        balticWebMap.testFunction();
        console.log(locationService.detteerentest);


        locationService.get().then(function(result){
            $scope.myPosition =  [ result.longitude,result.latitude];
            postMessageToEndUser($scope, "you are located in " + result.longitude + " " + result.latitude ,'success', 10000 );
        });

        $scope.alerts = [
            { type: 'success', msg: 'Welcome to MaritimeWeb', timeout: 2000 }
        ];

        $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
        };

        $scope.layerGroupAtons = balticWebMap.groupAtons.getLayers().getArray();
        $scope.layerGroupVessels = balticWebMap.groupVessels.getLayers().getArray();
        $scope.layerGroupBasemaps = balticWebMap.groupBaseMaps.getLayers().getArray();
        $scope.layerGroupWeather = balticWebMap.groupWeather.getLayers().getArray();

        $scope.myPosition = {};
        $scope.vesselsonmap = [];
        $scope.vessels = {};
        //var vesselVectorLayer = {};
        var firstRun = true;
        var loadTimer;


        // #########################################################################################################################
        // ################################    move this method to vessel-layer.js and remove scope dependency  ####################
        // #########################################################################################################################
        var refreshVessels = function(evt) {

            $scope.clientBBox = balticWebMap.clientBBOX();
            $scope.zoomLvl = balticWebMap.map.getView().getZoom();
            $scope.alerts.push({msg: 'Fetching vessel data',
                type: 'info',
                timeout: 2000
            });

             vesselService.getVesselsInArea($scope.zoomLvl, $scope.clientBBox).then(function(response){
                 $scope.vesselsonmap = [];
                 $scope.vessels = response;
                 console.log($scope.vessels.length + " vessels loaded  at zoomLvl=" + $scope.zoomLvl + " bbox=" + $scope.clientBBox   );

                 $scope.vesselsStatus = "OK";
                 $scope.lastFetchTimestamp = new Date();

                 for(var i = 0; i< $scope.vessels.length; i++){ // process vessel-data and create features
                     var vesselData = {  name: $scope.vessels[i].name || "",
                         type: $scope.vessels[i].type,
                         x: $scope.vessels[i].x,
                         y: $scope.vessels[i].y,
                         angle: $scope.vessels[i].angle,
                         mmsi: $scope.vessels[i].mmsi || "",
                         callSign: $scope.vessels[i].callSign || "",
                         moored: $scope.vessels[i].moored || false,
                         inAW: $scope.vessels[i].inAW || false
                     };

                     var vesselFeature;
                     if($scope.zoomLvl > 8) {
                         vesselFeature = vesselLayer.createVesselFeature(vesselData);
                     }else{
                         vesselFeature = vesselLayer.createMinimalVesselFeature(vesselData);
                     }
                     $scope.vesselsonmap.push(vesselFeature);
                 }

                 // update ol3 layers with new data layers
                 var vectorSource = new ol.source.Vector({
                     features: $scope.vesselsonmap //add an array of vessel features
                 });

                 balticWebMap.groupVessels.getLayers().remove(balticWebMap.layerVessels);
                 balticWebMap.layerVessels = new ol.layer.Vector({
                     name: "vesselVectorLayer",
                     title: "Vessels - dynamic",
                     source: vectorSource
                 });

                 firstRun = false;

                 balticWebMap.groupVessels.getLayers().push(balticWebMap.layerVessels);

                 postMessageToEndUser($scope, $scope.vessels.length + " vessels retrieved",'success', 2000 );

             }, function(reason) {
                 //alert('Failed: ' + reason);
                 $scope.alerts.push({msg: "Connection problems " + reason,
                     type: 'danger',
                     timeout: 4000
                 });
             });

        };
        // #########################################################################################################################


        // When the map extent changes, reload the Vessels's using a timer to batch up changes
        var mapChanged = function () {

            if (!firstRun) { // for anything but the first run, check if the layer is visible.
                if (!balticWebMap.isLayerVisible('vesselVectorLayer', balticWebMap.groupVessels)) {
                   // console.log("     --- dont vesselVectorLayer, not visible");
                    return null;
                }
            }
            // Make sure we reload at most every second
            if (loadTimer) {
               // console.log("     --- too fast");
                $timeout.cancel(loadTimer);
            }
            loadTimer = $timeout(refreshVessels, 1000);
        };


        // #########################################################################################################################
        // ################################    Listeners                                                        ####################
        // #########################################################################################################################


        var panTomyPosition = document.getElementById('pan-to-myposition');
        panTomyPosition.addEventListener('click', function() {
            balticWebMap.panToPosition($scope.myPosition);
        }, false);

        //maritimeweb.map.once(mapChanged );
        balticWebMap.map.on('moveend', mapChanged );         // update the map when a user pan-move ends.
        balticWebMap.groupVessels.on('change:visible', mapChanged); // listens when visibility on map has been toggled.

    });

var postMessageToEndUser = function ($scope, msg, type, timeout) {
    $scope.alerts.push({
        msg: msg,
        type: type,
        timeout: timeout
    });
};