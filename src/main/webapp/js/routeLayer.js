/*
 * Dependencies:
 * 
 * aisview.js
 * aisviewUI.js
 * ....
 */

embryo.route = {};

(function() {

	"use strict";

	embryo.route.initLayer = function() {
		console.log('Initialization of Route Layer');

		// Create vector layer for routes

		// Find a better color code. How to convert sRGB to HTML codes?
		var defTemplate = OpenLayers.Util.applyDefaults({
			strokeWidth : 2,
			strokeDashstyle : 'dashdot',
			strokeColor : "${getColor}", // using context.getColor(feature)
			fillColor : "${getColor}" // using context.getColor(feature)
		}, OpenLayers.Feature.Vector.style["default"]);

		var context = {
			getColor : function(feature) {
				return feature.attributes.active ? 'red ' : '#D5672D';
			}
		};

		var defaultStyle = new OpenLayers.Style(defTemplate, {
			context : context
		});

		var select = OpenLayers.Util.applyDefaults({}, OpenLayers.Feature.Vector.style.select);
		var selectStyle = new OpenLayers.Style(select);

		var temporary = OpenLayers.Util.applyDefaults({}, OpenLayers.Feature.Vector.style.temporary);
		var temporaryStyle = new OpenLayers.Style(temporary);

		embryo.route.layer = new OpenLayers.Layer.Vector("routeLayer", {
			styleMap : new OpenLayers.StyleMap({
				'default' : defaultStyle,
				'select' : selectStyle,
				'temporary' : temporaryStyle
			})
		});

		embryo.map.add({
			layer : embryo.route.layer,
			select : false
		});

		embryo.route.drawActiveRoute();
	};

	embryo.route.drawActiveRoute = function() {
		var injector = angular.element(document).injector();
		var RouteService = injector.get('RouteService');
		var ShipService = injector.get('ShipService');

		ShipService.getYourShip(function(ship) {
			function callback(route) {
				if (typeof route !== 'undefined') {
					embryo.route.draw(route, true);
				}
			}

			RouteService.getActive(ship.mmsi, callback);
		});
	};

	embryo.route.draw = function(route, active) {
		// Remove old tracks
		// routeLayer.removeAllFeatures();

		if (!active) {
			active = false;
		}

		// Draw tracks
		if (route && route.waypoints) {
			var firstPoint = true;
			var currentPoint;
			var previousPoint = null;

			var points = [];
			var lines = [];

			for ( var index in route.waypoints) {
				currentPoint = embryo.map.createPoint(route.waypoints[index].longitude, route.waypoints[index].latitude);

				//points.push(embryo.route.createWaypointFeature(currentPoint));
				if (!firstPoint) {
					lines.push(new OpenLayers.Geometry.LineString([previousPoint, currentPoint]));
				}
				firstPoint = false;
				previousPoint = currentPoint;
			}

			var multiLine = new OpenLayers.Geometry.MultiLineString(lines);
			var feature = new OpenLayers.Feature.Vector(multiLine, {
				featureType : 'route',
				active : active,
				route : route
			});
			
			embryo.route.layer.addFeatures([feature]);
			// embryo.route.layer.addFeatures(points);

			// Draw features
			embryo.route.layer.refresh();
		}
	};

	embryo.route.addModifyControl = function() {
		// var controls = {
		// point: new OpenLayers.Control.DrawFeature(embryo.route.layer,
		// OpenLayers.Handler.Point),
		// line: new OpenLayers.Control.DrawFeature(embryo.route.layer,
		// OpenLayers.Handler.Path)
		// };
		//        
		// for(var key in controls) {
		// embryo.mapPanel.map.addControl(controls[key]);
		// }

		embryo.route.modCtrl = new OpenLayers.Control.ModifyFeature(embryo.route.layer, {
			createVertices : true,
			mode : OpenLayers.Control.ModifyFeature.RESHAPE
		});
		embryo.mapPanel.map.addControl(embryo.route.modCtrl);
		embryo.route.modCtrl.activate();

		embryo.route.layer.events.on({
			"beforefeaturemodified" : function(feature) {
				console.log('beforefeaturemodified' + feature);
			},
			'featureselected' : function(feature) {
				console.log('featureselected' + feature);
			},
			'featureunselected' : function(feature) {
				console.log('featureunselected' + feature);
			}
		});
	};

	embryo.route.createWaypointFeature = function(point) {
		var style_green = {
			strokeColor : "#00FF00",
			// strokeColor: "#ee9900",
			strokeWidth : 6,
			pointRadius : 6,
		};

		return new OpenLayers.Feature.Vector(OpenLayers.Geometry.Polygon.createRegularPolygon(point, 20.0, 30, 0.0),
				null, style_green);
	};

	setTimeout(embryo.route.initLayer,1000);
	
//	embryo.authenticated(embryo.route.initLayer);
}());