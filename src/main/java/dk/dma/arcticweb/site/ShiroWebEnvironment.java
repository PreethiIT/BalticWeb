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
package dk.dma.arcticweb.site;

import org.apache.shiro.config.Ini;
import org.apache.shiro.config.Ini.Section;
import org.apache.shiro.web.env.IniWebEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.arcticweb.service.JpaRealm;

public class ShiroWebEnvironment extends IniWebEnvironment {

    // No CDI support. Constructor also before CDI has been enabled. Thus creating logger manually. 
    private final Logger logger = LoggerFactory.getLogger(ShiroWebEnvironment.class);
    
    public ShiroWebEnvironment() {
        logger.info("Initialising Shiro security for Embryonic e-Navigation Application");

        Ini ini = new Ini();
        
        Section s = ini.addSection("main");
        s.put("realm", JpaRealm.class.getName());
        
        setIni(ini);
    }

}
