package main.com.encens.khipus.service.production;

import com.encens.hp90.exception.ConcurrencyException;
import com.encens.hp90.exception.EntryDuplicatedException;
import com.encens.hp90.exception.ReferentialIntegrityException;
import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.ProductionPlanning;

import javax.ejb.Local;


@Local
public interface ProductionPlanningService extends GenericService {

    public void refresh(Object entity);

    public ProductionPlanning find(long id);
}
