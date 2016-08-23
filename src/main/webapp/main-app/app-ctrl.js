angular.module('maritimeweb.app').controller("AppController", ['$scope', '$http', '$window', '$timeout', 'Auth', 'MapService', 'VesselService', 'NwNmService', 'growl', '$uibModal',
    function ($scope, $http, $window, $timeout, Auth, MapService, VesselService, NwNmService, growl, $uibModal) {
        var loadTimerService = false;

        $scope.welcomeToBalticWebModal = function (size) {
            var uibModalInstance = $uibModal.open({
                animation: 'true',
                templateUrl: '/prototype/js/welcome.html',
                controller: 'AcceptTermsCtrl',
                size: size
            })
        };

        $scope.loggedIn = Auth.loggedIn;

        /** Logs the user in via Keycloak **/
        $scope.login = function () {
            Auth.authz.login();
        };

        /** Logs the user out via Keycloak **/
        $scope.logout = function () {
            Auth.authz.logout();
        };

        /** Returns the user name ,**/
        $scope.userName = function () {
            if (Auth.authz.idTokenParsed) {
                return Auth.authz.idTokenParsed.name
                    || Auth.authz.idTokenParsed.preferred_username;
            }
            return undefined;
        };

        /** Enters the Keycloak account management **/
        $scope.accountManagement = function () {
            Auth.authz.accountManagement();
        };

        // Map state and layers
        $scope.mapState = JSON.parse($window.localStorage.getItem('mapState-storage')) ? JSON.parse($window.localStorage.getItem('mapState-storage')) : {};
        $scope.mapBackgroundLayers = MapService.createStdBgLayerGroup();
        $scope.mapWeatherLayers = MapService.createStdWeatherLayerGroup();
        $scope.mapMiscLayers = MapService.createStdMiscLayerGroup();
        //$scope.mapTrafficLayers = ""; // is set in the ais-vessel-layer


        var accepted_terms = $window.localStorage.getItem('terms_accepted_ttl');
        console.log("accepted_terms ttl = " + accepted_terms);
        var now = new Date();

        if (accepted_terms == null || (new Date(accepted_terms).getTime() < now )) {
            $scope.welcomeToBalticWebModal('lg');
        } else {
            growl.info("Welcome back");
        }


        /**************************************/
        /** Vessel sidebar functionality      **/
        /**************************************/

        // Vessels
        $scope.vessels = [];

        /** Returns the icon to use for the given vessel **/
        $scope.iconForVessel = function (vo) {
            return '/img/' + VesselService.imageAndTypeTextForVessel(vo).name;
        };

        /** Returns the lat-lon attributes of the vessel */
        $scope.toLonLat = function (vessel) {
            return {lon: vessel.x, lat: vessel.y};
        };

        /**************************************/
        /** NW-NM sidebar functionality      **/
        /**************************************/

        $scope.nwNmServices = [];
        $scope.nwNmMessages = [];

        /** Reloads the NW-NM services **/
        $scope.refreshNwNmServices = function () {

            $scope.nwNmServices.length = 0;

            NwNmService.getNwNmServices($scope.mapState['wktextent'])
                .success(function (services) {
                    // Update the selected status from localstorage
                    angular.forEach(services, function (service) {
                        $scope.nwNmServices.push(service);
                        service.selected = $window.localStorage[service.instanceId] == 'true';
                    })
                })
                .error(function (error) {
                    // growl.error("Error getting NW NM service. Reason=" + error);
                    console.error("Error getting NW NM service. Reason=" + error);
                })
        };
/*

        var stopWatching = $scope.$watch("mapState['wktextent']", function (newValue) {
            if (angular.isDefined(newValue)) {

                if (loadTimerService) {
                    $timeout.cancel(loadTimerService);
                }
                loadTimerService = $timeout(function () {
                    //console.log("load timer start");
                    $scope.refreshNwNmServices();
                }, 5000);
            }
        });

*/

        /** Update the selected status of the service **/
        $scope.nwNmSelected = function (service) {
            $window.localStorage[service.instanceId] = service.selected;
        };


        /** Toggle the selected status of the layer **/
        $scope.toggleLayer = function (layer) {
            (layer.getVisible() == true) ? layer.setVisible(false) : layer.setVisible(true); // toggle layer visibility
            if (layer.getVisible()) {
                growl.info('Activating ' + layer.get('title') + ' layer');
            }
        };

        /** Toggle the selected status of the service **/
        $scope.toggleService = function (service) {
            service.selected = (service.selected == true) ? false : true; // toggle layer visibility
            if (service.selected) {
                growl.info('Activating ' + service.name + ' layer');
            }
        };

        /** Toggle the selected status of the service **/
        $scope.switchBaseMap = function (basemap) {
            angular.forEach($scope.mapBackgroundLayers.getLayers().getArray(), function (value) { // disable every basemaps
                // console.log("disabling " + value.get('title'));
                value.setVisible(false)
            });
            basemap.setVisible(true);// activate selected basemap
            growl.info('Activating map ' + basemap.get('title'));
        };

        $scope.showVesselDetails = function (vessel) {
            console.log("mmsi" + vessel);
            //var vesselDetails = VesselService.details(vessel.mmsi);
            VesselService.showVesselInfoFromMMsi(vessel);
            //console.log("App Ctr received = vesselDetails" +JSON.stringify(vesselDetails));
            //growl.info("got vesseldetails " + JSON.stringify(vesselDetails));
            growl.info("Vessel details retrieved");

        };


        /** Show the details of the message */
        $scope.showNwNmDetails = function (message) {
            NwNmService.showMessageInfo(message);
        };


        /** Returns the area heading for the message with the given index */
        $scope.nwnmAreaHeading = function (index) {
            var msg = $scope.nwNmMessages[index];
            return NwNmService.getAreaHeading(msg);
        };

    }]);
