package main.com.encens.khipus.service.production;

import com.encens.hp90.exception.EntryNotFoundException;
import com.encens.hp90.framework.service.ExtendedGenericServiceBean;
import com.encens.hp90.model.production.RawMaterialProducer;
import com.encens.hp90.model.production.RawMaterialProducerDiscount;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;

@Stateless
@Name("rawMaterialProducerDiscountService")
@AutoCreate
public class RawMaterialProducerDiscountServiceBean extends ExtendedGenericServiceBean implements RawMaterialProducerDiscountService {

    @Override
    public RawMaterialProducerDiscount prepareDiscount(RawMaterialProducer rawMaterialProducer) throws EntryNotFoundException {
        try {
            RawMaterialProducerDiscount discount = (RawMaterialProducerDiscount) getEntityManager().createNamedQuery("RawMaterialProducerDiscount.findWithGreatestCodeByRawMaterialProducer")
                                                                                     .setParameter("rawMaterialProducer", rawMaterialProducer)
                                                                                     .getSingleResult();
            if (discount.getRawMaterialPayRecord() == null) {
                return discount;
            } else {
                return createNewRawMaterialProducerDiscount(rawMaterialProducer, discount.getCode() + 1);
            }
        } catch (NoResultException ex) {
            return createNewRawMaterialProducerDiscount(rawMaterialProducer, 1);
        }
    }

    private RawMaterialProducerDiscount createNewRawMaterialProducerDiscount(RawMaterialProducer rawMaterialProducer, long code) throws EntryNotFoundException {
        rawMaterialProducer = findById(RawMaterialProducer.class, rawMaterialProducer.getId());
        RawMaterialProducerDiscount discount = new RawMaterialProducerDiscount();
        discount.setCode(code);
        discount.setRawMaterialProducer(rawMaterialProducer);
        return discount;
    }
}
