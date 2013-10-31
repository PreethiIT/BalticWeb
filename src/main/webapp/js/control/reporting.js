embryo.reporting = {

}

embryo.reporting.vesselInformation = {
    title: "Vessel Information",
    status: function (vesselOverview, vesselDetails) {
        return "OK";
    },
    show: function (vesselOverview, vesselDetails) {
        $("#vesselInformationPanel").css("display", "block");
        $("#maxSpeed").focus();
    },
    hide: function () {
        $("#vesselInformationPanel").css("display", "none");
    }
}

embryo.reporting.schedule = {
    title: "Schedule",
    status: function (vesselOverview, vesselDetails) {
        return "OK";
    },
    show: function (vesselOverview, vesselDetails) {
        $("#schedulePanel").css("display", "block");
    },
    hide: function () {
        $("#schedulePanel").css("display", "none");
    }
}

embryo.reporting.activeRoute = {
    title: "Active Route",
    status: function (vesselOverview, vesselDetails) {
        return "OK";
    },
    show: function (vesselOverview, vesselDetails) {
        $("#activeRoutePanel").css("display", "block");
    },
    hide: function () {
        $("#activeRoutePanel").css("display", "none");
    }
}

embryo.reporting.greenposReport = {
    title: "Greenpos Report",
    status: function (vesselOverview, vesselDetails) {
        return "OK";
    },
    show: function (vesselOverview, vesselDetails) {
        $("#greenposReportPanel").css("display", "block");
    },
    hide: function () {
        $("#greenposReportPanel").css("display", "none");
    }
}

function setupReporting(id, vessel, vesselDetails) {
    var html = "";
    $.each(embryo.reporting, function (k, v) {
        html += "<tr><th>"+v.title+"</th>";

        var status = v.status(vessel, vesselDetails)

        switch(status) {
            case "OK":
                html += "<td><span class='label label-success'>OK</span></td>"
                break;
            default:
                html += "<td><span class='label'>NOT AVAILABLE</span></td>"
                break;
        }

        html += "<td><a href=# aid="+k+">edit</a></td>"

        html += "</tr>"
    });

    $(id).html(html);

    $("a", $(id)).click(function (e) {
        e.preventDefault();
        clearAdditionalInformation();
        embryo.reporting[$(this).attr("aid")].show(vessel, vesselDetails);
        $(this).parents("tr").addClass("alert");
    })
}