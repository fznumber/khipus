package com.encens.khipus.service.employees;

import com.encens.khipus.model.employees.Employee;
import com.encens.khipus.model.employees.RHMark;
import com.encens.khipus.util.DateUtils;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.*;

/**
 * RHMark service implementation class
 *
 * @author
 */

@Name("rhMarkService")
@Stateless
@AutoCreate
public class RHMarkServiceBean implements RHMarkService {
    @In("#{entityManager}")
    private EntityManager em;
    @Logger
    protected Log log;

    public List<RHMark> findRHMarkByEmployeeIdNumberByInitDateByEndDate(Employee employee, Date initDate, Date endDate) {
        try {
            Query query = em.createNamedQuery("RHMark.findRHMarkByMarkCodeByInitDateByEndDate")
                    .setParameter("markCode", employee.getMarkCode())
                    .setParameter("initDate", initDate)
                    .setParameter("endDate", endDate);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<RHMark>(0);
        }
    }

    public List<RHMark> findRHMarkByInitDateByEndDate(Date initDate, Date endDate) {
        try {
            Query query = em.createNamedQuery("RHMark.findRHMarkByInitDateAndEndDate")
                    .setParameter("initDate", initDate)
                    .setParameter("endDate", endDate);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<RHMark>(0);
        }
    }

    public Map<Date, List<Date>> getRHMarkDateTimeMapByDateRange(Employee employee, Date initDate, Date endDate) {
        Map<Date, List<Date>> result = new HashMap<Date, List<Date>>();
        try {
            List<Object[]> rhMarkDateList = em.createNamedQuery("RHMark.findRHMarkDateForPayrollGeneration")
                    .setParameter("markCode", employee.getMarkCode())
                    .setParameter("initDate", initDate)
                    .setParameter("endDate", endDate).getResultList();
            for (Object rhMarkDate[] : rhMarkDateList) {
                Date date = DateUtils.toCalendar((Date) rhMarkDate[0]).getTime();
                Date time = DateUtils.joinDateAndTime((Date) rhMarkDate[0], (Date) rhMarkDate[1]).getTime();
                if (!result.containsKey(date)) {
                    List<Date> rhMarkListTemp = new ArrayList<Date>();
                    rhMarkListTemp.add(time);
                    result.put(date, rhMarkListTemp);
                } else {
                    result.get(date).add(time);
                }
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }
}