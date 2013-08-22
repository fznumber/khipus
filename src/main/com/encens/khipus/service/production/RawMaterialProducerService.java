package main.com.encens.khipus.service.production;

import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.RawMaterialProducer;
import com.encens.hp90.model.production.ProductiveZone;

import javax.ejb.Local;
import java.util.List;


@Local
public interface RawMaterialProducerService extends GenericService {

//    public List<RawMaterialProducer> findAllThatDontHaveCollectedRawMaterial(ProductiveZone productiveZone, Date date);

    public List<RawMaterialProducer> findAll(ProductiveZone productiveZone);
}
