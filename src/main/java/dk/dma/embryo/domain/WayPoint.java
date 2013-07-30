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
package dk.dma.embryo.domain;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class WayPoint implements Serializable {

    private static final long serialVersionUID = -7205030526506222850L;

    // //////////////////////////////////////////////////////////////////////
    // Entity fields (also see super class)
    // //////////////////////////////////////////////////////////////////////

    private String name;

    private Position position;
    
    private Double rot;

    private Double turnRadius;
    
    private RouteLeg leg;

    // //////////////////////////////////////////////////////////////////////
    // business logic
    // //////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////////
    // Utility methods
    // //////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////////
    // Constructors
    // //////////////////////////////////////////////////////////////////////
    public WayPoint() {
        position = new Position();
    }

    public WayPoint(String name, Double latitude, Double longitude, Double rot, Double turnRadius) {
        this();
        
        this.name = name;
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        this.rot = rot;
        this.turnRadius = turnRadius;
    }

    // //////////////////////////////////////////////////////////////////////
    // Object methods
    // //////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        return "WayPoint [name=" + name + ", position=" + position + ", turnRadius=" + turnRadius + "]";
    }

    // //////////////////////////////////////////////////////////////////////
    // Property methods
    // //////////////////////////////////////////////////////////////////////
    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public Double getTurnRadius() {
        return turnRadius;
    }

    public RouteLeg getLeg() {
        return leg;
    }
    
    

    public Double getRot() {
        return rot;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLeg(RouteLeg leg) {
        this.leg = leg;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setRot(Double rot) {
        this.rot = rot;
    }

    public void setTurnRadius(Double turnRadius) {
        this.turnRadius = turnRadius;
    }

}
