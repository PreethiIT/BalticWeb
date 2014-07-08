/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.embryo.weather.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import dk.dma.embryo.common.configuration.Property;
import dk.dma.embryo.weather.model.DistrictForecast;
import dk.dma.embryo.weather.model.RegionForecast;

/**
 * Parser for reading routes in RT3 format. RT3 format is among others used by Transas ECDIS.
 * 
 * @author Jesper Tejlgaard
 */

@Named
public class DmiForecastParser_En {

    public static final Locale DEFAULT_LOCALE = new Locale("en", "UK");

    @Property("embryo.weather.dmi.parser.districts.en")
    @Inject
    public Set<String> districts;

    private boolean closeReader;

    public RegionForecast parse(InputStream is) throws IOException {
        if (is instanceof BufferedInputStream) {
            return parse((BufferedInputStream) is);
        }
        return parse(new BufferedInputStream(is));
    }

    public RegionForecast parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }

    private RegionForecast parse(BufferedInputStream is) throws IOException {
        RegionForecast result = new RegionForecast();

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(is));

            // Normalize text representation
            doc.getDocumentElement().normalize();

            NodeList children = doc.getDocumentElement().getChildNodes();
            DateTime from = null;
            DateTime to = null;

            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element elem = (Element) children.item(i);

                    if (elem.getNodeName().equalsIgnoreCase("dato")) {
                        from = extractFrom(elem);
                        result.setFrom(from.toDate());
                    } else if (elem.getNodeName().equalsIgnoreCase("oversigttidspunkt")) {
                        String time = extractElementText(elem);
                        time = time.replace("UTC.", "UTC");
                        time = time.replace("Synopsis", "");
                        time = time.replace("synopsis", "");
                        result.setTime(time.trim());
                    } else if (elem.getNodeName().equalsIgnoreCase("gyldighed")) {
                        to = extractTo(elem, from);
                        result.setTo(to.toDate());
                    } else if (elem.getNodeName().equalsIgnoreCase("synoptic")) {
                        String text = extractElementText(elem, "oversigt");
                        result.setDesc(text);
                    } else if (districts.contains(elem.getNodeName())) {
                        result.getDistricts().add(extractDistrikt(elem));
                    } 
                }
            }
        } catch (RuntimeException | ParserConfigurationException | SAXException e) {
            throw new IOException("Error parsing weather forecast", e);
        } finally {
            if (closeReader) {
                is.close();
            }
        }

        return result;
    }

    private String prettifyDateText(String text) {
        text = text.replace(" the", "");
        text = text.replace("UTC.", "");
        text = text.replace(".", "");
        text = text.replace(",", "");
        text = text.trim();
        text = text.substring(0, 1).toLowerCase() + text.substring(1);
        return text;
    }

    public DateTime extractFrom(Element dato) throws IOException {
        String text = extractElementText(dato);
        text = prettifyDateText(text);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE dd MMMM YYYY HHmm").withZone(DateTimeZone.UTC)
                .withLocale(DEFAULT_LOCALE);
        DateTime dt = formatter.parseDateTime(text);
        return dt;
    }

    public DateTime extractTo(Element gyldighed, DateTime from) throws IOException {
        String text = extractElementText(gyldighed);
        text = text.replace("Forecast, valid to ", "");
        text = text.replace(",", " " + from.getYear());
        text = prettifyDateText(text);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE dd MMMM yyyy HH").withZone(DateTimeZone.UTC)
                .withLocale(DEFAULT_LOCALE);
        DateTime to = formatter.parseDateTime(text);

        if (from.getMonthOfYear() == 12 && to.getDayOfMonth() < from.getDayOfMonth()) {
            text = text.replace("" + from.getYear(), "" + (1 + from.getYear()));
            to = formatter.parseDateTime(text);
        }
        return to;
    }

    public DistrictForecast extractDistrikt(Element distrikt) throws IOException {
        DistrictForecast forecast = new DistrictForecast();

        String name = distrikt.getNodeName();

        String forecastElemName = "udsigtfor" + name;
        String wavesElemName = "waves" + name;

        if ("nunapisuateakangia".equals(name)) {
            forecastElemName = "udsigtfornunapisuatakangia";
            wavesElemName = "waveskangia";
        } else if ("nunapisuatakitaa".equals(name)) {
            forecastElemName = "udsigtfornunapisuatakitaa";
            wavesElemName = "waveskitaa";
        }

        forecast.setName(distrikt.getAttribute("name").replace(":", ""));
        forecast.setForecast(extractElementText(distrikt, forecastElemName));
        forecast.setWaves(extractElementText(distrikt, wavesElemName));
        return forecast;
    }

    public String extractElementText(Element root, String elementName) throws IOException {
        NodeList uniqueList = root.getElementsByTagName(elementName);
        if (uniqueList.getLength() != 1) {
            throw new IOException("Expected exactly one <" + elementName + "> element within <" + root.getNodeName()
                    + "> element");
        }

        return extractElementText((Element) uniqueList.item(0));
    }

    public String extractElementText(Element element) throws IOException {
        List<Node> textList = new ArrayList<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if ("text".equals(n.getNodeName())) {
                textList.add(n);
            }
        }
        if (textList.size() != 1) {
            throw new IOException("Expected exactly one <text> element within <" + element.getNodeName() + "> element");
        }

        return trim(textList.get(0).getTextContent());
    }

    public static String trim(String input) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(line.trim());
        }
        return result.toString().trim();
    }
}
