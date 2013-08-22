package main.com.encens.khipus.service.production;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import java.util.List;

@Name("productionOrderCodeGeneratorService")
@Stateless
@AutoCreate
public class ProductionOrderCodeGeneratorServiceBean implements ProductionOrderCodeGeneratorService {

    @In private EntityManager entityManager;

    @Override
    public int findLasCounter(String seed) {
        List<String> codes = entityManager.createNamedQuery("ProductionOrder.findBySubDateOnCode")
                                          .setParameter("seed", seed)
                                          .getResultList();

        String greatest = seed + "0";
        for(String code : codes) {
            if (code.compareTo(greatest) > 0) {
                greatest = code;
            }
        }

        String integerPart = greatest.substring(seed.length());
        return Integer.parseInt(integerPart);
    }
}
