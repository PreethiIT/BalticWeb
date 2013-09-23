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
package dk.dma.arcticweb.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import dk.dma.embryo.domain.GreenPosReport;

@Stateless
public class GreenPosDaoImpl extends DaoImpl implements GreenPosDao {

    public GreenPosDaoImpl() {
        super();
    }

    public GreenPosDaoImpl(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public GreenPosReport findLatest(String shipMaritimeId) {

        TypedQuery<GreenPosReport> query = em.createNamedQuery("GreenPosReport:findLatest", GreenPosReport.class);
        query.setParameter("shipMaritimeId", shipMaritimeId);
        query.setMaxResults(1);

        List<GreenPosReport> result = query.getResultList();

        return getSingleOrNull(result);
    }

}
