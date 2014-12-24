package com.encens.khipus.service.customers;

import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 26/12/14
 * Time: 1:07
 * To change this template use File | Settings | File Templates.
 */
@Stateless
@Name("rePrintsService")
@AutoCreate
public class RePrintsServiceBean extends ExtendedGenericServiceBean implements RePrintsService {
}
