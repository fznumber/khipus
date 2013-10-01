package com.encens.khipus.service.production;

import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.framework.service.ExtendedGenericServiceBean;
import com.encens.khipus.model.production.ProductiveZone;
import com.encens.khipus.model.production.RawMaterialProducer;
import com.encens.khipus.model.production.RawMaterialProducerDiscount;
import com.encens.khipus.model.production.SalaryMovementProducer;
import com.encens.khipus.util.RoundUtil;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 6/7/13
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */
@Name("salaryMovementProducerService")
@Stateless
@AutoCreate
public class SalaryMavementProducerServiceBean extends ExtendedGenericServiceBean implements SalaryMovementProducerService {

    @Override
    public RawMaterialProducerDiscount prepareDiscount(RawMaterialProducer rawMaterialProducer, Date startDate, Date endDate) throws EntryNotFoundException {

        RawMaterialProducerDiscount rawMaterialProducerDiscount = new RawMaterialProducerDiscount();

        rawMaterialProducerDiscount.setRawMaterialProducer(rawMaterialProducer);
        rawMaterialProducerDiscount.setConcentrated(0.0);
        rawMaterialProducerDiscount.setYogurt(0.0);
        rawMaterialProducerDiscount.setVeterinary(0.0);
        rawMaterialProducerDiscount.setCredit(0.0);
        rawMaterialProducerDiscount.setCans(0.0);
        rawMaterialProducerDiscount.setOtherDiscount(0.0);
        rawMaterialProducerDiscount.setOtherIncoming(0.0);

        Double concentrated = 0.0;
        Double yogurt = 0.0;
        Double veterinary = 0.0;
        Double credit = 0.0;
        Double cans = 0.0;
        Double otherDiscount = 0.0;
        Double otherIncoming = 0.0;

        List<Object[]> salaryMovementProducers = getEntityManager().createNamedQuery("SalaryMovementProducer.getDiscount")
                                                               .setParameter("startDate",startDate,TemporalType.DATE)
                                                               .setParameter("endDate",endDate,TemporalType.DATE)
                                                               .setParameter("rawMaterialProducer",rawMaterialProducer)
                                                               .getResultList();
        if(salaryMovementProducers.size()>0)
        {
            for(Object[] salaryMovementProducer: salaryMovementProducers)
            {
                   Double valor = (Double)salaryMovementProducer[0];
                   String typeDiscount = (String)salaryMovementProducer[2];
                       if(typeDiscount.compareTo("CONCENTRADOS")==0)
                       {
                           concentrated += valor;
                       }
                       if(typeDiscount.compareTo("YOGURT")==0)
                       {
                           yogurt += valor;
                       }
                       if(typeDiscount.compareTo("VETERINARIO")==0)
                       {
                           veterinary += valor;
                       }
                       if(typeDiscount.compareTo("TACHOS")==0)
                       {
                           cans += valor;
                       }
                       if(typeDiscount.compareTo("OTROS EGRESOSO")==0)
                       {
                           otherDiscount += valor;
                       }
                       if(typeDiscount.compareTo("OTROS INGRESOS")==0)
                       {
                           otherIncoming += valor;
                       }
                       if(typeDiscount.compareTo("CREDITO")==0)
                       {
                           credit += valor;
                       }
            }
        }

        rawMaterialProducerDiscount.setConcentrated(RoundUtil.getRoundValue(concentrated,2, RoundUtil.RoundMode.SYMMETRIC));
        rawMaterialProducerDiscount.setYogurt(RoundUtil.getRoundValue(yogurt,2, RoundUtil.RoundMode.SYMMETRIC));
        rawMaterialProducerDiscount.setVeterinary(RoundUtil.getRoundValue(veterinary,2,RoundUtil.RoundMode.SYMMETRIC));
        rawMaterialProducerDiscount.setCredit(RoundUtil.getRoundValue(credit,2,RoundUtil.RoundMode.SYMMETRIC));
        rawMaterialProducerDiscount.setCans(RoundUtil.getRoundValue(cans,2,RoundUtil.RoundMode.SYMMETRIC));
        rawMaterialProducerDiscount.setOtherDiscount(RoundUtil.getRoundValue(otherDiscount,2,RoundUtil.RoundMode.SYMMETRIC));
        rawMaterialProducerDiscount.setOtherIncoming(RoundUtil.getRoundValue(otherIncoming,2,RoundUtil.RoundMode.SYMMETRIC));

        return rawMaterialProducerDiscount;
    }
}
