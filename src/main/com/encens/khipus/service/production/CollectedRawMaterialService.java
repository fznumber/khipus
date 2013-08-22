package main.com.encens.khipus.service.production;

import com.encens.hp90.exception.ConcurrencyException;
import com.encens.hp90.exception.EntryDuplicatedException;
import com.encens.hp90.exception.ReferentialIntegrityException;
import com.encens.hp90.model.production.CollectedRawMaterial;
import com.encens.hp90.model.production.ProductiveZone;

import javax.ejb.Local;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/7/13
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
@Local
public interface CollectedRawMaterialService {

//    public List<CollectedRawMaterial> prepareRawMaterialCollection(ProductiveZone productiveZone, Date date);

    void save(ProductiveZone productiveZone, List<CollectedRawMaterial> collectedRawMaterialList, Date date) throws ConcurrencyException, EntryDuplicatedException;

    void delete(ProductiveZone productiveZone, Date date) throws ConcurrencyException, ReferentialIntegrityException;
}
