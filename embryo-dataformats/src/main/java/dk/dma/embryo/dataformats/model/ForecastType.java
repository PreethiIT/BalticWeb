/* Copyright (c) 2011 Danish Maritime Authority.
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
package dk.dma.embryo.dataformats.model;

import dk.dma.embryo.dataformats.netcdf.NetCDFType;

public class ForecastType extends NetCDFType {

    private Type type;
    
    public ForecastType(String name, String code, Type type) {
        setName(name);
        setCode(code);
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    
    public enum Type {
        ICE_FORECAST, CURRENT_FORECAST, WAVE_FORECAST, WIND_FORECAST
    }
    
    @Override
    public String toString() {
        return "Forecast type: " + getName() + " (" + type + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        if(obj.getClass() != getClass()) {
            return false;
        }
        return ((ForecastType)obj).getType() == getType();
    }
    
    @Override
    public int hashCode() {
        return getType().hashCode();
    }
}
