package main.com.encens.khipus.service.production;

import com.encens.hp90.exception.ConcurrencyException;
import com.encens.hp90.exception.EntryDuplicatedException;
import com.encens.hp90.exception.ReferentialIntegrityException;
import com.encens.hp90.framework.service.DataBaseCommand;
import com.encens.hp90.framework.service.DataBaseExecutor;
import com.encens.hp90.model.production.CollectedRawMaterial;
import com.encens.hp90.model.production.CollectionForm;
import com.encens.hp90.model.production.ProductiveZone;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

@Name("collectedRawMaterialService")
@Stateless
@AutoCreate
public class CollectedRawMaterialServiceBean implements CollectedRawMaterialService {

    @In(value = "#{entityManager}")
    private EntityManager em;

    @In
    private RawMaterialProducerService rawMaterialProducerService;

   /* @Override
    public List<CollectedRawMaterial> prepareRawMaterialCollection(ProductiveZone productiveZone, Date date) {
        List<CollectedRawMaterial> rawMaterialList = em.createNamedQuery("CollectedRawMaterial.WithRawMaterialProducerAndProductiveZoneFindByDateAndProductiveZone")
                                         .setParameter("productiveZone", productiveZone)
                                         .setParameter("date", date)
                                         .getResultList();

        if (rawMaterialList.size() == 0) {
            addPendentProducers(productiveZone, date, rawMaterialList);
        }

        return rawMaterialList;
    }

    private void addPendentProducers(ProductiveZone productiveZone, Date date, List<CollectedRawMaterial> rawMaterialList) {
        List<RawMaterialProducer> pendent = rawMaterialProducerService.findAllThatDontHaveCollectedRawMaterial(productiveZone, date);

        for(RawMaterialProducer mp : pendent) {
            CollectedRawMaterial cm = new CollectedRawMaterial();
            //cm.setProductiveZone(productiveZone);
            cm.setRawMaterialProducer(mp);
            cm.setAmount(0.0);
            //cm.setDate(date);
            rawMaterialList.add(cm);
        }
    }*/

    @Override
    public void delete(ProductiveZone productiveZone, Date date) throws ConcurrencyException, ReferentialIntegrityException {
        new DataBaseExecutor(new DeleteCommand(productiveZone, date)).delete();
    }

    @Override
    public void save(ProductiveZone productiveZone, List<CollectedRawMaterial> collectedRawMaterialList, Date date) throws ConcurrencyException, EntryDuplicatedException {
        new DataBaseExecutor(new SaveCommand(productiveZone, collectedRawMaterialList, date)).update();
    }

    class DeleteCommand implements DataBaseCommand {
        ProductiveZone productiveZone;
        Date date;

        DeleteCommand(ProductiveZone productiveZone, Date date) {
            this.productiveZone = productiveZone;
            this.date = date;
        }

        @Override
        public void execute() {
            List<CollectedRawMaterial> list = em.createNamedQuery("CollectedRawMaterial.WithRawMaterialProducerFindByProductiveZoneAndDate")
                    .setParameter("productiveZone", productiveZone)
                    .setParameter("date", date)
                    .getResultList();

            for (CollectedRawMaterial item : list) {
                em.remove(item);
            }

            em.flush();
        }
    }

    class SaveCommand implements  DataBaseCommand {
        ProductiveZone productiveZone;
        List<CollectedRawMaterial> collectedRawMaterialList;
        Date date;

        public SaveCommand(ProductiveZone productiveZone, List<CollectedRawMaterial> collectedRawMaterialList, Date date) {
            this.productiveZone = productiveZone;
            this.collectedRawMaterialList = collectedRawMaterialList;
            this.date = date;
        }

        @Override
        public void execute() {
            mergeUnmanaged(collectedRawMaterialList);
            updateCollectionForm(productiveZone, collectedRawMaterialList, date);
            em.flush();
        }

        private void mergeUnmanaged(List<CollectedRawMaterial> collectedRawMaterialList) {
            for (CollectedRawMaterial cm : collectedRawMaterialList) {
                if (!em.contains(cm)) {
                    em.merge(cm);
                }
            }
        }

        private void updateCollectionForm(ProductiveZone productiveZone, List<CollectedRawMaterial> collectedRawMaterialList, Date date) {
            List<CollectionForm> cfs = em.createNamedQuery("CollectionForm.findByDateAndProductiveZone")
                    .setParameter("productiveZone", productiveZone)
                    .setParameter("date", date)
                    .getResultList();

            CollectionForm cf;
            if (cfs == null || cfs.size() == 0) {
                cf = createCollectionForm(productiveZone, date);
                em.persist(cf);
            } else {
                cf = cfs.get(0);
            }

            //cf.setReceivedAmount(calculateTotalAmount(collectedRawMaterialList));
        }

        private CollectionForm createCollectionForm(ProductiveZone productiveZone, Date date) {
            CollectionForm cf;
            cf = new CollectionForm();
            //cf.setProductiveZone(productiveZone);
            cf.setDate(date);
            //cf.setReceivedAmount(0.0);
            //cf.setRejectedAmount(0.0);
            //cf.setWeightedAmount(0.0);
            return cf;
        }

        private double calculateTotalAmount(List<CollectedRawMaterial> collectedRawMaterialList) {
            double total = 0.0;
            for (CollectedRawMaterial cm : collectedRawMaterialList) {
                total += cm.getAmount();
            }
            return total;
        }
    }
}
