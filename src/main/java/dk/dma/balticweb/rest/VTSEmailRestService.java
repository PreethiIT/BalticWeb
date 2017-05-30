/* Copyright (c) 2017 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.balticweb.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


import dk.dma.embryo.common.configuration.PropertyFileService;
import dk.dma.embryo.common.mail.MailSender;
import dk.dma.embryo.user.mail.ForgotPasswordMail;
import dk.dma.embryo.user.model.SecuredUser;
import dk.dma.embryo.user.security.Subject;
import dk.dma.embryo.user.service.UserService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;


/**
 * This service catches JSON from frontend, compiles a nice email (subject/body) then sends it to the intended address.
 */

@Path("/vtsemail")
public class VTSEmailRestService {


    @Inject
    private MailSender mailSender;

    @Inject
    private Subject subject;


    private String generatedEmailSubject = "";
    private String generatedEmailBody = "";
    private String emailTo = "";
    private String emailFrom = "";


    @Setter
    @Getter
    private static class Reader{

        //A lot of gettersetter
        String vtsShortName;
        String vtsCallSign;
        String vtsEmail;

        String vesselName;
        String vesselCallSign;
        String vesselMMSI;
        String vesselIMO;
        String vesselDraught;
        String vesselAirDraught;
        String vesselPersonsOnboard;
        String vesselLength;
        String vesselDeadWeight;
        String vesselGRT;
        String vesselDefects;
        String vesselType;

        String fuelTotalFuel;
        String fuelTypeHFORegular;
        String fuelTypeHFOLowSulphur;
        String fuelTypeHFOUltraLowSulphur;
        String fuelTypeIFORegular;
        String fuelTypeIFOLowSulphur;
        String fuelTypeIFOUltraLowSulphur;
        String fuelTypeMDORegular;
        String fuelTypeMDOLowSulphur;
        String fuelTypeMDOUltraLowSulphur;
        String fuelTypeMGORegular;
        String fuelTypeMGOLowSulphur;
        String fuelTypeMGOUltraLowSulphur;
        String fuelTypeLPG;
        String fuelTypeLNG;

        String cargoType;
        String cargoIMOClass01;
        String cargoIMOClass02;
        String cargoIMOClass03;
        String cargoIMOClass04;
        String cargoIMOClass05;
        String cargoIMOClass06;
        String cargoIMOClass07;
        String cargoIMOClass08;
        String cargoIMOClass09;
        String cargoDangerousCargoTotalTonnage;
        String cargoDangerousCargoOnBoard;
        String cargoIMOClassesOnBoard;
        String cargoPollutantOrDCLostOverBoard;
        String cargoInformationOrOwnerContact;

        String voyagePositionLon;
        String voyagePositionLat;
        String voyageSpeed;
        String voyageTrueHeading;
        String voyagePortOfDestination;
        String voyageVTSETADate;
        String voyageVTSETATime;




    }



    @GET
    @Produces("text/plain")
    public String getMessage() {
        return "VTS report transmission service is active, expecting POST.";
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTrackInJSON(Reader vtsdata) {

        SimpleDateFormat sdf = new SimpleDateFormat( "HH:mm:ss - yyyy/MM/dd" );
        sdf.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

        //"BELTREP ship report: Brick of the Sea, IMO123456789, 11:15:43 - 2017/05/29 UTC"
        String emailSubject = vtsdata.vtsShortName + " ship report: " + vtsdata.vesselName + ", IMO" + vtsdata.vesselIMO + ", " + sdf.format(new Date()) + " UTC";

        //Start
        String emailBody = "<html><body style='color: #111111; font-family:Verdana; font-size:11px;'>";

        //General info about this email
        emailBody += "<div style='color:#bbbbbb' font-size:9px;>";
        emailBody += "This email was generated by the Baltic Web interface, as part of the EfficienSea2 project, 2017, by the Danish Maritime Authority, under grant from the European Union's Horizon 2020 reseach and innovation programme, agreement No. 636329.<br>";
        emailBody += "Contact: Department of E-Navigation, sfs@dma.dk<br>";
        emailBody += "Time and date this report was generated:<span style='color:#111111'>" + sdf.format(new Date()) + " UTC</span>";
        emailBody += "</div>";
        emailBody += "<br><br>";

        //VTS report content
        emailBody += "<br><div>";
        emailBody += "<span style='text-decoration: underline'>IMO Designator A</span><br>";
        emailBody += "Vessel name: <strong>" + vtsdata.vesselName + "</strong><br>";
        emailBody += "Vessel callsign: <strong>" + vtsdata.vesselCallSign + "</strong><br>";
        emailBody += "<br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator B</span><br>";
        emailBody += "ETA at reporting area: <strong>" + vtsdata.voyageVTSETATime + " - " + vtsdata.voyageVTSETADate + " UTC </strong>";
        emailBody += "<br><br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator I</span><br>";
        emailBody += "Destination port: <strong>" + vtsdata.voyagePortOfDestination + "</strong><br>";
        emailBody += "Expected time of arrival: <strong>N/A or unknown</strong>";
        emailBody += "<br><br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator L</span><br>";
        emailBody += "Expected route: strong>Currently disabled function</strong>";
        emailBody += "<br><br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator O</span><br>";
        emailBody += "Current maximum draught: <strong>" + vtsdata.vesselDraught + "</strong><br>";
        emailBody += "<br><br>";
        emailBody += "<span style='text-decoration: underline'>IMO Designator U2</span><br>";
        emailBody += "Current air draught: <strong>" + vtsdata.vesselAirDraught + "</strong>";
        emailBody += "<br><br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator T</span><br>";
        emailBody += "Designated Person Ashore, or communication of cargo contact information:<br><strong>" + vtsdata.cargoInformationOrOwnerContact + "</strong>";
        emailBody += "<br><br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator W</span><br>";
        emailBody += "Number of persons on board: <strong>" + vtsdata.vesselPersonsOnboard + "</strong>";
        emailBody += "<br><br>";

        emailBody += "<span style='text-decoration: underline'>IMO Designator X</span><br>";
        emailBody += "Bunker information:";

        //HFO
        if(vtsdata.fuelTypeHFORegular != "0" && vtsdata.fuelTypeHFORegular != ""  && vtsdata.fuelTypeHFORegular != null) {
            emailBody += "HFO: <strong>" + vtsdata.fuelTypeHFORegular + "</strong><br>";
        }
        if(vtsdata.fuelTypeHFOLowSulphur != "0" && vtsdata.fuelTypeHFOLowSulphur != "" && vtsdata.fuelTypeHFOLowSulphur != null) {
            emailBody += "HFO Low Sulphur: <strong>" + vtsdata.fuelTypeHFOLowSulphur + "</strong><br>";
        }
        if(vtsdata.fuelTypeHFOUltraLowSulphur != "0" && vtsdata.fuelTypeHFOUltraLowSulphur != "" && vtsdata.fuelTypeHFOUltraLowSulphur != null) {
            emailBody += "HFO Ultra Low Sulphur: <strong>" + vtsdata.fuelTypeHFOUltraLowSulphur + "</strong><br>";
        }

        //IFO
        if(vtsdata.fuelTypeIFORegular != "0" && vtsdata.fuelTypeIFORegular != ""  && vtsdata.fuelTypeIFORegular != null) {
            emailBody += "IFO: <strong>" + vtsdata.fuelTypeIFORegular + "</strong><br>";
        }
        if(vtsdata.fuelTypeIFOLowSulphur != "0" && vtsdata.fuelTypeIFOLowSulphur != "" && vtsdata.fuelTypeIFOLowSulphur != null) {
            emailBody += "IFO Low Sulphur: <strong>" + vtsdata.fuelTypeIFOLowSulphur + "</strong><br>";
        }
        if(vtsdata.fuelTypeIFOUltraLowSulphur != "0" && vtsdata.fuelTypeIFOUltraLowSulphur != "" && vtsdata.fuelTypeIFOUltraLowSulphur != null) {
            emailBody += "IFO Ultra Low Sulphur: <strong>" + vtsdata.fuelTypeIFOUltraLowSulphur + "</strong><br>";
        }

        //IFO
        if(vtsdata.fuelTypeMDORegular != "0" && vtsdata.fuelTypeMDORegular != ""  && vtsdata.fuelTypeMDORegular != null) {
            emailBody += "MDO: <strong>" + vtsdata.fuelTypeMDORegular + "</strong><br>";
        }
        if(vtsdata.fuelTypeMDOLowSulphur != "0" && vtsdata.fuelTypeMDOLowSulphur != "" && vtsdata.fuelTypeMDOLowSulphur != null) {
            emailBody += "MDO Low Sulphur: <strong>" + vtsdata.fuelTypeMDOLowSulphur + "</strong><br>";
        }
        if(vtsdata.fuelTypeMDOUltraLowSulphur != "0" && vtsdata.fuelTypeMDOUltraLowSulphur != "" && vtsdata.fuelTypeMDOUltraLowSulphur != null) {
            emailBody += "MDO Ultra Low Sulphur: <strong>" + vtsdata.fuelTypeMDOUltraLowSulphur + "</strong><br>";
        }

        emailBody += "<br>";
        emailBody += "Total bunker tonnage: <strong>" + vtsdata.fuelTotalFuel + "</strong><br>";
        emailBody += "<br><br>";

        emailBody += "<br><br>";
        emailBody += "</div>";

        //End
        emailBody += "</body></html>";


        //for email to actually send
        generatedEmailSubject = emailSubject;
        generatedEmailBody = emailBody;
        emailTo = vtsdata.vtsEmail;
        emailFrom = "vessel.arcticweb@gmail.com";




//        return Response.status(201).entity(emailSubject+"\n"+emailBody).build();

        return Response.status(201).entity("{\"confirm\":true,\"message\":\""+emailSubject+"<br>"+emailBody+"\"}").build();

    }


}





