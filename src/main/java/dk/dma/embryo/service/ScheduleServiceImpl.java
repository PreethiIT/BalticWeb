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
package dk.dma.embryo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;

import dk.dma.embryo.component.RouteActivator;
import dk.dma.embryo.component.RouteSaver;
import dk.dma.embryo.dao.RealmDao;
import dk.dma.embryo.dao.ScheduleDao;
import dk.dma.embryo.dao.VesselDao;
import dk.dma.embryo.domain.Route;
import dk.dma.embryo.domain.SailorRole;
import dk.dma.embryo.domain.Vessel;
import dk.dma.embryo.domain.Voyage;
import dk.dma.embryo.domain.WayPoint;
import dk.dma.embryo.security.AuthorizationChecker;
import dk.dma.embryo.security.Subject;
import dk.dma.embryo.security.authorization.Roles;
import dk.dma.embryo.security.authorization.RolesAllowAll;
import dk.dma.embryo.security.authorization.VesselModifierInterceptor;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Interceptors(value=AuthorizationChecker.class)
public class ScheduleServiceImpl implements ScheduleService {

    @Inject
    private ScheduleDao scheduleRepository;

    @Inject
    private VesselDao vesselRepository;

    @Inject
    private RealmDao realmDao;

    @Inject
    private Subject subject;

    @Inject
    private Logger logger;

    public ScheduleServiceImpl() {
    }

    public ScheduleServiceImpl(ScheduleDao scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @Roles(value=SailorRole.class)
    @Interceptors(VesselModifierInterceptor.class)
    public void updateSchedule(Long mmsi, List<Voyage> toBeSaved, String[] toBeDeleted) {

        List<String> ids = new ArrayList<>(toBeSaved.size());
        for (int i = 0; i < toBeSaved.size(); i++) {
            ids.add(toBeSaved.get(i).getEnavId());
        }

        List<Voyage> persisted = scheduleRepository.getByEnavIds(ids);
        Map<String, Voyage> persistedAsMap = Voyage.asMap(persisted);

        Vessel vessel = vesselRepository.getVessel(mmsi);

        // In order to maintain JPA relations we have to select from DB and merge data manually
        for (Voyage voyage : toBeSaved) {
            Voyage v = null;
            if (persistedAsMap.containsKey(voyage.getEnavId())) {
                v = persistedAsMap.get(voyage.getEnavId());

                v.setArrival(voyage.getArrival());
                v.setBerthName(voyage.getBerthName());
                v.setDeparture(voyage.getDeparture());
                v.setPosition(voyage.getPosition());
                v.setCrewOnBoard(voyage.getCrewOnBoard());
                v.setPassengersOnBoard(voyage.getPassengersOnBoard());
                v.setDoctorOnBoard(voyage.getDoctorOnBoard());
            } else {
                v = voyage;
                vessel.addVoyageEntry(v);
            }
            scheduleRepository.saveEntity(v);
        }

        if (toBeDeleted.length > 0) {
            List<String> toBeDeletedAsList = Arrays.asList(toBeDeleted);
            List<Voyage> toDelete = scheduleRepository.getByEnavIds(toBeDeletedAsList);
            for (Voyage voyage : toDelete) {
                scheduleRepository.remove(voyage);
            }
        }
    }

    @Override
    @RolesAllowAll
    public List<Voyage> getSchedule(Long mmsi) {
        List<Voyage> schedule = scheduleRepository.getSchedule(mmsi);

        for (Voyage voyage : schedule) {
            Route r = voyage.getRoute();
            if (r != null) {
                for (WayPoint w : r.getWayPoints()) {
                    w.getName();
                }
            }
        }

        return schedule;
    }

    /**
     * Used to save uploaded routes.
     * 
     * Automatically fills out route fields also being part of voyage information, like departure location, destination
     * location, times etc.
     */
    @Override
    @Roles({SailorRole.class})
    @Interceptors(VoyageModifierInterceptor.class)
    public String saveRoute(Route route, String voyageId, Boolean active) {
        return new RouteSaver(scheduleRepository).saveRoute(route, voyageId, active);
    }
    
    @Override
    @Roles(SailorRole.class)
    @Interceptors(RouteModifierInterceptor.class)
    public String saveRoute(Route route) {
        if (route.getId() == null) {
            Long id = scheduleRepository.getRouteId(route.getEnavId());
            route.setId(id);
        }

        scheduleRepository.saveEntity(route);

        return route.getEnavId();
    }

    @Override
    @RolesAllowAll
    public Route getActiveRoute(Long mmsi) {
        Route r = scheduleRepository.getActiveRoute(mmsi);
        if (r != null) {
            if (r.getWayPoints().size() > 0) {
                r.getWayPoints().get(0);
            }
        }
        return r;
    }


    @Override
    @Roles(SailorRole.class)
    @Interceptors(RouteModifierInterceptor.class)
    public Route activateRoute(String routeEnavId, Boolean activate) {
        logger.debug("activateRoute({}, {})", routeEnavId, activate);
        return new RouteActivator(scheduleRepository, realmDao, subject).activateRoute(routeEnavId, activate);
    }

    @Override
    @RolesAllowAll
    public Route getRouteByEnavId(String enavId) {
        Route route = scheduleRepository.getRouteByEnavId(enavId);
        return route;
    }
    
    public static class RouteModifierInterceptor{
        @Inject
        private Subject subject;

        @AroundInvoke
        Object onlyOwnRoutes(InvocationContext ctx) throws Exception{
            String enavId;
            
            if(ctx.getParameters()[0] instanceof Route){
                enavId = ((Route)ctx.getParameters()[0]).getEnavId();
            }else if (ctx.getParameters()[0] instanceof String){
                enavId = (String)ctx.getParameters()[0];
            }else{
                throw new IllegalArgumentException("First argument must be one of types " + Route.class.getName() + ", " + String.class.getName());
            }
            
            if(enavId != null && !subject.authorizedToModifyRoute(enavId)){
                throw new AuthorizationException("Not authorized to modify route");
            }
            
            return ctx.proceed();
        }
    }

    public static class VoyageModifierInterceptor{
        @Inject
        private Subject subject;

        @AroundInvoke
        Object onlyOwnVoyages(InvocationContext ctx) throws Exception{
            String voyageEnavId = (String)ctx.getParameters()[1];
            if(!subject.authorizedToModifyVoyage(voyageEnavId)){
                throw new AuthorizationException("Not authorized to modify voyage/route");
            }
            
            return ctx.proceed();
        }
    }

}
