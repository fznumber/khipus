package com.encens.khipus.service.production;

import com.encens.khipus.framework.service.GenericServiceBean;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

/**
 * @author Ariel Siles Encias
 */

@Stateless
@Name("employeeTimeCardServiceService")
@AutoCreate
public class EmployeeTimeCardServiceBean extends GenericServiceBean implements EmployeeTimeCardService {

    @In("#{entityManager}")
    private EntityManager em;


}
